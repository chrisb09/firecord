package net.legendofwar.firecord.jedis.dataset.dataentry.object;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;

@Aspect
public class FieldListener {

    public static long variableChanges = 0l;
    
    @After("set(!static !final * net.legendofwar.firecord.jedis.dataset.dataentry.object.AbstractObject+.*) && target(instance)")
    public void afterVariableChange(JoinPoint joinPoint, net.legendofwar.firecord.jedis.dataset.dataentry.object.AbstractObject instance) {
        FieldListener.variableChanges++;
        //Object newValue = joinPoint.getArgs()[0];
        //FieldSignature fieldSignature = (FieldSignature) joinPoint.getSignature();
        //String fieldName = fieldSignature.getName();
        //System.out.println("Variable '" + fieldName + "' changed in instance of " + instance.getClass().getSimpleName() + ": " + newValue);
    }

}
