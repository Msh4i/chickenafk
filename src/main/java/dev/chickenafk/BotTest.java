package dev.chickenafk;

import dev.chickenafk.bot.Bot;
import dev.chickenafk.bot.Bots;
import dev.chickenafk.ui.PanelScreen;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

/**
 * Development only (-Dchickenafk.bottest=true, server on localhost:25599 from runServer). Drives the panel
 * like a player, lets a real bot join the local server, and uses RCON to play chickennw's part: the /gir
 * prompt, the AFK title and countdown, the sidebar kristal line, items, a chest, a kick. Every check logs
 * "TEST OK" or "TEST FAILED".
 */
final class BotTest {
	static final boolean ENABLED = Boolean.getBoolean("chickenafk.bottest");
	private static final String NAME = "TestBot";
	private static final String PASSWORD = "sifre123";
	/** chickennw's login prompt, in plain letters (the server writes it in small caps). */
	private static final String PROMPT = "chickennw » /gir sifre komutunu kullanarak giris yapin. Toplamda 3 deneme hakkiniz kaldi.";

	private record Step(String name, Runnable action, BooleanSupplier done, int timeoutTicks) {
	}

	private static final List<Step> STEPS = new ArrayList<>();
	private static int index = -1;
	private static int ticks;
	private static boolean failed;
	private static PanelScreen panel;
	private static Rcon rcon;
	private static BlockPos chest;

	private BotTest() {
	}

	private static Account account() {
		return Account.find(NAME);
	}

	private static Bot bot() {
		return Bots.of(account());
	}

	private static boolean has(ItemStack[] stacks, net.minecraft.world.item.Item item, int count) {
		for (ItemStack s : stacks) {
			if (s != null && s.is(item) && s.getCount() >= count) {
				return true;
			}
		}
		return false;
	}

	private static void rcon(String command) {
		try {
			if (rcon == null) {
				rcon = new Rcon("localhost", 25598, "chickenafk-test");
			}
			rcon.command(command);
		} catch (IOException e) {
			ChickenAfk.LOG.error("RCON command failed: {}", command, e);
		}
	}

