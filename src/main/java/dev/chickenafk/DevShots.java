package dev.chickenafk;

import dev.chickenafk.ui.MenuWidget;
import dev.chickenafk.ui.PanelScreen;
import dev.chickenafk.ui.Pose;
import dev.chickenafk.ui.PoseLabScreen;
import java.io.IOException;
import java.nio.file.Files;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;

/**
 * Development only. With -Dchickenafk.shots=true the game drives the UI like a player (clicks, keys,
 * typing), checks the results, screenshots each step into run/screenshots and quits. Checks log
 * "TEST OK" or "TEST FAILED". With -Dchickenafk.lab=pose it only screenshots that pose from four cameras.
 */
final class DevShots {
	static final boolean ENABLED = Boolean.getBoolean("chickenafk.shots") || System.getProperty("chickenafk.lab") != null;
	private static final String LAB = System.getProperty("chickenafk.lab");
	private static final String[] TABS = {"GENEL", "ENVANTER", "SOHBET", "MAKRO"};

	private enum Stage { TITLE, WELCOME, TOUR, POSES, TABS, REOPEN, LAB, QUIT }

	private static Stage stage = Stage.TITLE;
	private static int ticks;
	private static int index;
	private static PanelScreen panel;
	private static boolean failed;

	private DevShots() {
	}

	static void tick(Minecraft mc) {
		if (!ENABLED) {
			return;
		}
		ticks++;
		switch (stage) {
			case TITLE -> title(mc);
			case WELCOME -> {
				if (ticks == 30) {
					shot(mc, "chickenafk-welcome.png");
					panel.devKey(GLFW.GLFW_KEY_ENTER);
					check(panel.tourStep() == 0, "Enter on the welcome screen starts the tour");
					next(Stage.TOUR);
				}
			}
			case TOUR -> tour(mc);
			case POSES -> {
				// give each maker pose time to ease in before the picture
				if (ticks >= 50) {
					shot(mc, "chickenafk-panel-" + Pose.MAKER.get(index).name + ".png");
					if (++index < Pose.MAKER.size()) {
						panel.setMakerPose(Pose.MAKER.get(index));
						ticks = 0;
					} else {
						index = 0;
						panel.openDrawer(0, TABS[0]);
						next(Stage.TABS);
					}
				}
			}
			case TABS -> {
				if (ticks >= 30) {
					shot(mc, "chickenafk-drawer-" + TABS[index].toLowerCase() + ".png");
					if (++index < TABS.length) {
						panel.openDrawer(0, TABS[index]);
						ticks = 0;
					} else {
						next(Stage.REOPEN);
					}
				}
			}
			case REOPEN -> {
				if (ticks == 5) {
					check(!new PanelScreen(null).tourActive(), "tour stays hidden after 'Bir daha gösterme'");
					next(Stage.QUIT);
				}
			}
			case LAB -> {
				if (ticks >= 30) {
					shot(mc, "chickenafk-lab-" + LAB + ".png");
					next(Stage.QUIT);
				}
			}
			case QUIT -> {
				if (ticks >= 20) {
					ChickenAfk.LOG.info(failed ? "TESTS DONE WITH FAILURES" : "TESTS DONE, ALL OK");
					mc.stop();
				}
			}
		}
	}

	private static void title(Minecraft mc) {
		if (!(mc.screen instanceof TitleScreen) || ticks < 120) {
			return;
		}
		if (LAB != null) {
			Pose p = Pose.byName(LAB);
			mc.setScreen(new PoseLabScreen(p, p.face));
			next(Stage.LAB);
			return;
		}
		// start from a fresh install: no saved settings, then a few accounts (never started) to look at
		try {
			Files.deleteIfExists(Config.file());
			Files.deleteIfExists(Account.file());
		} catch (IOException e) {
			ChickenAfk.LOG.warn("could not delete test files", e);
		}
		Config.reload();
		Account.reload();
		for (String name : new String[] {"MsHai_1", "Tavuk_2", "Horoz_3", "Civciv_4", "Yumurta_5"}) {
			Account.all().add(new Account(name, ""));
		}
		shot(mc, "chickenafk-title.png");
		// open the panel the way a player would: click the main menu card
		for (var child : mc.screen.children()) {
			if (child instanceof MenuWidget w) {
				mc.screen.mouseClicked(new MouseButtonEvent(w.getX() + w.getWidth() / 2.0,
						w.getY() + w.getHeight() / 2.0, new MouseButtonInfo(0, 0)), false);
			}
		}
		if (!(mc.screen instanceof PanelScreen opened)) {
			check(false, "clicking the main menu card opens the panel");
			next(Stage.QUIT);
			return;
		}
		check(true, "clicking the main menu card opens the panel");
		panel = opened;
		check(panel.tourActive() && panel.tourStep() == -1, "first opening shows the welcome screen");
		next(Stage.WELCOME);
	}

	private static void tour(Minecraft mc) {
		int last = PanelScreen.tourSteps() - 1;
		if (ticks == 25) {
			shot(mc, "chickenafk-tour-" + (index + 1) + ".png");
			if (index < last) {
				panel.devKey(GLFW.GLFW_KEY_RIGHT);
				check(panel.tourStep() == index + 1, "right arrow goes to tour step " + (index + 2));
				index++;
				ticks = 0;
			} else {
				panel.devClickNeverBox();
			}
		} else if (ticks == 35 && index == last) {
			shot(mc, "chickenafk-tour-never.png");
			panel.devKey(GLFW.GLFW_KEY_ENTER);
			check(!panel.tourActive(), "Bitir closes the tour");
			Config.reload();
			check(Config.get().tourHidden, "'Bir daha gösterme' is saved to config.json");
			index = 0;
			panel.setMakerPose(Pose.MAKER.get(0));
			next(Stage.POSES);
		}
	}

	private static void check(boolean ok, String what) {
		if (!ok) {
			failed = true;
		}
		ChickenAfk.LOG.info("{} {}", ok ? "TEST OK:" : "TEST FAILED:", what);
	}

	private static void next(Stage s) {
		stage = s;
		ticks = 0;
	}

	private static void shot(Minecraft mc, String name) {
		Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), 1, msg -> {
		});
	}
}
