package net.legendofwar.firecord.jedis.dataset.dataentry.object;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import net.legendofwar.firecord.Firecord;

public class AnnotationChecker {

    public static ClassAnnotation get(Class<?> clazz) {
        Annotation[] annotations = clazz.getAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation instanceof ClassAnnotation) {
                ClassAnnotation yourAnnotation = (ClassAnnotation) annotation;
                return yourAnnotation;
            }
        }
        return null;
    }

    public static boolean isFieldRestricted(Field field) {
        RestrictedTo r = field.getAnnotation(RestrictedTo.class);
        if (r == null) {
            return false;
        }
        return !r.type().includes(Firecord.getNodeType());
    }

    public static boolean isParallelLoadAllowed(Field field) {
        InitParallel r = field.getAnnotation(InitParallel.class);
        if (r == null) {
            return false;
        }
        return true;
    }

    public static boolean isParallelLoadAllowed(Class<?> clazz) {
        return clazz.isAnnotationPresent(InitParallel.class);
    }

}
