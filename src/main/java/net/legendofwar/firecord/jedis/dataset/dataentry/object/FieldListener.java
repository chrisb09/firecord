package net.legendofwar.firecord.jedis.dataset.dataentry.object;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.ConstructorSignature;
import org.aspectj.lang.reflect.FieldSignature;

import net.legendofwar.firecord.Firecord;
import net.legendofwar.firecord.communication.ByteMessage;
import net.legendofwar.firecord.communication.JedisCommunication;
import net.legendofwar.firecord.communication.JedisCommunicationChannel;
import net.legendofwar.firecord.jedis.ClassicJedisPool;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;
import net.legendofwar.firecord.jedis.dataset.dataentry.SimpleInterface;
import net.legendofwar.firecord.jedis.dataset.dataentry.event.ReferenceUpdateEvent;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RWrapper;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.SimpleData;
import net.legendofwar.firecord.jedis.dataset.datakeys.ByteFunctions;
import net.legendofwar.firecord.jedis.dataset.datakeys.ClassNameLookup;
import net.legendofwar.firecord.jedis.dataset.datakeys.DataKeySuffix;
import redis.clients.jedis.Jedis;

@Aspect
public class FieldListener {

    public static long variableChanges = 0l;

    @After("execution(net.legendofwar.firecord.jedis.dataset.dataentry.object.AbstractObject+.new(..)) && !cflowbelow(execution(net.legendofwar.firecord.jedis.dataset.dataentry.object.AbstractObject+.new(..))) && this(instance)")
    public void afterConstructor(JoinPoint joinPoint, AbstractObject instance) {
        if (joinPoint.getSignature() instanceof ConstructorSignature) {

            instance.initialized = true;
        }
    }

