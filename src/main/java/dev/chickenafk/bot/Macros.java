package dev.chickenafk.bot;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.chickenafk.ChickenAfk;
import dev.chickenafk.MacroStep;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * Runs the macros from chickenafk.json for one bot: triggers (mesaj, aralik, menu-acildi,
 * baglanti-koptu), conditions and actions, with waits handled by a queue instead of blocking.
 * The player's own commands from the macro tab run once after joining SMP, before going AFK.
 */
final class Macros {
	private record Macro(String name, String trigger, JsonObject triggerSpec, JsonObject condition, JsonArray actions) {
	}

	/** A running list of actions, resumed after waits. */
	private static final class Job {
		final LinkedList<JsonObject> actions = new LinkedList<>();
		final String message;
		final boolean userSteps;
		long resumeAt;

		Job(JsonArray actions, String message, boolean userSteps) {
			for (JsonElement e : actions) {
				if (e.isJsonObject()) {
					this.actions.add(e.getAsJsonObject());
				}
			}
			this.message = message;
			this.userSteps = userSteps;
		}
	}

	private record Seen(long at, String plain) {
	}

	private static final long HISTORY_MS = 5 * 60_000;
	private static final Random RANDOM = new Random();
	private static List<Macro> loaded;

	private final Bot bot;
	private final Set<String> flags = new HashSet<>();
	private final Deque<Seen> messages = new ArrayDeque<>();
	private final Deque<Seen> commands = new ArrayDeque<>();
	private final Map<Macro, Long> nextRun = new HashMap<>();
	private final Deque<Job> jobs = new ArrayDeque<>();
	private Job current;

	Macros(Bot bot) {
		this.bot = bot;
	}

	Set<String> flags() {
		return flags;
	}

	// --- loading ----------------------------------------------------------------------------

	private static List<Macro> macros() {
		if (loaded == null) {
			loaded = new ArrayList<>();
			Identifier id = Identifier.fromNamespaceAndPath(ChickenAfk.MOD_ID, "chickenafk.json");
			try (Reader r = Minecraft.getInstance().getResourceManager().openAsReader(id)) {
				for (JsonElement e : JsonParser.parseReader(r).getAsJsonObject().getAsJsonArray("makrolar")) {
					JsonObject m = e.getAsJsonObject();
					if (m.has("acik") && !m.get("acik").getAsBoolean()) {
						continue;
					}
					JsonObject trigger = m.getAsJsonObject("tetik");
					loaded.add(new Macro(str(m, "ad"), str(trigger, "tip"), trigger,
							m.has("kosul") ? m.getAsJsonObject("kosul") : new JsonObject(), m.getAsJsonArray("aksiyon")));
				}
			} catch (Exception e) {
				ChickenAfk.LOG.error("Could not load chickenafk.json macros", e);
			}
		}
		return loaded;
	}

	// --- triggers ---------------------------------------------------------------------------

	void tick(long now) {
		while (!messages.isEmpty() && now - messages.peekFirst().at() > HISTORY_MS) {
			messages.removeFirst();
		}
		while (!commands.isEmpty() && now - commands.peekFirst().at() > HISTORY_MS) {
			commands.removeFirst();
		}
		boolean busyWithUserSteps = current != null && current.userSteps || jobs.stream().anyMatch(j -> j.userSteps);
		for (Macro m : macros()) {
			if (!m.trigger().equals("aralik")) {
				continue;
			}
			Long due = nextRun.get(m);
			if (due == null || now >= due) {
				nextRun.put(m, now + jitter(m.triggerSpec()));
				// the player's own commands go first; the AFK flow waits for them
				if (due != null && !busyWithUserSteps && test(m.condition(), null, now)) {
					run(m, null);
				}
			}
		}
		runJobs(now);
	}

	void onMessage(String text) {
		long now = System.currentTimeMillis();
		messages.addLast(new Seen(now, Text.plain(text)));
		for (Macro m : macros()) {
			if (m.trigger().equals("mesaj") && test(m.condition(), text, now)) {
				run(m, text);
			}
		}
	}

	void onMenuOpened() {
		long now = System.currentTimeMillis();
		for (Macro m : macros()) {
			if (m.trigger().equals("menu-acildi") && test(m.condition(), null, now)) {
				run(m, null);
			}
		}
	}

	/** Disconnect macros run right away (they only reset flags); anything queued is dropped. */
	void onDisconnect() {
		jobs.clear();
		current = null;
		long now = System.currentTimeMillis();
		for (Macro m : macros()) {
			if (m.trigger().equals("baglanti-koptu") && test(m.condition(), null, now)) {
				run(m, null);
			}
		}
		runJobs(Long.MAX_VALUE);
		jobs.clear();
		current = null;
	}

	/** Whether a command containing the text was sent in the last few minutes. For the test run. */
	boolean sent(String text) {
		return sentCount(text) > 0;
	}

	int sentCount(String text) {
		String plain = Text.plain(text);
		return (int) commands.stream().filter(c -> c.plain().contains(plain)).count();
	}

