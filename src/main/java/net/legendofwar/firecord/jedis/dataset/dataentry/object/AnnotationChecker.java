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
        RestrictedTo r = field.getAnnotation(RestrictedTo.class);
        if (r == null){
            return false;
        }
        if (!r.type().includes(Firecord.getNodeType())){
            return false;
        }
        r = field.getClass().getAnnotation(RestrictedTo.class);
        if (r == null){
            return false;
        }
        if (!r.type().includes(Firecord.getNodeType())){
            return false;
        }
        return true;
    }

    public static boolean isStaticInitFunction(Method method){
        return method.isAnnotationPresent(StaticInit.class);
    }

}