    public static Field[] getStaticFields(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> Modifier.isStatic(field.getModifiers()))
                .toArray(Field[]::new);
    }

    /*
    @formatter:off
    @Before("staticinitialization(net.legendofwar.firecord.jedis.dataset.dataentry.object.AbstractObject+)")
    public void beforeStaticInitialization(JoinPoint jp) {
        Class<?> clazz = jp.getSignature().getDeclaringType();
        if (clazz != null){
            if (TestObject.class.isAssignableFrom(clazz)){
                try {
                    //System.out.println("@Before: "+(String.join(",", Arrays.stream(clazz.getDeclaredFields()).map(field -> field.getName()).toList())));
                    //System.out.println("  f: "+clazz.getDeclaredField("f").get(null));
                } catch (SecurityException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    @formatter:on    
    */

    @After("staticinitialization(net.legendofwar.firecord.jedis.dataset.dataentry.object.AbstractObject+)")
    public void afterStaticInitialization(JoinPoint jp) {
        Class<?> clazz = jp.getSignature().getDeclaringType();

        Bytes key = AbstractObject.getStaticClassNameKey(clazz.getName());

        HashMap<String, Bytes> references = new HashMap<String, Bytes>();
        try (Jedis j = ClassicJedisPool.getJedis()) {
            Map<byte[], byte[]> refs = j.hgetAll(ByteFunctions.join(key.getData()));
            for (Map.Entry<byte[], byte[]> en : refs.entrySet()) {
                references.put(new Bytes(en.getKey()).asString(), new Bytes(en.getValue()));
            }
        }
        boolean hadNoEntries = references.isEmpty();
        AbstractObject.loadObject(clazz, null, references);
        if (hadNoEntries) {
            try (Jedis j = ClassicJedisPool.getJedis()) {
                j.set(ByteFunctions.join(key, DataKeySuffix.TYPE), DataType.CLASS.toString().getBytes());
                j.set(ByteFunctions.join(key, DataKeySuffix.CLASS), ClassNameLookup.getId(clazz.getName()).getData());
            }
        }
        for (Method m : clazz.getMethods()) {
            if (m != null && AnnotationChecker.isStaticInitFunction(m) && m.getParameterTypes().length == 0) {
                try {
                    m.invoke(null, new Object[0]);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @SuppressWarnings("null")
    @Around("set(!final * net.legendofwar.firecord.jedis.dataset.dataentry.object.AbstractObject+.*) && args(newValue)")
    public void aroundFieldChange(ProceedingJoinPoint joinPoint, Object newValue) throws Throwable {
        assert(newValue == null || newValue instanceof AbstractData<?>);
        AbstractData<?> newAbstractDataValue = (AbstractData<?>) newValue;
        AbstractObject instance = (AbstractObject) joinPoint.getTarget();
        Object replacingObject = null;
        if (instance == null || instance.isInitialized()) { // static fields don't have an instance
            FieldSignature fieldSignature = (FieldSignature) joinPoint.getSignature();
            Field field = fieldSignature.getField();
            boolean isStatic = Modifier.isStatic(field.getModifiers());
            Class<?> declaringClass = fieldSignature.getDeclaringType();

            String fieldName = fieldSignature.getName();

            if (field != null && AbstractData.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                AbstractData<?> oldValue = isStatic ? (AbstractData<?>) field.get(null) : (AbstractData<?>) field.get(instance);
                Class<?> fieldType = field.getType();

                if (newAbstractDataValue == null) {
                    // set reference to null

                    if (instance != null) {
                        instance.references.put(fieldName, new Bytes());
                    }

                    try (Jedis j = ClassicJedisPool.getJedis()) {
                        j.hset(isStatic ? AbstractObject.getStaticClassNameKey(declaringClass.getName()).getData()
                                : instance.getKey().getData(), new Bytes(field.getName()).getData(),
                                new Bytes().getData());
                    }


                    if (oldValue != null && oldValue instanceof AbstractData ad){
                        if (instance != null){
                            ad.owners.remove(instance);
                        }
                    }

                    JedisCommunication.broadcast(JedisCommunicationChannel.REFERENCE_UPDATE,
                            ByteMessage.write(isStatic ? new Bytes(declaringClass.getName()) : instance.getKey(),
                                    new Bytes(fieldName), new Bytes().getData(),
                                    new Bytes(Byte.valueOf((byte) (isStatic ? 1 : 0)))));
                    AbstractData.notifyListeners(isStatic ? null : instance,
                            new ReferenceUpdateEvent<AbstractData<?>>(Firecord.getId(),
                                    JedisCommunicationChannel.REFERENCE_UPDATE, isStatic ? null : instance,
                                    isStatic ? declaringClass : null, oldValue, newAbstractDataValue, fieldName, isStatic));

                } else if (AbstractData.class.isAssignableFrom(fieldType)) {
                    // implies newValue is instance of AbstractData

                    AbstractData<?> adNewValue = (AbstractData<?>) newAbstractDataValue;

                    if (adNewValue.getKey() == null) {
                        // newValue is a temp entry
                        // meaning we need to create a new entry

                        Bytes key = AbstractObject
                                .getFieldKey(isStatic ? AbstractObject.getStaticClassNameKey(declaringClass.getName())
                                        : instance.getKey(), fieldName);

                        if (SimpleInterface.class.isAssignableFrom(newAbstractDataValue.getClass())) {

                            AbstractData<?> oldData = null;
                            synchronized (AbstractData.loaded) {
                                if (AbstractData.loaded.containsKey(key)) {
                                    oldData = AbstractData.loaded.get(key);
                                }
                            }
                            if (oldData != null) {
                                AbstractObject.removeFromLoaded(oldData);
                            }

                            // informs the other instances to call .removeFromLoaded too
                            // in order to prepare for overwrite
                            JedisCommunication.broadcast(JedisCommunicationChannel.OBJECT_OVERWRITE, key);

                            Object defaultValue = null;
                            if (SimpleData.class.isAssignableFrom(newAbstractDataValue.getClass())) {
                                defaultValue = SimpleData.getValue(((SimpleData<?>) newAbstractDataValue));
                            } else if (SimpleAbstractObject.class.isAssignableFrom(newAbstractDataValue.getClass())) {
                                defaultValue = ((SimpleAbstractObject<?>) newAbstractDataValue).getTempValue();
                            }

                            try (Jedis j = ClassicJedisPool.getJedis()) {
                                j.del(key.getData());
                                j.del(ByteFunctions.join(key, DataKeySuffix.TYPE));
                            }
                            AbstractData<?> replacingEntry = null;
                            if (defaultValue != null) {
                                if (RWrapper.class.isAssignableFrom(newAbstractDataValue.getClass())) {
                                    // Otherwise the RWrapper constructor fails since it's defined for
                                    // AbstracData<?> but
                                    // usually is passed a subclass of object
                                    replacingEntry = AbstractObject.callConstructor(key, newAbstractDataValue.getClass(),
                                            defaultValue, AbstractData.class);
                                } else {
                                    replacingEntry = AbstractObject.callConstructor(key, newAbstractDataValue.getClass(),
                                            defaultValue, defaultValue.getClass());
                                }
                            } else {
                                replacingEntry = AbstractObject.callConstructor(key, newAbstractDataValue.getClass());
                            }

                            if (replacingEntry != null) {
                                replacingObject = replacingEntry;

                                // TODO: reload entry above (remove prior loaded occurances) & send message to
                                // other instances to do the same

                                if (instance != null) {
                                    instance.references.put(fieldName, new Bytes(field.getName()));
                                }

                                try (Jedis j = ClassicJedisPool.getJedis()) {
                                    j.hset(isStatic
                                            ? AbstractObject.getStaticClassNameKey(declaringClass.getName()).getData()
                                            : instance.getKey().getData(), new Bytes(field.getName()).getData(),
                                            key.getData());
                                }


                                if (oldValue != null && oldValue instanceof AbstractData ad){
                                    if (instance != null){
                                        ad.owners.remove(instance);
                                    }
                                }

                                replacingEntry.owners.add(instance);

                                JedisCommunication.broadcast(JedisCommunicationChannel.REFERENCE_UPDATE,
                                        ByteMessage.write(
                                                isStatic ? new Bytes(declaringClass.getName()).getData()
                                                        : instance.getKey(),
                                                new Bytes(fieldName), key,
                                                new Bytes(Byte.valueOf((byte) (isStatic ? 1 : 0)))));
                                AbstractData.notifyListeners(isStatic ? null : instance,
                                        new ReferenceUpdateEvent<AbstractData<?>>(Firecord.getId(),
                                                JedisCommunicationChannel.REFERENCE_UPDATE, isStatic ? null : instance,
                                                isStatic ? declaringClass : null, oldValue, newAbstractDataValue, fieldName,
                                                isStatic));
                            } else {
                                // Undefined behavior
                                System.out.println("Undefined behaviour.");
                            }

                        }

                    } else {

                        // we are replacing an existing entry

                        if (oldValue == null || oldValue != newAbstractDataValue) {

                            // entry has truly changed

                            if (instance != null) {
                                instance.references.put(fieldName, new Bytes(((AbstractData<?>) newAbstractDataValue).getKey()));
                            }

                            try (Jedis j = ClassicJedisPool.getJedis()) {
                                j.hset(isStatic ? new Bytes(declaringClass.getName()).getData()
                                        : instance.getKey().getData(), new Bytes(fieldName).getData(),
                                        ((AbstractData<?>) newAbstractDataValue).getKey().getData());
                            }

                            if (oldValue != null && oldValue instanceof AbstractData ad){
                                if (instance != null){
                                    ad.owners.remove(instance);
                                }
                            }
                            if (newAbstractDataValue instanceof AbstractData<?> ad){
                                ad.owners.add(instance);
                            }

                            JedisCommunication.broadcast(JedisCommunicationChannel.REFERENCE_UPDATE,
                                    ByteMessage.write(
                                            isStatic ? new Bytes(declaringClass.getName()).getData()
                                                    : instance.getKey(),
                                            new Bytes(fieldName), ((AbstractData<?>) newAbstractDataValue).getKey(),
                                            new Bytes(Byte.valueOf((byte) (isStatic ? 1 : 0)))));
                            AbstractData.notifyListeners(isStatic ? null : instance,
                                    new ReferenceUpdateEvent<AbstractData<?>>(Firecord.getId(),
                                            JedisCommunicationChannel.REFERENCE_UPDATE, isStatic ? null : instance,
                                            isStatic ? declaringClass : null, oldValue, newAbstractDataValue, fieldName, isStatic));

                        }

                    }
                }

            }
        }
        if (replacingObject == null) {
            joinPoint.proceed();
        } else {
            joinPoint.proceed(new Object[] { replacingObject });
        }
    }

}
