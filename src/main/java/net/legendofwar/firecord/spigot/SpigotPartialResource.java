package net.legendofwar.firecord.spigot;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import net.legendofwar.firecord.jedis.PartialResource;
import net.legendofwar.firecord.jedis.dataset.Bytes;
import net.legendofwar.firecord.jedis.dataset.dataentry.AbstractData;

public class SpigotPartialResource extends PartialResource {

    HashMap<String, List<Bytes>> unavailable = new HashMap<>();
    HashMap<Plugin, List<AbstractData<?>>> loaded = new HashMap<>();

    SpigotPartialResource() {

    }

    @Override
    public void registerLoad(Class<?> clazz, AbstractData<?> ad) {
        Plugin p = getPluginFromClass(clazz);
        if (p != null) {
            if (!loaded.containsKey(p)) {
                loaded.put(p, new ArrayList<AbstractData<?>>());
            }
            loaded.get(p).add(ad);
        }
    }

    @Override
    public void registerUnavailableLoad(String className, Bytes bytes) {
        if (!unavailable.containsKey(className)) {
            unavailable.put(className, new ArrayList<Bytes>());
        }
        unavailable.get(className).add(bytes);
    }

    public static Plugin getPluginFromClass(Class<?> targetClass) {
        try {
            // Get the class loader of the target class
            ClassLoader classLoader = targetClass.getClassLoader();

            // If the class loader is an instance of URLClassLoader (common in Java)
            if (classLoader instanceof URLClassLoader) {
                @SuppressWarnings("resource")
                URLClassLoader urlClassLoader = (URLClassLoader) classLoader;

                // Iterate through the URLs in the class loader
                for (URL url : urlClassLoader.getURLs()) {
                    // Convert the URL to a File
                    File file = new File(url.toURI());

                    // Try to get the Plugin instance using reflection
                    Plugin plugin = getPluginFromFile(file);
                    if (plugin != null) {
                        return plugin;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static Plugin getPluginFromFile(File file) {
        try {
            // Get the plugin by its JAR file
            Plugin plugin = Bukkit.getPluginManager().getPlugin(file.getName());
            if (plugin != null) {
                return plugin;
            }

            // Use reflection to access the getFile method
            Method getFileMethod = JavaPlugin.class.getDeclaredMethod("getFile");
            getFileMethod.setAccessible(true);

            // Iterate through all plugins to find the one with a matching JAR file
            for (Plugin loadedPlugin : Bukkit.getPluginManager().getPlugins()) {
                File loadedPluginFile = (File) getFileMethod.invoke(loadedPlugin);
                if (loadedPluginFile.equals(file)) {
                    return loadedPlugin;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

}
