package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;

import net.legendofwar.firecord.jedis.dataset.Bytes;

public final class RItemStack extends DynamicLargeData<ItemStack> {

    final static ItemStack DEFAULT_VALUE = new ItemStack(Material.AIR, 1);

    public RItemStack(@NotNull Bytes key) {
        this(key, DEFAULT_VALUE);
    }

    public RItemStack(@NotNull Bytes key, @NotNull ItemStack value) {
        super(key, value);
    }

    @Override
    protected Bytes toBytes() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(this.value);

            dataOutput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Bytes(outputStream.toByteArray());
    }

    @Override
    protected void fromBytes(@NotNull byte[] value) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(value);
        try {
            BukkitObjectInputStream inputData = new BukkitObjectInputStream(inputStream);
            ItemStack itemStack = null;
            Object input = inputData.readObject();
            if (input != null) {
                itemStack = (ItemStack) input;
            }
            inputData.close();

            this.value = itemStack;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        get();
        return this.value.toString();
    }

}
