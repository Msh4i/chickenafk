package dev.chickenafk.ui;

import dev.chickenafk.ChickenAfk;
import dev.chickenafk.Config;
import java.util.List;
import java.util.Random;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * First-run welcome and walkthrough. MsHai greets the player, then a spotlight steps through the panel
 * while a small MsHai stands on the tip card pointing at it. Shown on every panel opening until the
 * player ticks "Bir daha gösterme".
 */
final class Tour {
	/** What the tour needs from the panel. */
	interface Host {
		/** Design-space rectangle of a panel part, or null for a step with no target. */
		float @Nullable [] target(String id);

		void showDrawer(boolean open);
	}

	private record Step(@Nullable String target, String title, String body) {
	}

	private static final List<Step> STEPS = List.of(
			new Step("row", "Hesapların burada", "Her hesap kendi karakteriyle duruyor. Birine tıkla, envanteri, sohbeti ve makrosu açılır."),
			new Step("hero", "Seçili hesap", "Adı, durumu ve sonraki shard'a kalan süre. Sayaç AFK alanına girince başlar, her 3 dakikada bir shard gelir."),
			new Step("start", "Tek tuşla başlat", "Hesaplar sırayla bağlanır, giriş yapar, SMP'ye geçer ve AFK alanına kendileri girer. Kopan geri döner."),
			new Step("drawer", "Hesabın detayı", "Envanter, sohbet ve makro burada. AFK'den önce çalışacak komutları makroya eklersin."),
			new Step("creator", "Bu benim", "Modu ben yaptım. Bir sorun olursa ya da aklına bir şey gelirse buradan yaz."),
			new Step(null, "Ana menüde de çalışır", "Dünyaya girmesen de hesaplar çalışmaya devam eder. Ana menüde sağ alttaki karttan, oyunda K tuşuyla açarsın."));

	private static final float CARD_W = 300;
	private static final float GUIDE_H = 92;
	private static final Random RANDOM = new Random();

	private enum Phase { OFF, WELCOME, STEPS }

	private final Canvas c;
	private final Host host;
	private final Figure greeter;
	private final Figure guide = new Figure(true, Pose.POINT_RIGHT);
	private Phase phase = Phase.OFF;
	private int step;
	private boolean never;
	private float[] startHit = new float[4];
	private float[] laterHit = new float[4];
	private float[] neverHit = new float[4];
	private float[] backHit = new float[4];
	private float[] nextHit = new float[4];

	Tour(Canvas c, Host host) {
		this.c = c;
		this.host = host;
		Pose p;
		do {
			p = Pose.randomMaker(RANDOM, null);
		} while (p == Pose.SIT);
		greeter = new Figure(true, p);
	}

	void start() {
		phase = Phase.WELCOME;
		step = 0;
	}

	boolean active() {
		return phase != Phase.OFF;
	}

	/** Index of the step on screen, or -1 on the welcome screen. For the screenshot run. */
	int step() {
		return phase == Phase.STEPS ? step : -1;
	}

	static int stepCount() {
		return STEPS.size();
	}

	/** Centre of the "Bir daha gösterme" box, for the screenshot run's click test. */
	float[] neverBoxCenter() {
		return new float[] {(neverHit[0] + neverHit[2]) / 2, (neverHit[1] + neverHit[3]) / 2};
	}

	void render(float mx, float my) {
		if (phase == Phase.WELCOME) {
			welcome(mx, my);
		} else if (phase == Phase.STEPS) {
			steps(mx, my);
		}
	}

	private void welcome(float mx, float my) {
		c.fill(-2000, -2000, 3100, 2619, 0xFA000000);
		Identifier skin = skin(greeter.pose().face);
		c.fill(150, 418, 278, 420, 0x33FFFFFF);
		greeter.render(c.graphics(), skin, c.guiX(146), c.guiY(160), c.guiX(274), c.guiY(416), 20);

		float x = 440;
		c.text(Theme.SEMI, "Selam, ben MsHai.", x, 170, 44, Theme.TEXT);
		float y = 236;
		for (String line : c.wrap(Theme.SANS, "Bu mod hesaplarını senin yerine chickennw'de AFK alanına sokar. Nasıl çalıştığını 30 saniyede göstereyim.", 15, 480)) {
			c.text(Theme.SANS, line, x, y, 15, Theme.SUB);
			y += 23;
		}
		y += 16;
		String[] items = {"Hesabını ekle", "Hepsini başlat'a bas", "Shard'lar kendiliğinden birikir"};
		c.fill(x, y, x + 500, y + 1, Theme.LINE2);
		for (int i = 0; i < items.length; i++) {
			c.text(Theme.MONO, "0" + (i + 1), x, y + 14, 12, Theme.MUTE);
			c.text(Theme.SANS, items[i], x + 42, y + 12, 14, Theme.TEXT);
			c.fill(x, y + 40, x + 500, y + 41, Theme.LINE);
			y += 41;
		}
		y += 22;
		float sw = c.width(Theme.SEMI, "Turu başlat", 13) + 52;
		startHit = new float[] {x, y, x + sw, y + 38};
		button("Turu başlat", startHit, true, true, mx, my);
		float lw = c.width(Theme.SEMI, "Şimdi değil", 13) + 32;
		laterHit = new float[] {x + sw + 10, y, x + sw + 10 + lw, y + 38};
		button("Şimdi değil", laterHit, false, false, mx, my);
		checkbox(x + 500, y + 12);
	}