	void recordCommand(String command) {
		commands.addLast(new Seen(System.currentTimeMillis(), Text.plain(command)));
	}

	void setFlag(String flag) {
		if (flags.add(flag) && flag.equals("smp-de")) {
			userSteps();
		}
	}

	/** The macro tab's own commands, 3 s apart, right after joining SMP. */
	private void userSteps() {
		JsonArray steps = new JsonArray();
		for (MacroStep s : MacroStep.USER) {
			if (s.enabled) {
				steps.add(action("bekle", "sure", "3s"));
				steps.add(action("komut", "metin", s.command));
			}
		}
		if (!steps.isEmpty()) {
			bot.account.note("Kendi komutların çalışıyor (" + steps.size() / 2 + ").");
			jobs.addLast(new Job(steps, null, true));
		}
	}

	private static JsonObject action(String type, String key, String value) {
		JsonObject o = new JsonObject();
		o.addProperty("tip", type);
		o.addProperty(key, value);
		return o;
	}

	private void run(Macro m, String message) {
		jobs.addLast(new Job(m.actions(), message, false));
	}

	// --- actions ----------------------------------------------------------------------------

	private void runJobs(long now) {
		while (true) {
			if (current == null) {
				current = jobs.pollFirst();
				if (current == null) {
					return;
				}
			}
			if (now < current.resumeAt) {
				return;
			}
			JsonObject a = current.actions.pollFirst();
			if (a == null) {
				current = null;
				continue;
			}
			act(a, current, now);
		}
	}

	private void act(JsonObject a, Job job, long now) {
		switch (str(a, "tip")) {
			case "yaz" -> bot.account.note(str(a, "metin"));
			case "komut" -> {
				String text = str(a, "metin");
				if (text.contains("{{sifre}}")) {
					if (bot.account.password.isEmpty()) {
						bot.account.note("Sunucu şifre istiyor ama bu hesaba şifre girilmemiş.");
						return;
					}
					text = text.replace("{{sifre}}", bot.account.password);
				}
				bot.say(text);
			}
			case "bekle" -> job.resumeAt = now + jitter(a);
			case "bayrak-koy" -> setFlag(str(a, "ad"));
			case "bayrak-sil" -> flags.remove(str(a, "ad"));
			case "item-tikla" -> {
				String name = str(a, "isim-icerir");
				if (!bot.clickMenuItem(name)) {
					bot.account.note("Menüde bulunamadı: " + name);
				}
			}
			case "eger" -> {
				boolean ok = test(a.getAsJsonObject("kosul"), job.message, now);
				JsonArray branch = ok ? a.getAsJsonArray("aksiyon") : a.getAsJsonArray("degilse");
				if (branch != null) {
					// run the branch next, before the rest of the list
					for (int i = branch.size() - 1; i >= 0; i--) {
						if (branch.get(i).isJsonObject()) {
							job.actions.addFirst(branch.get(i).getAsJsonObject());
						}
					}
				}
			}
			default -> ChickenAfk.LOG.warn("Unknown macro action {}", str(a, "tip"));
		}
	}

	// --- conditions -------------------------------------------------------------------------

	/** Every key must hold. Unknown keys fail, so a typo never makes a macro fire by accident. */
	private boolean test(JsonObject condition, String message, long now) {
		if (condition == null) {
			return true;
		}
		for (Map.Entry<String, JsonElement> c : condition.entrySet()) {
			JsonElement v = c.getValue();
			boolean ok = switch (c.getKey()) {
				case "regex" -> message != null && Pattern.compile(v.getAsString()).matcher(Text.lower(message)).find();
				case "icerir" -> message != null && Text.contains(message, v.getAsString());
				case "bagli" -> bot.inPlay() == v.getAsString().equals("evet");
				case "bayrak" -> flags.contains(v.getAsString());
				case "bayrak-yok" -> !flags.contains(v.getAsString());
				case "son-mesaj-icermez" -> !seen(messages, v.getAsJsonObject(), now);
				case "son-komut-icermez" -> !seen(commands, v.getAsJsonObject(), now);
				case "menu-item-var" -> bot.menuHas(v.getAsString());
				case "menu-item-yok" -> !bot.menuHas(v.getAsString());
				default -> {
					ChickenAfk.LOG.warn("Unknown macro condition {}", c.getKey());
					yield false;
				}
			};
			if (!ok) {
				return false;
			}
		}
		return true;
	}

	private static boolean seen(Deque<Seen> history, JsonObject spec, long now) {
		String text = Text.plain(str(spec, "metin"));
		long window = Text.millis(str(spec, "sure"));
		for (Seen s : history) {
			if (now - s.at() <= window && s.plain().contains(text)) {
				return true;
			}
		}
		return false;
	}

	private static long jitter(JsonObject spec) {
		long base = Text.millis(str(spec, "sure"));
		long spread = Text.millis(str(spec, "sapma"));
		return Math.max(0, base + (spread > 0 ? (long) ((RANDOM.nextDouble() * 2 - 1) * spread) : 0));
	}

	private static String str(JsonObject o, String key) {
		return o != null && o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
	}
}
