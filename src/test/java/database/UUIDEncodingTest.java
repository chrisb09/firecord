package database;

import org.junit.jupiter.api.Test;

import net.legendofwar.firecord.jedis.dataset.Bytes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

public class UUIDEncodingTest {

    @Test
    public void testEncodeDecode() {
        
        UUID uuid = UUID.randomUUID();

        assertEquals(uuid, new Bytes(uuid).getUUID());

    }

}