	private void steps(float mx, float my) {
		Step s = STEPS.get(step);
		float[] t = s.target() == null ? null : host.target(s.target());
		float x0;
		float y0;
		float x1;
		float y1;
		if (t != null) {
			x0 = t[0] - 8;
			y0 = t[1] - 8;
			x1 = t[2] + 8;
			y1 = t[3] + 8;
			// dim everything around the target
			int shade = 0xD2000000;
			c.fill(-2000, -2000, 3100, y0, shade);
			c.fill(-2000, y1, 3100, 2619, shade);
			c.fill(-2000, y0, x0, y1, shade);
			c.fill(x1, y0, 3100, y1, shade);
			corners(x0, y0, x1, y1);
		} else {
			c.fill(-2000, -2000, 3100, 2619, 0xD2000000);
			x0 = Canvas.W / 2;
			x1 = x0;
			y0 = Canvas.H / 2 - 40;
			y1 = y0;
		}

		List<String> body = c.wrap(Theme.SANS, s.body(), 13, CARD_W - 32);
		float ch = 16 + 2 + 14 + 24 + body.size() * 20 + 14 + 30 + 16;
		float tx;
		float ty;
		if (t == null) {
			tx = (Canvas.W - CARD_W) / 2;
			ty = (Canvas.H - ch) / 2 + 30;
		} else {
			// first place that fits: below the target (guide on the card, pointing up), then to its
			// right, then its left, then above
			float centred = Math.min(Canvas.W - CARD_W - 10, Math.max(10, (x0 + x1) / 2 - CARD_W / 2));
			float middle = y0 + Math.max(0, (y1 - y0 - ch) / 2);
			if (y1 + GUIDE_H + 16 + ch <= Canvas.H - 10) {
				tx = centred;
				ty = y1 + GUIDE_H + 16;
			} else if (x1 + 24 + CARD_W <= Canvas.W - 10) {
				tx = x1 + 24;
				ty = middle;
			} else if (x0 - 24 - CARD_W >= 10) {
				tx = x0 - 24 - CARD_W;
				ty = middle;
			} else {
				tx = centred;
				ty = y0 - ch - 24;
			}
		}
		ty = Math.min(ty, Canvas.H - ch - 8);
		// the guide stands on top of the card; if there is no room above, beside it
		boolean side = ty < GUIDE_H + 8;
		ty = Math.max(side ? 10 : GUIDE_H + 8, ty);

		c.fill(tx, ty, tx + CARD_W, ty + ch, Theme.BLACK);
		c.box(tx, ty, tx + CARD_W, ty + ch, Theme.LINE3);
		float px = tx + 16;
		float segW = (CARD_W - 32 - (STEPS.size() - 1) * 4) / STEPS.size();
		for (int i = 0; i < STEPS.size(); i++) {
			float sx = px + i * (segW + 4);
			c.fill(sx, ty + 16, sx + segW, ty + 18, i <= step ? Theme.TEXT : Theme.LINE2);
		}
		c.text(Theme.SEMI, s.title(), px, ty + 32, 18, Theme.TEXT);
		float by = ty + 60;
		for (String line : body) {
			c.text(Theme.SANS, line, px, by, 13, Theme.SUB);
			by += 20;
		}
		float rowY = ty + ch - 46;
		boolean last = step == STEPS.size() - 1;
		backHit = new float[4];
		if (step > 0) {
			float bw = c.width(Theme.SANS, "← Geri", 13);
			backHit = new float[] {px, rowY + 6, px + bw, rowY + 26};
			c.text(Theme.SANS, "← Geri", px, rowY + 9, 13, Canvas.inside(mx, my, px, rowY, px + bw, rowY + 30) ? Theme.TEXT : Theme.MUTE);
		}
		String label = last ? "Bitir" : "İleri";
		float nw = c.width(Theme.SEMI, label, 13) + (last ? 28 : 44);
		nextHit = new float[] {tx + CARD_W - 16 - nw, rowY, tx + CARD_W - 16, rowY + 30};
		button(label, nextHit, true, !last, mx, my);
		if (last) {
			checkbox(nextHit[0] - 14, rowY + 8);
		}

		// guide points from the card toward the target
		boolean right = t != null && (x0 + x1) / 2 > tx + CARD_W / 2;
		guide.setPose(right ? Pose.POINT_RIGHT : Pose.POINT_LEFT);
		float gw = 45;
		float gx = side ? (right ? tx - gw - 12 : tx + CARD_W + 12) : (right ? tx + CARD_W - gw - 14 : tx + 14);
		float gy = side ? ty + ch - GUIDE_H : ty - GUIDE_H + 2;
		guide.render(c.graphics(), skin(5), c.guiX(gx), c.guiY(gy), c.guiX(gx + gw), c.guiY(gy + GUIDE_H), right ? 28 : -28);
	}

