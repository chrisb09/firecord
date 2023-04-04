package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import org.bukkit.util.Vector;

public final class RVector extends SmallData<Vector> {

    final static Vector DEFAULT_VALUE = new Vector();

    public RVector(String key) {
        this(key, null);
    }

    public RVector(String key, Vector defaultValue) {
        super(key, defaultValue);
    }

    @Override
    protected void fromString(String value) {
        String[] parts = value.split(":");
        Double[] components = new Double[3];
        for (int i = 0; i < 3; i++) {
            components[i] = Double.parseDouble(parts[i]);
        }
        this.value = new Vector(components[0], components[1], components[2]);
    }

    @Override
    public String toString() {
        return value.getX() + ":" + value.getY() + ":" + value.getZ();
    }

}
