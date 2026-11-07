package dev.chickenafk;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/**
 * Writes on-screen server text (titles, action bar, boss bars, sidebar) to a log with timestamps,
 * so we can learn the exact AFK-area wording and timing before parsing it for shard counting.
 * Each source only logs when its text changes. A research tool, so it is off unless the game is started
 * with -Dchickenafk.capture=true.
 */
public final class TextCapture {
	private static final boolean ENABLED = Boolean.getBoolean("chickenafk.capture");
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final String SMALL_CAPS = "ᴀʙᴄᴅᴇꜰғɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱsᴛᴜᴠᴡxʏᴢ";
	private static final String LATIN = "abcdeffghijklmnopqrsstuvwxyz";

	private static final Map<String, String> last = new HashMap<>();
	private static Writer out;
	private static int ticks;

	private TextCapture() {
	}

	public static void text(String kind, Component text) {
		if (text != null) {
			text(kind, text.getString());
		}
	}

	public static void text(String kind, String text) {
		if (!ENABLED || text == null || text.equals(last.put(kind, text))) {
			return;
		}
		write(kind, text);
	}

	/** Logs the sidebar exactly as it is drawn: vanilla order, team prefix/suffix, top 15 lines. */
	public static void tickSidebar(Minecraft mc) {
		if (!ENABLED || mc.level == null || ++ticks % 40 != 0) {
			return;
		}
		Scoreboard board = mc.level.getScoreboard();
		Objective objective = board.getDisplayObjective(DisplaySlot.SIDEBAR);
		if (objective == null) {
			return;
		}
		NumberFormat format = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);
		String lines = board.listPlayerScores(objective).stream()
				.filter(e -> !e.isHidden())
				.sorted(Comparator.comparingInt(PlayerScoreEntry::value).reversed()
						.thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER))
				.limit(15)
				.map(e -> {
					String name = PlayerTeam.formatNameForTeam(board.getPlayersTeam(e.owner()), e.ownerName()).getString();
					String value = e.formatValue(format).getString();
					return value.isBlank() ? name : name + " " + value;
				})
				.collect(Collectors.joining(" | "));
		text("SB", objective.getDisplayName().getString() + " :: " + lines);
	}

	/** For busy sources (chat, sidebar): keep only lines that look AFK or shard related. */
	public static void ifRelevant(String kind, String text) {
		if (ENABLED && text != null && isRelevant(text)) {
			text(kind, text);
		}
	}

	public static void ifRelevant(String kind, Component text) {
		if (text != null) {
			ifRelevant(kind, text.getString());
		}
	}

	static boolean isRelevant(String text) {
		String plain = normalize(text);
		return plain.contains("afk") || plain.contains("shard") || plain.contains("parca")
				|| plain.contains("parça") || plain.contains("dakika") || plain.matches(".*\\d+\\s*(dk|dakika|m|min)\\b.*");
	}

	/** Server text is often in small caps; map it to plain letters for matching. */
	static String normalize(String text) {
		StringBuilder b = new StringBuilder(text.length());
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			int k = SMALL_CAPS.indexOf(c);
			b.append(k >= 0 ? LATIN.charAt(k) : c);
		}
		return b.toString().toLowerCase(Locale.ROOT);
	}

	private static void write(String kind, String text) {
		try {
			if (out == null) {
				Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve(ChickenAfk.MOD_ID);
				Files.createDirectories(dir);
				out = Files.newBufferedWriter(dir.resolve("ekran-yazilari.log"), StandardCharsets.UTF_8,
						StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			}
			out.write(LocalDateTime.now().format(TIME) + "  " + String.format("%-9s", kind) + "  "
					+ text.replace('\n', '|') + System.lineSeparator());
			out.flush();
		} catch (IOException e) {
			ChickenAfk.LOG.warn("Could not write screen text log", e);
		}
	}
}
