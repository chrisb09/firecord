package net.legendofwar.firecord.jedis.dataset.dataentry.object;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.legendofwar.firecord.tool.NodeType;

// TODO: currently unused

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ClassAnnotation {
    NodeType restrictedTo() default NodeType.ANY;
    String overwriteName() default ""; //represents null in this case
    boolean synchronize() default true; // if false, we dont send updates of this class and objects of this class to other nodes
}