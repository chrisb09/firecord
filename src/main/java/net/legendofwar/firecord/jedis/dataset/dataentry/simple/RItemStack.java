package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

public final class RItemStack extends DynamicLargeData<ItemStack> {

    final static ItemStack DEFAULT_VALUE = new ItemStack(Material.AIR, 1);

    public RItemStack(@NotNull String key) {
        this(key, null);
    }

    public RItemStack(@NotNull String key, ItemStack defaultValue) {
        super(key, defaultValue);
    }

    @Override
    protected void fromString(@NotNull String value) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decode(value));
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
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(this.value);

            dataOutput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new String(Base64Coder.encode(outputStream.toByteArray()));
    }

}
