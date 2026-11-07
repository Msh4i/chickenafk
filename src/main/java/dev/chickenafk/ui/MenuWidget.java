package dev.chickenafk.ui;

import dev.chickenafk.Account;
import dev.chickenafk.UpdateCheck;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Bottom-right card on the title screen: the running accounts standing in a row, and a button that
 * opens the panel. Bots keep running in the main menu, so this is where the player sees them first.
 */
public final class MenuWidget extends AbstractWidget {
	/** Card bounds in the panel's design space, anchored bottom-right above the copyright line. */
	private static final float X0 = 742;
	private static final float Y0 = 412;
	private static final float X1 = 1082;
	private static final float Y1 = 588;
	private static final int MAX_SHOWN = 4;

	private final Screen parent;
	private final Canvas c = new Canvas();
	private final List<Figure> figures = new ArrayList<>();

	public MenuWidget(Screen parent) {
		super(0, 0, 0, 0, Component.translatable("chickenafk.button.open"));
		this.parent = parent;
		for (int i = 0; i < MAX_SHOWN; i++) {
			figures.add(null);
		}
	}

	@Override
	protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		var window = Minecraft.getInstance().getWindow();
		int gw = window.getGuiScaledWidth();
		int gh = window.getGuiScaledHeight();
		// Sit in the bottom-right corner, above the copyright line, without covering vanilla's buttons.
		// The card fits either beside the button column (200 wide, centred, plus its round side buttons)
		// or below it (the column ends at height/4 + 152); take whichever lets it be bigger.
		float right = gw - 6;
		float bottom = gh - 14;
		float design = Math.min(gw / Canvas.W, gh / Canvas.H);
		float beside = (right - (gw / 2f + 100 + 24 + 8)) / (X1 - X0);
		float below = Math.min((right - 6) / (X1 - X0), (bottom - (gh / 4f + 160)) / (Y1 - Y0));
		float scale = Math.min(design, Math.max(beside, below));
		c.beginAt(g, right - X1 * scale, bottom - Y1 * scale, scale);
		// keep the clickable area in step with the drawn card
		setX(c.guiX(X0));
		setY(c.guiY(Y0));
		setWidth(c.guiX(X1) - getX());
		setHeight(c.guiY(Y1) - getY());
		boolean over = isHovered();
		float lift = over ? -2 : 0;

		c.fill(X0, Y0 + lift, X1, Y1 + lift, 0xD6000000);
		c.box(X0, Y0 + lift, X1, Y1 + lift, over ? Theme.LINE3 : Theme.LINE2);

		List<Account> running = Account.all().stream().filter(a -> a.state != Account.State.OFF).toList();
		long afk = running.stream().filter(a -> a.state == Account.State.AFK).count();
		Bits.spaced(c, Theme.SEMI, "CHICKENAFK", X0 + 14, Y0 + 12 + lift, 11.5f, Theme.TEXT, 2.2f);
		// a newer release takes the corner; the panel's top bar has the download button
		String update = UpdateCheck.latest();
		String live = update != null ? "v" + update + " çıktı" : afk + " AFK · " + running.size() + " açık";
		float lw = c.width(Theme.MONO, live, 11.5f);
		c.text(Theme.MONO, live, X1 - 14 - lw, Y0 + 12 + lift, 11.5f, update != null ? Theme.TEXT : Theme.SUB);
		c.fill(X1 - 26 - lw, Y0 + 15 + lift, X1 - 20 - lw, Y0 + 21 + lift, update != null || !running.isEmpty() ? Theme.OK : Theme.OFF);
		c.fill(X0, Y0 + 34 + lift, X1, Y0 + 35 + lift, Theme.LINE);

		// running accounts, standing side by side
		int n = Math.min(MAX_SHOWN, running.size());
		if (n == 0) {
			c.textCentered(Theme.SANS, "Henüz çalışan hesap yok", (X0 + X1) / 2, Y0 + 76 + lift, 12, Theme.MUTE);
		}
		float figW = 40;
		float figH = 80;
		float gap = 26;
		float start = (X0 + X1) / 2 - (n * figW + (n - 1) * gap) / 2;
		float[] turns = {-24, -8, 8, 24};
		for (int i = 0; i < n; i++) {
			Account a = running.get(i);
			Figure f = figures.get(i);
			if (f == null) {
				f = new Figure(a.slim, Pose.IDLE);
				figures.set(i, f);
			}
			float x = start + i * (figW + gap);
			f.render(g, a.skin, c.guiX(x), c.guiY(Y0 + 46 + lift), c.guiX(x + figW), c.guiY(Y0 + 46 + figH + lift), turns[i]);
		}
		c.fill(X0, Y0 + 132 + lift, X1, Y0 + 133 + lift, Theme.LINE);

		// open button across the bottom
		float by0 = Y0 + 142 + lift;
		float by1 = Y1 - 10 + lift;
		c.fill(X0 + 12, by0, X1 - 12, by1, over ? 0xFFD9D9D9 : Theme.TEXT);
		c.label(Theme.SEMI, "Paneli aç", (X0 + X1) / 2, (by0 + by1) / 2, 13, Theme.BLACK);
		c.end();
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		Minecraft.getInstance().setScreen(new PanelScreen(parent));
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
