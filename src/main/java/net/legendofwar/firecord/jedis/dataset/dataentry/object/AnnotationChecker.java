package net.legendofwar.firecord.jedis.dataset.dataentry.object;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.legendofwar.firecord.Firecord;

public class AnnotationChecker {
    
    public static ClassAnnotation get(Class<?> clazz){
        Annotation[] annotations = clazz.getAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation instanceof ClassAnnotation) {
                ClassAnnotation yourAnnotation = (ClassAnnotation) annotation;
                return yourAnnotation;
            }
        }
        return null;
    }

    public static boolean isFieldRestricted(Field field){
        // Check if the field itself has the RestrictedTo annotation
        RestrictedTo r = field.getAnnotation(RestrictedTo.class);
        if (r != null && !r.type().includes(Firecord.getNodeType())){
            return true;
        }
        // Check if the type of the field has the RestrictedTo annotation
        r = field.getType().getAnnotation(RestrictedTo.class);
        if (r != null && !r.type().includes(Firecord.getNodeType())){
            return true;
        }

        ClassAnnotation classAnnotation = get(field.getType());
        if (classAnnotation != null && !classAnnotation.restrictedTo().includes(Firecord.getNodeType())){
            return true;
        }
        return false;
    }

    public static boolean isSynchronizationEnabled(Class<?> clazz) {
        if (clazz == null) {
            return true; // by default, don't prohibit synchronzation
        }
        ClassAnnotation annotation = get(clazz);
        return annotation == null || annotation.synchronize(); // Default to true if annotation is absent
    }

    public static boolean isStaticInitFunction(Method method){
        return method.isAnnotationPresent(StaticInit.class);
    }

}


