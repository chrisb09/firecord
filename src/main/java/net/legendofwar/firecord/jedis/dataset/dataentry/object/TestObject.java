package net.legendofwar.firecord.jedis.dataset.dataentry.object;

import net.legendofwar.firecord.jedis.dataset.dataentry.DataGenerator;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RBoolean;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RDouble;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RInteger;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RItemStack;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RString;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RVector;
import net.legendofwar.firecord.jedis.dataset.dataentry.simple.RWrapper;

import java.lang.Math;

public class TestObject extends AbstractObject {

    private RInteger testint = new RInteger("testint");
    public Integer ti = 7;

    private RInteger a; // = RInteger(25);
    RString b; // = null;
    public RBoolean c; // = null;
    RWrapper d;
    final RBoolean e = new RBoolean("exampleboolean");

    static RDouble f;

    // This creates a new RDouble every time the class is initialized, meaning
    // they're also different on different nodes running at the same time
    RDouble g = (RDouble) DataGenerator.createAnonymous(DataType.DOUBLE, (Double) 3.141);

    RVector h;

    REnum<DataType> i;

    RItemStack j;

    public TestObject(String key) {
        super(key);
        b.setIfEmpty("Hello World");
        d.setIfEmpty("testlist1");
        f.setIfEmpty(23.4);
    }

    @Override
    public String toString() {
        System.out.println(b);
        return a + ":" + b + ":" + c + ":" + d + ":" + e + ":" + f + ":" + g + ":" + h + ":" + i + ":"
                + ((j != null) ? j.toString() : "null");
    }

    public RInteger getTestInt() {
        return testint;
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
        i.set(DataType.values()[(int) (Math.random() * DataType.values().length)]);
    }

}
