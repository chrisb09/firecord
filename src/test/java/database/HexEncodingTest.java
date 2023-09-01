package database;

import org.junit.jupiter.api.Test;

import net.legendofwar.firecord.jedis.dataset.Bytes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

public class HexEncodingTest {

    @Test
    public void testEncodeDecode() {
        
        Bytes data = new Bytes(UUID.randomUUID()); // random 16 Byte byte[]

        assertEquals(data, Bytes.byHexString(data.toString()));
    }

}

