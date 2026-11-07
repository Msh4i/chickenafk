package dev.chickenafk;

import dev.chickenafk.ui.MenuWidget;
import dev.chickenafk.ui.PanelScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

/**
 * Dev-only check of the Esc menu card: ./gradlew runClient -Ppause joins the local test server (start it with
 * runServer first), presses Esc, screenshots the pause menu, opens the panel from the card, closes it again and
 * expects to land back on the pause menu.
 */
final class PauseTest {
	private static final boolean ENABLED = Boolean.getBoolean("chickenafk.pausetest");
	private static int ticks;
	private static int step;

	private PauseTest() {
	}

	static void tick(Minecraft mc) {
		if (!ENABLED || mc.level == null || mc.player == null) {
			return;
		}
		ticks++;
		switch (step) {
			case 0 -> {
				// let the world settle before pausing
				if (ticks > 100) {
					mc.pauseGame(false);
					next();
				}
			}
			case 1 -> {
				if (ticks == 20) {
					check(mc.screen instanceof PauseScreen, "Esc opens the pause menu");
					shot(mc, "chickenafk-pause.png");
				}
				if (ticks == 24) {
					MenuWidget card = mc.screen == null ? null : mc.screen.children().stream()
							.filter(MenuWidget.class::isInstance).map(MenuWidget.class::cast).findFirst().orElse(null);
					check(card != null, "the pause menu has the ChickenAFK card");
					if (card != null) {
						mc.screen.mouseClicked(new MouseButtonEvent(card.getX() + card.getWidth() / 2.0,
								card.getY() + card.getHeight() / 2.0, new MouseButtonInfo(0, 0)), false);
					}
					check(mc.screen instanceof PanelScreen, "clicking the card opens the panel");
					next();
				}
			}
			case 2 -> {
				if (ticks == 30) {
					shot(mc, "chickenafk-pause-panel.png");
					mc.screen.onClose();
					check(mc.screen instanceof PauseScreen, "closing the panel goes back to the pause menu");
					ChickenAfk.LOG.info("PAUSE TESTS DONE");
					mc.stop();
				}
			}
			default -> {
			}
		}
	}

	private static void next() {
		step++;
		ticks = 0;
	}

	private static void check(boolean ok, String what) {
		ChickenAfk.LOG.info("{} {}", ok ? "TEST OK:" : "TEST FAILED:", what);
	}

	private static void shot(Minecraft mc, String name) {
		Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), 1, msg -> {
		});
	}
}
