package net.legendofwar.firecord.jedis.dataset.dataentry.object;

import net.legendofwar.firecord.jedis.dataset.dataentry.DataPool;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RBoolean;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RDouble;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RInteger;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RString;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RVector;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RWrapper;

import java.lang.Math;

public class TestObject extends AbstractObject {

    private RInteger a;
    RString b;
    public final RBoolean c = null;
    RWrapper d;
    final RBoolean e = new RBoolean("exampleboolean", true);

    static RDouble f;

    // This creates a new RDouble every time the class is initialized, meaning
    // they're also different on different nodes running at the same time
    RDouble g = (RDouble) DataPool.createAnonymous(DataType.DOUBLE, (Double) 3.141);

    RVector h;

    REnum<DataType> i;

    public TestObject(String key) {
        super(key);
        b.setIfNull("Hello World");
        d.setIfNull("testlist1");
        f.setIfNull(23.4);
    }

    @Override
    public String toString() {
        return a + ":" + b + ":" + c + ":" + d + ":" + e + ":" + f + ":" + g + ":" + h + ":" + i;
    }

    public void incrA() {
        a.add(1);
    }

    public void toggleE() {
        e.set(!e.get());
    }

    public void selectRandomTestlist() {
        int x = (1 + ((int) (Math.random() * 3)));
        System.out.println("Select testlist" + x);
        d.set("testlist" + x);
    }

    public void selectRandomDatatype() {
        i.set(DataType.values()[(int) (Math.random()*DataType.values().length)]);
    }

}
