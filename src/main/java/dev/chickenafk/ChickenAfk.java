package dev.chickenafk;

import com.mojang.blaze3d.platform.InputConstants;
import dev.chickenafk.bot.Bots;
import dev.chickenafk.ui.PanelScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ChickenAfk implements ClientModInitializer {
	public static final String MOD_ID = "chickenafk";
	public static final Logger LOG = LoggerFactory.getLogger("ChickenAFK");

	@Override
	public void onInitializeClient() {
		LOG.info("ChickenAFK loaded");
		UpdateCheck.start();
	}

	public static void onClientTick(Minecraft mc) {
		TextCapture.tickSidebar(mc);
		Bots.tick();
		DevShots.tick(mc);
		BotTest.tick(mc);
		PauseTest.tick(mc);
		while (Keys.OPEN_PANEL.consumeClick()) {
			if (mc.screen == null) {
				mc.setScreen(new PanelScreen(null));
			}
		}
	}

	/** Created lazily: Options.load() can run before our entrypoint. */
	public static final class Keys {
		public static final KeyMapping.Category CATEGORY =
				KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
		public static final KeyMapping OPEN_PANEL = new KeyMapping(
				"key.chickenafk.open_panel", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY);

		private Keys() {
		}
	}
}
