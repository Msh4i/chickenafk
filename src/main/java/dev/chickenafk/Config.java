package dev.chickenafk;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;

/** Settings kept in &lt;game dir&gt;/chickenafk/config.json. */
public final class Config {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static Config instance;

	/** Set when the player ticks "Bir daha gösterme" on the welcome screen or tour. */
	public boolean tourHidden;

	public static Config get() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	public static Path file() {
		return Minecraft.getInstance().gameDirectory.toPath().resolve(ChickenAfk.MOD_ID).resolve("config.json");
	}

	private static Config load() {
		Path file = file();
		if (Files.exists(file)) {
			try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				Config c = GSON.fromJson(r, Config.class);
				if (c != null) {
					return c;
				}
			} catch (IOException | RuntimeException e) {
				ChickenAfk.LOG.warn("Could not read {}, using defaults", file, e);
			}
		}
		return new Config();
	}

	public void save() {
		Path file = file();
		try {
			Files.createDirectories(file.getParent());
			try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
				GSON.toJson(this, w);
			}
		} catch (IOException e) {
			ChickenAfk.LOG.warn("Could not save {}", file, e);
		}
	}

	/** Forget the loaded copy, so the next {@link #get()} reads the file again. */
	static void reload() {
		instance = null;
	}
}
