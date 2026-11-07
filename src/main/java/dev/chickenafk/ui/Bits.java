package dev.chickenafk.ui;

import dev.chickenafk.Account;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Small pieces both the panel and the drawer draw: status line, kristal amount, spaced labels. */
final class Bits {
	private static final ItemStack SHARD = new ItemStack(Items.AMETHYST_SHARD);

	private Bits() {
	}

	/** Coloured dot plus status text; returns the x where it ends. */
	static float status(Canvas c, Account a, float x, float y) {
		c.fill(x, y + 4, x + 6, y + 10, statusColor(a));
		return x + 12 + c.text(Theme.SANS, statusText(a), x + 12, y, 12, Theme.SUB);
	}

	static float statusWidth(Canvas c, Account a) {
		return 12 + c.width(Theme.SANS, statusText(a), 12);
	}

	static int statusColor(Account a) {
		return switch (a.state) {
			case AFK -> Theme.OK;
			case JOINING, CONNECTING -> Theme.WAIT;
			case OFF -> Theme.OFF;
		};
	}

	static String statusText(Account a) {
		return switch (a.state) {
			case AFK -> "AFK alanında";
			case JOINING -> "SMP'ye giriyor";
			case CONNECTING -> "Bağlanıyor";
			case OFF -> "Kapalı";
		};
	}

	/** Kristal amount as the server writes it, followed by a shard icon. */
	static void kristal(Canvas c, Account a, float x, float y, int color) {
		float w = c.text(Theme.MONO, kristalText(a), x, y, 12, color);
		c.item(SHARD, x + w + 3, y - 1, 14);
	}

	static float kristalWidth(Canvas c, Account a) {
		return c.width(Theme.MONO, kristalText(a), 12) + 18;
	}

	/** Kristal as the server writes it; a dash until the sidebar has been seen. */
	static String kristalText(Account a) {
		return a.kristal == null ? "—" : a.kristal;
	}

	/** Text with extra space between letters, for the small uppercase labels. Returns the end x. */
	static float spaced(Canvas c, Theme.Font font, String s, float x, float y, float size, int color, float gap) {
		for (int i = 0; i < s.length(); i++) {
			x += c.text(font, String.valueOf(s.charAt(i)), x, y, size, color) + gap;
		}
		return x - gap;
	}

	static float spacedWidth(Canvas c, Theme.Font font, String s, float size, float gap) {
		float w = 0;
		for (int i = 0; i < s.length(); i++) {
			w += c.width(font, String.valueOf(s.charAt(i)), size) + gap;
		}
		return w - gap;
	}

	static float progress(Account a) {
		int left = a.nextReward();
		return left < 0 ? 0 : Math.max(0, Math.min(1, 1 - left / 180f));
	}

	/** Time to the next reward as mm:ss, or --:-- outside the AFK area. */
	static String countdown(Account a) {
		int s = a.nextReward();
		return s < 0 ? "--:--" : String.format("%02d:%02d", s / 60, s % 60);
	}
}
