package oxidizing.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import oxidizing.Main;

public class ConfigIO {

    private static final File CONFIG_FILE = new File("config/oxidizing.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static ModConfig CURRENT = new ModConfig();

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                // An empty file parses to null
                if (loaded != null) CURRENT = loaded;
            } catch (IOException | RuntimeException e) {
                Main.LOGGER.error("Failed to load config file: {}", CONFIG_FILE.getName(), e);
            }
        } else {
            save();
        }
    }

    public static void save() {
        // The config folder doesn't exist on a fresh dedicated server
        CONFIG_FILE.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(CURRENT, writer);
        } catch (IOException e) {
            Main.LOGGER.error("Failed to save config file: {}", CONFIG_FILE.getName(), e);
        }
    }
}