	/** White L-shaped corners around the spotlight. */
	private void corners(float x0, float y0, float x1, float y1) {
		float l = 18;
		float w = 2;
		c.fill(x0, y0, x0 + l, y0 + w, Theme.TEXT);
		c.fill(x0, y0, x0 + w, y0 + l, Theme.TEXT);
		c.fill(x1 - l, y0, x1, y0 + w, Theme.TEXT);
		c.fill(x1 - w, y0, x1, y0 + l, Theme.TEXT);
		c.fill(x0, y1 - w, x0 + l, y1, Theme.TEXT);
		c.fill(x0, y1 - l, x0 + w, y1, Theme.TEXT);
		c.fill(x1 - l, y1 - w, x1, y1, Theme.TEXT);
		c.fill(x1 - w, y1 - l, x1, y1, Theme.TEXT);
	}

	/** "Bir daha gösterme" box, its right edge at {@code right}. */
	private void checkbox(float right, float y) {
		String label = "Bir daha gösterme";
		float lw = c.width(Theme.SANS, label, 12);
		float x = right - lw - 22;
		neverHit = new float[] {x, y - 2, x + lw + 22, y + 16};
		c.box(x, y, x + 14, y + 14, never ? Theme.TEXT : Theme.LINE3);
		if (never) {
			c.fill(x + 3, y + 3, x + 11, y + 11, Theme.TEXT);
		}
		c.text(Theme.SANS, label, x + 22, y, 12, Theme.SUB);
	}

	private void button(String label, float[] r, boolean primary, boolean arrow, float mx, float my) {
		boolean over = Canvas.inside(mx, my, r[0], r[1], r[2], r[3]);
		if (primary) {
			c.fill(r[0], r[1], r[2], r[3], over ? 0xFFD9D9D9 : Theme.TEXT);
		} else {
			if (over) {
				c.fill(r[0], r[1], r[2], r[3], Theme.S2);
			}
			c.box(r[0], r[1], r[2], r[3], over ? Theme.LINE3 : Theme.LINE2);
		}
		float ty = Canvas.middle((r[1] + r[3]) / 2, 13);
		int color = primary ? Theme.BLACK : Theme.TEXT;
		if (arrow) {
			float lw = c.width(Theme.SEMI, label, 13);
			float lx = (r[0] + r[2]) / 2 - (lw + 16) / 2;
			c.text(Theme.SEMI, label, lx, ty, 13, color);
			c.text(Theme.SEMI, "→", lx + lw + 6, ty, 13, color);
		} else {
			c.textCentered(Theme.SEMI, label, (r[0] + r[2]) / 2, ty, 13, color);
		}
	}

	private static Identifier skin(int face) {
		return Identifier.fromNamespaceAndPath(ChickenAfk.MOD_ID, "textures/maker/" + face + ".png");
	}

	/** Takes every click while active, so the panel underneath stays still. */
	boolean click(float mx, float my) {
		if (phase == Phase.OFF) {
			return false;
		}
		if (in(neverHit, mx, my)) {
			never = !never;
		} else if (phase == Phase.WELCOME) {
			if (in(startHit, mx, my)) {
				begin();
			} else if (in(laterHit, mx, my)) {
				finish();
			}
		} else if (in(nextHit, mx, my)) {
			next();
		} else if (in(backHit, mx, my)) {
			back();
		}
		return true;
	}

	boolean key(KeyEvent e) {
		if (phase == Phase.OFF) {
			return false;
		}
		if (e.isEscape()) {
			finish();
		} else if (phase == Phase.WELCOME && e.isConfirmation()) {
			begin();
		} else if (phase == Phase.STEPS && (e.key() == GLFW.GLFW_KEY_RIGHT || e.isConfirmation())) {
			next();
		} else if (phase == Phase.STEPS && e.key() == GLFW.GLFW_KEY_LEFT) {
			back();
		}
		return true;
	}

	private void begin() {
		phase = Phase.STEPS;
		step = 0;
		show();
	}

	private void next() {
		if (step < STEPS.size() - 1) {
			step++;
			show();
		} else {
			finish();
		}
	}

	private void back() {
		if (step > 0) {
			step--;
			show();
		}
	}

	private void show() {
		host.showDrawer("drawer".equals(STEPS.get(step).target()));
	}

	private void finish() {
		phase = Phase.OFF;
		host.showDrawer(false);
		if (never) {
			Config config = Config.get();
			config.tourHidden = true;
			config.save();
		}
	}

	private static boolean in(float[] r, float x, float y) {
		return Canvas.inside(x, y, r[0], r[1], r[2], r[3]);
	}
}