	private static void shot(String name) {
		Minecraft mc = Minecraft.getInstance();
		Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), 1, msg -> {
		});
	}

	private static void step(String name, Runnable action, BooleanSupplier done, int timeoutTicks) {
		STEPS.add(new Step(name, action, done, timeoutTicks));
	}

	/** A screenshot a few frames into the step: the frame on screen during a tick is the one drawn before it. */
	private static void screenshot(String name) {
		step("screenshot " + name, () -> { }, () -> {
			if (ticks == 12) {
				shot(name);
			}
			return ticks > 14;
		}, 30);
	}

	private static void steps() {
		step("panel opens with no accounts", () -> {
			Minecraft mc = Minecraft.getInstance();
			panel = new PanelScreen(mc.screen);
			mc.setScreen(panel);
		}, () -> Account.all().isEmpty(), 20);
		step("(form filled)", () -> {
			panel.devClick("add", false);
			panel.devType(NAME);
			panel.devKey(GLFW.GLFW_KEY_TAB);
			panel.devType("x");
		}, () -> true, 2);
		screenshot("bottest-form.png");
		step("'Hesap ekle' form adds the account with its password", () -> {
			panel.devKey(GLFW.GLFW_KEY_BACKSPACE);
			panel.devType(PASSWORD);
			panel.devKey(GLFW.GLFW_KEY_ENTER);
		}, () -> account() != null && account().password.equals(PASSWORD) && Files.exists(Account.file()), 20);
		step("(drawer for the password button)", () -> panel.openDrawer(0, "GENEL"), () -> true, 10);
		step("'Değiştir' changes the saved password", () -> {
			panel.devClick("password", false);
			panel.devType("yeni456");
			panel.devKey(GLFW.GLFW_KEY_ENTER);
		}, () -> account().password.equals("yeni456"), 20);
		step("(password back for the /gir check)", () -> {
			panel.devClick("password", false);
			panel.devType(PASSWORD);
			panel.devKey(GLFW.GLFW_KEY_ENTER);
		}, () -> account().password.equals(PASSWORD), 20);
		step("'Hepsini başlat' connects the bot and it reaches play", () -> panel.devClick("start", false),
				() -> bot().inPlay(), 600);
		step("no /gir before the server asks, not even on 'takım üyesi … giriş yaptı'",
				() -> rcon("tellraw " + NAME + " \"chickennw » Takim uyesi Oyuncu, sunucuya giris yapti!\""),
				() -> ticks > 100 && !bot().sentCommand("/gir"), 130);
		step("server asks for /gir; the macro answers with the saved password",
				() -> rcon("tellraw " + NAME + " \"" + PROMPT + "\""),
				() -> bot().sentCommand("/gir " + PASSWORD), 200);
		step("a repeated prompt doesn't burn another of the 3 attempts",
				() -> rcon("tellraw " + NAME + " \"" + PROMPT.replace("3 deneme", "2 deneme") + "\""),
				() -> ticks > 100 && bot().sentCount("/gir") == 1, 130);
		step("'AFK bölgesine girdiniz' sets the smp-afkte flag",
				() -> rcon("tellraw " + NAME + " \"AFK bolgesine girdiniz! 3d sonra odul alacaksiniz!\""),
				() -> bot().flags().contains("smp-afkte"), 100);
		step("AFK title and 'sonraki ödül: 2d' subtitle: AFK state and ~2:59 countdown", () -> {
			rcon("title " + NAME + " subtitle \"sonraki odul: 2d\"");
			rcon("title " + NAME + " title \"AFK DURUMDASINIZ\"");
		}, () -> account().state == Account.State.AFK && account().nextReward() > 170 && account().nextReward() <= 180, 100);
		step("countdown 5s then 3d counts one reward", () -> {
			rcon("title " + NAME + " subtitle \"sonraki odul: 5s\"");
			rcon("title " + NAME + " title \"AFK DURUMDASINIZ\"");
			rcon("title " + NAME + " subtitle \"sonraki odul: 3d\"");
			rcon("title " + NAME + " title \"AFK DURUMDASINIZ\"");
		}, () -> account().rewards == 1, 100);
		step("sidebar 'KRISTAL 23.9k' line is read", () -> {
			rcon("team add kristal");
			rcon("team modify kristal prefix \"KRISTAL 23.9k\"");
		}, () -> "23.9k".equals(account().kristal), 100);
		step("given items show in the inventory", () -> {
			rcon("give " + NAME + " minecraft:diamond 5");
			panel.openDrawer(0, "ENVANTER");
		}, () -> has(account().inventory, Items.DIAMOND, 5), 100);
		screenshot("bottest-envanter.png");
		step("bot opens a chest next to it; the menu shows its items", () -> {
			chest = bot().blockPos().east(2);
			rcon("setblock " + chest.getX() + " " + chest.getY() + " " + chest.getZ() + " minecraft:chest");
			rcon("item replace block " + chest.getX() + " " + chest.getY() + " " + chest.getZ()
					+ " container.0 with minecraft:emerald 3");
		}, () -> true, 20);
		step("(menu opened)", () -> {
			bot().useBlock(chest);
			panel.openDrawer(0, "MENU");
		}, () -> account().menu != null && account().menu.filled && has(account().menu.slots, Items.EMERALD, 3), 100);
		screenshot("bottest-menu.png");
		step("shift-click in the menu moves the emeralds into the inventory",
				() -> panel.devClick("menu:0", true), () -> has(account().inventory, Items.EMERALD, 3), 100);
		step("chat typed in the panel reaches the server and comes back", () -> {
			panel.openDrawer(0, "SOHBET");
		}, () -> true, 5);
		step("(chat sent)", () -> {
			panel.devClick("chat", false);
			panel.devType("merhaba chickenafk");
			panel.devKey(GLFW.GLFW_KEY_ENTER);
		}, () -> account().chat.stream().anyMatch(l -> l.text().contains("merhaba chickenafk") && !l.note()), 100);
		step("long server lines wrap; the chat log file keeps them without the password",
				() -> rcon("tellraw " + NAME + " \"chickennw » Görünüşe bakılırsa Chicken Client kullanmıyorsun. Hemen sitemizden Chicken Client indirerek 2x coin şansı ve sınırsız bedava VIP üyelik gibi harika özellikler kazanabilirsin.\""),
				() -> {
					try {
						String log = Files.readString(Account.file().resolveSibling("sohbet.log"));
						return log.contains("sınırsız bedava VIP") && log.contains("/gir ••••") && !log.contains(PASSWORD);
					} catch (java.io.IOException e) {
						return false;
					}
				}, 100);
		screenshot("bottest-sohbet.png");
		step("kicked bot drops its menu and reconnects by itself", () -> rcon("kick " + NAME + " test kick"),
				() -> !bot().inPlay(), 100);
		step("(reconnected, no stale menu)", () -> { }, () -> bot().inPlay() && account().menu == null, 1200);
		step("(back to Genel)", () -> panel.openDrawer(0, "GENEL"), () -> true, 5);
		screenshot("bottest-genel.png");
		step("'Durdur' stops the bot", () -> panel.devClick("run", false),
				() -> account().state == Account.State.OFF, 100);
	}

	static void tick(Minecraft mc) {
		if (!ENABLED) {
			return;
		}
		ticks++;
		if (index == -1) {
			if (!(mc.screen instanceof TitleScreen) || ticks < 100) {
				return;
			}
			// fresh install: no accounts, tour already dismissed
			try {
				Files.deleteIfExists(Account.file());
				Files.deleteIfExists(Config.file());
			} catch (IOException e) {
				ChickenAfk.LOG.warn("could not reset test files", e);
			}
			Account.reload();
			Config.reload();
			Config.get().tourHidden = true;
			Config.get().save();
			steps();
			start(0);
			return;
		}
		if (index >= STEPS.size()) {
			if (ticks == 20) {
				ChickenAfk.LOG.info(failed ? "BOT TESTS DONE WITH FAILURES" : "BOT TESTS DONE, ALL OK");
				mc.stop();
			}
			return;
		}
		Step s = STEPS.get(index);
		boolean done;
		try {
			done = s.done().getAsBoolean();
		} catch (RuntimeException e) {
			done = false;
		}
		if (done) {
			if (!s.name().startsWith("(") && !s.name().startsWith("screenshot")) {
				ChickenAfk.LOG.info("TEST OK: {}", s.name());
			}
			start(index + 1);
		} else if (ticks > s.timeoutTicks()) {
			failed = true;
			ChickenAfk.LOG.error("TEST FAILED: {} (state {}, reason {})", s.name(),
					account() == null ? "-" : account().state, account() == null ? "-" : account().lastReason);
			start(STEPS.size());
		}
	}

	private static void start(int i) {
		index = i;
		ticks = 0;
		if (i < STEPS.size()) {
			STEPS.get(i).action().run();
		}
	}
}
