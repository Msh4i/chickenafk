package dev.chickenafk;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;

/**
 * One AFK account: what the player entered (name, /gir password) plus the live state its bot reports.
 * Saved entries live in &lt;game dir&gt;/chickenafk/hesaplar.json; the mod ships with none.
 */
public final class Account {
	public enum State { OFF, CONNECTING, JOINING, AFK }

	/** One chat line. Server lines have no sender; the mod's own notes use {@link #NOTE}. */
	public record ChatLine(String time, String sender, String text) {
		public boolean fromServer() {
			return sender == null;
		}

		public boolean note() {
			return NOTE.equals(sender);
		}
	}

	/** A container the server opened for this account (chest, server menu, ...). */
	public static final class Menu {
		public final int id;
		public final Component title;
		/** The menu's own slots, without the player inventory the server appends after them. */
		public ItemStack[] slots = new ItemStack[0];
		public int stateId;
		/** Set once the first contents arrived, so "menu opened" macros can look inside. */
		public boolean filled;

		public Menu(int id, Component title) {
			this.id = id;
			this.title = title;
		}
	}

	public static final String NOTE = "ChickenAFK";
	private static final int CHAT_LIMIT = 200;
	private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter LOG_CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");
	private static final long LOG_LIMIT = 2_000_000;

	/** Slot layout: 0-8 hotbar, 9-35 main inventory, 36-39 armor (helmet first). */
	public static final int HOTBAR = 0;
	public static final int MAIN = 9;
	public static final int ARMOR = 36;

	public final String name;
	public String password;
	public final Identifier skin;
	public final boolean slim;

	public volatile State state = State.OFF;
	/** Kristal balance exactly as the server's sidebar writes it ("23.9k"), or null if not seen yet. */
	public String kristal;
	/** When the account last entered the AFK area (ms), or 0. */
	public long afkSince;
	/** When the next reward is due (ms), from the AFK subtitle countdown, or 0. */
	public long nextRewardAt;
	public int rewards;
	public int ping;
	public int selectedSlot;
	public final ItemStack[] inventory = new ItemStack[40];
	public Menu menu;
	/** Why the last connection ended, shown in the drawer. */
	public String lastReason;
	public final List<ChatLine> chat = new ArrayList<>();

	public Account(String name, String password) {
		this.name = name;
		this.password = password == null ? "" : password;
		// offline accounts get the default skin the server would pick for their offline UUID
		PlayerSkin s = DefaultPlayerSkin.get(UUIDUtil.createOfflinePlayerUUID(name));
		this.skin = s.body().texturePath();
		this.slim = s.model() == PlayerModelType.SLIM;
		Arrays.fill(inventory, ItemStack.EMPTY);
	}

	public boolean running() {
		return state != State.OFF;
	}

	/** Seconds until the next reward, or -1 when not in the AFK area. */
	public int nextReward() {
		if (state != State.AFK || nextRewardAt == 0) {
			return -1;
		}
		return (int) Math.max(0, (nextRewardAt - System.currentTimeMillis() + 999) / 1000);
	}

	public String afkTime() {
		if (state != State.AFK || afkSince == 0) {
			return "—";
		}
		long minutes = (System.currentTimeMillis() - afkSince) / 60_000;
		return minutes < 60 ? minutes + " dk" : minutes / 60 + " sa " + minutes % 60 + " dk";
	}

	public void addChat(String sender, String text) {
		// a server can echo a command back (e.g. an error about "/gir <password>"); never show the password
		if (!password.isEmpty()) {
			text = text.replace(password, "••••");
		}
		chat.add(new ChatLine(LocalTime.now().format(CLOCK), sender, text));
		if (chat.size() > CHAT_LIMIT) {
			chat.removeFirst();
		}
		log(sender, text);
	}

	/**
	 * Appends the line to &lt;game dir&gt;/chickenafk/sohbet.log, so kicks and server messages can be read in
	 * full after the fact. Lines arrive here with the password already masked.
	 */
	private void log(String sender, String text) {
		String line = LocalTime.now().format(LOG_CLOCK) + "  " + name + "  " + (sender == null ? "" : sender + " » ") + text;
		try {
			Path file = Account.file().resolveSibling("sohbet.log");
			if (Files.exists(file) && Files.size(file) > LOG_LIMIT) {
				Files.move(file, file.resolveSibling("sohbet.old.log"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			}
			Files.writeString(file, line.replace('\n', ' ') + "\n", StandardCharsets.UTF_8,
					java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
		} catch (IOException ignored) {
			// the log is a convenience; chat in the panel keeps working without it
		}
	}

	/** A line from the mod itself (macro steps, connection events). */
	public void note(String text) {
		addChat(NOTE, text);
	}

	/** Clears everything that belonged to one connection; keeps chat and the last known kristal. */
	public void resetSession() {
		afkSince = 0;
		nextRewardAt = 0;
		ping = 0;
		menu = null;
		Arrays.fill(inventory, ItemStack.EMPTY);
	}

	// --- saved list ---------------------------------------------------------------------------

	private record Saved(String name, String password) {
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static List<Account> all;

	/** Every account the player added, in the order they were added. */
	public static List<Account> all() {
		if (all == null) {
			all = new ArrayList<>();
			for (Saved s : load()) {
				all.add(new Account(s.name(), s.password()));
			}
		}
		return all;
	}

	public static Path file() {
		return Minecraft.getInstance().gameDirectory.toPath().resolve(ChickenAfk.MOD_ID).resolve("hesaplar.json");
	}

	private static List<Saved> load() {
		Path file = file();
		if (!Files.exists(file)) {
			return List.of();
		}
		try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			List<Saved> list = GSON.fromJson(r, new TypeToken<List<Saved>>() { }.getType());
			return list == null ? List.of() : list;
		} catch (IOException | RuntimeException e) {
			ChickenAfk.LOG.warn("Could not read {}", file, e);
			return List.of();
		}
	}

	public static void save() {
		Path file = file();
		List<Saved> list = all().stream().map(a -> new Saved(a.name, a.password)).toList();
		try {
			Files.createDirectories(file.getParent());
			try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
				GSON.toJson(list, w);
			}
		} catch (IOException e) {
			ChickenAfk.LOG.warn("Could not save {}", file, e);
		}
	}

	/** Valid Minecraft name: 3-16 letters, digits or underscores. */
	public static boolean validName(String name) {
		return name.matches("[A-Za-z0-9_]{3,16}");
	}

	public static Account find(String name) {
		for (Account a : all()) {
			if (a.name.equalsIgnoreCase(name)) {
				return a;
			}
		}
		return null;
	}

	/** Forget the loaded list, so the next {@link #all()} reads the file again. For the test run. */
	static void reload() {
		all = null;
	}
}
