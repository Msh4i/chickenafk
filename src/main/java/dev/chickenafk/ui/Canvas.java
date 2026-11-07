package dev.chickenafk.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Draws in a fixed 1100x619 design space (the size the panel was designed at), scaled and centred
 * into whatever the game window is. Everything inside a frame uses design units.
 */
public final class Canvas {
	public static final float W = 1100;
	public static final float H = 619;

	private final Minecraft mc = Minecraft.getInstance();
	private GuiGraphics g;
	/** GUI units per design unit, and where the design space starts in GUI units. */
	private float k;
	private float ox;
	private float oy;

	public void begin(GuiGraphics g, int guiWidth, int guiHeight) {
		begin(g, guiWidth, guiHeight, 0.5f);
	}

	/**
	 * align 0.5 centres the design space in the window; 1 pins its bottom-right corner to the window's,
	 * for things that belong in a screen corner whatever the window's shape.
	 */
	public void begin(GuiGraphics g, int guiWidth, int guiHeight, float align) {
		this.g = g;
		k = Math.min(guiWidth / W, guiHeight / H);
		ox = (guiWidth - W * k) * align;
		oy = (guiHeight - H * k) * align;
		g.pose().pushMatrix();
		g.pose().translate(ox, oy);
		g.pose().scale(k, k);
	}

	/** Explicit placement: design point (0, 0) at GUI (originX, originY), scale GUI units per design unit. */
	public void beginAt(GuiGraphics g, float originX, float originY, float scale) {
		this.g = g;
		k = scale;
		ox = originX;
		oy = originY;
		g.pose().pushMatrix();
		g.pose().translate(ox, oy);
		g.pose().scale(k, k);
	}

	public void end() {
		g.pose().popMatrix();
	}

	public GuiGraphics graphics() {
		return g;
	}

	/** Design units to GUI units, for vanilla calls that ignore the pose (3D skins). */
	public int guiX(float x) {
		return Math.round(ox + x * k);
	}

	public int guiY(float y) {
		return Math.round(oy + y * k);
	}

	/** GUI-space mouse position to design units. */
	public float designX(double mouseX) {
		return (float) ((mouseX - ox) / k);
	}

	public float designY(double mouseY) {
		return (float) ((mouseY - oy) / k);
	}

	public void fill(float x0, float y0, float x1, float y1, int color) {
		g.pose().pushMatrix();
		g.pose().translate(x0, y0);
		g.pose().scale((x1 - x0) / 64f, (y1 - y0) / 64f);
		g.fill(0, 0, 64, 64, color);
		g.pose().popMatrix();
	}

	/** 1 design unit border, drawn inside the box. */
	public void box(float x0, float y0, float x1, float y1, int color) {
		fill(x0, y0, x1, y0 + 1, color);
		fill(x0, y1 - 1, x1, y1, color);
		fill(x0, y0, x0 + 1, y1, color);
		fill(x1 - 1, y0, x1, y1, color);
	}

	public void dashedBox(float x0, float y0, float x1, float y1, int color) {
		for (float x = x0; x < x1; x += 8) {
			fill(x, y0, Math.min(x + 4, x1), y0 + 1, color);
			fill(x, y1 - 1, Math.min(x + 4, x1), y1, color);
		}
		for (float y = y0; y < y1; y += 8) {
			fill(x0, y, x0 + 1, Math.min(y + 4, y1), color);
			fill(x1 - 1, y, x1, Math.min(y + 4, y1), color);
		}
	}

	/** Draws text with its top-left at (x, y) at the given design size; returns its width. */
	/** The raster that matches how many real screen pixels this text size covers right now. */
	private Theme.Raster raster(Theme.Font font, float size) {
		return font.pick(size * k * mc.getWindow().getGuiScale());
	}

	public float text(Theme.Font font, String s, float x, float y, float size, int color) {
		Theme.Raster r = raster(font, size);
		Component c = r.text(s);
		float scale = size / r.size();
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		g.drawString(mc.font, c, 0, 0, color, false);
		g.pose().popMatrix();
		return mc.font.width(c) * scale;
	}

	public float width(Theme.Font font, String s, float size) {
		Theme.Raster r = raster(font, size);
		return mc.font.width(r.text(s)) * size / r.size();
	}

	/** Splits text into lines no wider than maxWidth, breaking between words. */
	public java.util.List<String> wrap(Theme.Font font, String s, float size, float maxWidth) {
		java.util.List<String> lines = new java.util.ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : s.split(" ")) {
			String next = line.isEmpty() ? word : line + " " + word;
			if (!line.isEmpty() && width(font, next, size) > maxWidth) {
				lines.add(line.toString());
				line = new StringBuilder(word);
			} else {
				line = new StringBuilder(next);
			}
		}
		if (!line.isEmpty()) {
			lines.add(line.toString());
		}
		return lines;
	}

	/** The y to draw text at so its capitals sit vertically centred on cy (measured: caps centre at y + 0.235 size). */
	public static float middle(float cy, float size) {
		return cy - 0.235f * size;
	}

	/** Text centred both ways on (cx, cy), e.g. a button label. */
	public void label(Theme.Font font, String s, float cx, float cy, float size, int color) {
		textCentered(font, s, cx, middle(cy, size), size, color);
	}

	public void textCentered(Theme.Font font, String s, float cx, float y, float size, int color) {
		text(font, s, cx - width(font, s, size) / 2, y, size, color);
	}

	public void textRight(Theme.Font font, String s, float right, float y, float size, int color) {
		text(font, s, right - width(font, s, size), y, size, color);
	}

	/** An item icon with its top-left at (x, y), drawn size x size design units. */
	public void item(ItemStack stack, float x, float y, float size) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(size / 16f, size / 16f);
		g.renderItem(stack, 0, 0);
		g.pose().popMatrix();
	}

	/** The front of a player's head (face plus hat layer) from a 64x64 skin. */
	public void face(Identifier skin, float x, float y, float size) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(size / 8f, size / 8f);
		g.blit(RenderPipelines.GUI_TEXTURED, skin, 0, 0, 8, 8, 8, 8, 64, 64);
		g.blit(RenderPipelines.GUI_TEXTURED, skin, 0, 0, 40, 8, 8, 8, 64, 64);
		g.pose().popMatrix();
	}

	/** A 64x64 white icon texture, tinted, drawn size x size design units. */
	public void icon(Identifier texture, float x, float y, float size, int color) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(size / 64f, size / 64f);
		g.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0, 0, 64, 64, 64, 64, color);
		g.pose().popMatrix();
	}

	public static boolean inside(float x, float y, float x0, float y0, float x1, float y1) {
		return x >= x0 && x < x1 && y >= y0 && y < y1;
	}
}
