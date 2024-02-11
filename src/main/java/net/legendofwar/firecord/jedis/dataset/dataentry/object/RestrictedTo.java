package net.legendofwar.firecord.jedis.dataset.dataentry.object;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.legendofwar.firecord.tool.NodeType;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface RestrictedTo {
    NodeType type() default NodeType.ANY;
}