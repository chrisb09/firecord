package net.legendofwar.firecord.jedis.dataset.dataentry.simple;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import net.legendofwar.firecord.jedis.dataset.dataentry.DataType;

public class RItemStack extends LargeData<ItemStack> {

    public RItemStack(String key, @NotNull ItemStack defaultValue) {
        super(key, defaultValue, DataType.ITEMSTACK);
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
    public String toString(){
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
