package database;

import org.junit.jupiter.api.Test;

import net.legendofwar.firecord.jedis.dataset.Bytes;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NumberEncodingTest {

    @Test
    public void testEncodeDecode() {
        
        test(Integer.MIN_VALUE);
        test(Integer.MAX_VALUE);
        test(Long.MIN_VALUE);
        test(Long.MAX_VALUE);
        test(0);
        test(100);
        test(127);
        test(128);
        test(-1);
        test(-127);

    }

    public double log2(double x){
        return Math.log(x) / Math.log(2);
    }


    public void test(long value){

        int bytes = 1;
        if (value > 0) {
            long val = 127;
            while (val << (8*(bytes-1)) < value && bytes < 8){
                bytes++;
            }
        } else if (value < 0) {
            long val = -128;
            while (val << (8*(bytes-1)) > value && bytes < 8){
                bytes++;
            }
        }

        System.out.println("Bytes: "+bytes);
        if (bytes <= 0 || bytes > 8){
            bytes = 8;
        }
        for (int i=8;i>=bytes;i--){
            assertEquals(value, new Bytes(value, bytes).decodeNumber());
            System.out.println(value + " == decode(encode("+value+", "+i+"))");
        }


    }

}

