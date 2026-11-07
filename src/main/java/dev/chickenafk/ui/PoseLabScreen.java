package dev.chickenafk.ui;

import dev.chickenafk.ChickenAfk;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Development only: one pose drawn large from four cameras, for tuning angles from screenshots. */
public final class PoseLabScreen extends Screen {
	private static final float[][] VIEWS = {{-6, 0}, {-22, -35}, {-6, -90}, {-80, 0}};
	private static final String[] NAMES = {"front", "3/4", "side", "top"};

	private final Figure[] figures = new Figure[VIEWS.length];
	private final Identifier skin;

	public PoseLabScreen(Pose pose, int face) {
		super(Component.literal("pose lab"));
		for (int i = 0; i < figures.length; i++) {
			figures[i] = new Figure(true, pose);
		}
		skin = Identifier.fromNamespaceAndPath(ChickenAfk.MOD_ID, "textures/maker/" + face + ".png");
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		g.fill(0, 0, width, height, 0xFF202020);
		int w = width / VIEWS.length;
		int h = Math.min(height - 20, w * 2);
		for (int i = 0; i < VIEWS.length; i++) {
			int x0 = i * w;
			g.fill(x0, 10, x0 + w - 2, 10 + h, 0xFF2C2C2C);
			// floor line at the box bottom, so clipping is visible
			g.fill(x0, 10 + h - 1, x0 + w - 2, 10 + h, 0xFFFF5555);
			figures[i].renderView(g, skin, x0, 10, x0 + w - 2, 10 + h, VIEWS[i][0], VIEWS[i][1]);
			g.drawString(font, NAMES[i], x0 + 4, 12, 0xFFFFFFFF);
		}
	}
}
