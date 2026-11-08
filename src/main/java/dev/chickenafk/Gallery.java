package dev.chickenafk;

import dev.chickenafk.ui.PanelScreen;
import dev.chickenafk.ui.Pose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Development only: ./gradlew runClient -Pgallery takes the store page pictures into run/screenshots, with a few
 * demo accounts that look mid-session (no bots are started). With -Pgallery=world it joins the
 * local test server and picture the Esc menu.
 */
final class Gallery {
	private static final boolean ENABLED = Boolean.getBoolean("chickenafk.gallery");
	private static final String[] TABS = {"GENEL", "ENVANTER", "SOHBET"};

	private static int ticks;
	private static int step;
	private static PanelScreen panel;

	private Gallery() {
	}

	static void tick(Minecraft mc) {
		if (!ENABLED) {
			return;
		}
		ticks++;
		if (mc.level != null) {
			inWorld(mc);
		} else if (step > 0 || mc.screen instanceof TitleScreen) {
			menus(mc);
		}
	}

	private static void menus(Minecraft mc) {
		switch (step) {
			case 0 -> {
				if (ticks > 120) {
					demo();
					next();
				}
			}
			case 1 -> {
				if (ticks == 40) {
					shot(mc, "galeri-1-ana-menu.png");
					panel = new PanelScreen(mc.screen);
					mc.setScreen(panel);
					panel.setMakerPose(Pose.SIT);
					next();
				}
			}
			case 2 -> {
				// the sitting pose eases in
				if (ticks == 80) {
					shot(mc, "galeri-2-panel.png");
					panel.openDrawer(0, TABS[0]);
					next();
				}
			}
			case 3, 4, 5 -> {
				if (ticks == 30) {
					shot(mc, "galeri-" + (step) + "-" + TABS[step - 3].toLowerCase() + ".png");
					if (step < 5) {
						panel.openDrawer(0, TABS[step - 2]);
						next();
					} else {
						mc.stop();
					}
				}
			}
			default -> {
			}
		}
	}

	private static void inWorld(Minecraft mc) {
		if (step == 0 && ticks == 5) {
			// a calm, sunny test world: nothing attacks the player while the picture is taken
			try (Rcon rcon = new Rcon("localhost", 25598, "chickenafk-test")) {
				for (String command : new String[] {"difficulty peaceful", "time set noon", "weather clear",
						"kill @e[type=!player]", "gamemode creative @a", "tp @a 0 -60 0 0 10"}) {
					rcon.command(command);
				}
			} catch (java.io.IOException e) {
				ChickenAfk.LOG.warn("RCON failed", e);
			}
		}
		if (step == 0 && ticks % 20 == 0 && mc.player.isDeadOrDying()) {
			// the test player may have logged out dead last time
			mc.player.respawn();
			mc.setScreen(null);
		}
		if (step == 0 && ticks > 200 && !mc.player.isDeadOrDying()) {
			demo();
			// no tutorial hints or recipe toasts over the picture
			mc.getTutorial().setStep(TutorialSteps.NONE);
			mc.getToastManager().clear();
			mc.pauseGame(false);
			next();
		} else if (step == 1 && ticks == 40) {
			shot(mc, "galeri-6-esc.png");
			mc.stop();
		}
	}

	/** Accounts that look like a running session: three in the AFK area, one joining, one off. */
	private static void demo() {
		Config.get().tourHidden = true;
		Account.all().clear();
		long now = System.currentTimeMillis();
		String[] names = {"MsHai_1", "Tavuk_2", "Horoz_3", "Civciv_4", "Yumurta_5"};
		String[] kristal = {"23.9k", "18.2k", "9.4k", "1.3k", null};
		for (int i = 0; i < names.length; i++) {
			Account a = new Account(names[i], "x");
			a.kristal = kristal[i];
			if (i < 3) {
				a.state = Account.State.AFK;
				a.afkSince = now - (47 - i * 13) * 60_000L;
				a.nextRewardAt = now + (130 - i * 41) * 1000L;
				a.rewards = 15 - i * 4;
				a.ping = 38 + i * 7;
			} else if (i == 3) {
				a.state = Account.State.JOINING;
				a.ping = 52;
			}
			Account.all().add(a);
		}
		Account a = Account.all().getFirst();
		ItemStack[] items = {
				new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.BOW),
				new ItemStack(Items.COOKED_BEEF, 32), new ItemStack(Items.GOLDEN_APPLE, 6), new ItemStack(Items.TORCH, 64),
				new ItemStack(Items.ENDER_PEARL, 12), new ItemStack(Items.OAK_LOG, 48), new ItemStack(Items.WATER_BUCKET)};
		System.arraycopy(items, 0, a.inventory, Account.HOTBAR, items.length);
		a.inventory[Account.MAIN] = new ItemStack(Items.EMERALD, 37);
		a.inventory[Account.MAIN + 1] = new ItemStack(Items.IRON_INGOT, 64);
		a.inventory[Account.MAIN + 2] = new ItemStack(Items.GOLD_INGOT, 21);
		a.inventory[Account.MAIN + 3] = new ItemStack(Items.ARROW, 64);
		a.inventory[Account.MAIN + 9] = new ItemStack(Items.COBBLESTONE, 64);
		a.inventory[Account.MAIN + 10] = new ItemStack(Items.COBBLESTONE, 64);
		a.inventory[Account.MAIN + 18] = new ItemStack(Items.EXPERIENCE_BOTTLE, 16);
		a.inventory[Account.ARMOR] = new ItemStack(Items.DIAMOND_BOOTS);
		a.inventory[Account.ARMOR + 1] = new ItemStack(Items.DIAMOND_LEGGINGS);
		a.inventory[Account.ARMOR + 2] = new ItemStack(Items.DIAMOND_CHESTPLATE);
		a.inventory[Account.ARMOR + 3] = new ItemStack(Items.DIAMOND_HELMET);
		a.note("Bağlanıyor: oyna.chickennw.com");
		a.note("Giriş tamam, sunucuya bağlanıldı.");
		a.note("/gir ••••");
		a.note("SMP'ye katılınıyor (/joinq smp-spawn).");
		a.note("/joinq smp-spawn");
		a.addChat(null, "Fiyuuu smp-afk-1 sunucusuna gidiyorsun.");
		a.addChat(null, "AFK bölgesine girdiniz! 3d sonra ödül alacaksınız!");
		a.note("AFK bölgesindeyim — ödül sayacı başladı.");
		a.addChat("Oyuncu", "selam herkese");
		a.note("Ödül alındı (1. kez bu oturumda).");
		a.addChat("MsHai_1", "as");
	}

	private static void next() {
		step++;
		ticks = 0;
	}

	private static void shot(Minecraft mc, String name) {
		Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), 1, msg -> {
		});
	}
}
