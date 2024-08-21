package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.DataGenerator;

public final class RCharacter extends SmallData<Character> {

    final static DataGenerator<RCharacter> GENERATOR = new DataGenerator<>(new Bytes("rcharacter"), RCharacter.class);

    final static RCharacter create() {
        return GENERATOR.create();
    }

    final static RCharacter create(Vector defaultValue) {
        return GENERATOR.create(defaultValue);
    }

    final static void delete(RCharacter object) {
        DataGenerator.delete(object);
    }

    final static Character DEFAULT_VALUE = ' ';

    public RCharacter(@NotNull Bytes key) {
        this(key, null);
    }

    public RCharacter(@NotNull Bytes key, Character defaultValue) {
        super(key, defaultValue);
    }

    @Override
    protected Bytes toBytes() {
        return new Bytes((short) this.value.charValue(), Short.BYTES);
    }

    @Override
    protected void fromBytes(byte[] value) {
        this.value = (char) (short) new Bytes(value).decodeNumber();
    }

    @Override
    public String toString() {
        return "" + this.value;
    }

    public double getSortScore(){
        return this.value;
    }

}
