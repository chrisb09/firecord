package net.legendofwar.firecord.jedis.dataset.dataentry.object;

import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;
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

import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.dataset.Bytes;

public class TestObject extends AbstractObject {

    static DataGenerator<RDouble> dg = new DataGenerator<>(new Bytes("doublepool"), RDouble.class);

    private RInteger testint = new RInteger(new Bytes("testint"));
    public Integer ti = 7;

    public static AbstractData<?> overwrite_field;
    public static RInteger asnycCounter;

    private RInteger a = RInteger(25);
    private RInteger a_copy = RInteger(-100);

    public static RInteger a_static = RInteger(100);
    public static RInteger a_static_copy;

    public static RInteger nullt;

    RString b; // = null;
    public RBoolean c; // = null;
    RWrapper d;
    final RBoolean e = new RBoolean(new Bytes("exampleboolean"));

    static RDouble f;

    // This creates a new RDouble every time the class is initialized, meaning
    // they're also different on different nodes running at the same time
    RDouble g = dg.create(3.141);

    RVector h;

    REnum<DataType> i;

    RItemStack j;

    public TestObject(@NotNull Bytes key) {
        super(key);
        a_copy.set(-200);
        System.out.println("a: " + a);
        a.add(5);
        System.out.println("a: " + a);
        b.setIfEmpty("Hello World");
        d.setIfEmpty(new Bytes("testlist1"));
        f.setIfEmpty(23.4);
    }

    @Override
    public String toString() {
        System.out.println(b);
        return a + ":" + b + ":" + c + ":" + d + ":" + e + ":" + f + ":" + g + ":" + h + ":" + i + ":"
                + ((j != null) ? "length " + j.toString().length() : "not-loaded");
    }

    public RInteger getTestInt() {
        return testint;
    }

    public void incrA() {
        a.add(1);
    }

    public static void testChar(){

        Character character = ((char) ('a'+((int) (26*Math.random()))));

        System.out.println("character: "+character);
        System.out.println("  as short: "+(short) character.charValue());
        Bytes bytes = new Bytes((short) character.charValue(), Short.BYTES);
        System.out.println("  as bytes: "+bytes);
        byte[] bytearray = bytes.getData();
        System.out.println("bytearray: "+bytearray+" size: "+bytearray.length);
        System.out.println("  as bytes: "+new Bytes(bytes));
        long longdata = bytes.decodeNumber();
        System.out.println("  as long: "+longdata);
        short shortdata = (short) longdata;
        System.out.println("  as short: "+shortdata);
        char chardata = (char) shortdata;
        System.out.println("  as char: "+chardata);

    }

    public static void testEncodeDecode(){
        int minInt = Integer.MIN_VALUE;
        int maxInt = Integer.MAX_VALUE;
        long minLong = Long.MIN_VALUE;
        long maxLong = Long.MAX_VALUE;
        System.out.println(minInt+": "+new Bytes(minInt).decodeNumber());
        System.out.println(maxInt+": "+new Bytes(maxInt).decodeNumber());
        System.out.println(minLong+": "+new Bytes(minLong).decodeNumber());
        System.out.println(maxLong+": "+new Bytes(maxLong).decodeNumber());
        for (int i=1;i<=8;i++){
            System.out.println(i+". 200: "+new Bytes(200, i).decodeNumber());
        }
    }

    public static void testAsyncSet(){
        System.out.println("asyncCounter: "+asnycCounter);
        long start = System.nanoTime();
        asnycCounter.setAsync(asnycCounter.get()+1);
        long duration = System.nanoTime() - start;
        System.out.println("after asyncSet: "+asnycCounter);
        System.out.println("Time for asyncSet: "+duration+"ns");
    }

    public static void overwriteField(){
        int selection = (int) (Math.random() * 10);
        switch (selection){
            case 0: overwrite_field = RBoolean(false); break;
            case 1: overwrite_field = RByte((byte) 1); break;
            case 2: overwrite_field = RShort((short) 2); break;
            case 3: overwrite_field = RInteger(3); break;
            case 4: overwrite_field = RLong(4l); break;
            case 5: overwrite_field = RFloat(5f); break;
            case 6: overwrite_field = RDouble(6d); break;
            case 7: overwrite_field = RChar('7'); break;
            case 8: overwrite_field = RByteArray(new Bytes((byte) 8)); break;
            case 9: overwrite_field = RString("9"); break;
            default: overwrite_field = RString("default"); break;
        }
    }

    public static void toggleNullt(){
        if (nullt == null) {
            nullt = RInteger(((int) (100*Math.random())));
        } else {
            nullt = null;
        }
    }

    public void switchA() {
        RInteger temp = a;
        a = a_copy;
        a_copy = temp;
    }

    public String printA(){
        return "a: "+a+", a_copy:"+a_copy+"\na: "+a.getKey()+"\na_copy: "+a_copy.getKey();
    }

    public static void incrAStatic() {
        a_static.add(1);
    }

    public static void switchAStatic() {
        RInteger temp = a_static;
        a_static = a_static_copy;
        a_static_copy = temp;
    }

    public static String printAStatic(){
        return "a_static: "+a_static+", a_static_copy:"+a_static_copy+"\na_static: "+a_static.getKey()+"\na_static_copy: "+a_static_copy.getKey();
    }

    public void toggleE() {
        e.set(!e.get());
    }

    public void selectRandomTestlist() {
        int x = (1 + ((int) (Math.random() * 3)));
        System.out.println("Select testlist" + x);
        d.set(new Bytes("testlist" + x));
    }

    public void selectRandomDatatype() {
        i.set(DataType.values()[(int) (Math.random() * DataType.values().length)]);
    }

}
