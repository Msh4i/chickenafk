package dev.chickenafk.ui;

import dev.chickenafk.Account;
import dev.chickenafk.ChickenAfk;
import dev.chickenafk.Config;
import dev.chickenafk.UpdateCheck;
import dev.chickenafk.bot.Bots;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.util.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/** The ChickenAFK panel: accounts standing as characters, the selected one up top, the maker on the right. */
public final class PanelScreen extends Screen {
	private static final Random RANDOM = new Random();
	/** Last maker pose, so reopening the panel always shows a different one. */
	private static Pose lastMakerPose;

	private static final float SLOT_W = 128;
	private static final float SLOT_GAP = 6;
	private static final float CHAR_W = 86;
	private static final float CHAR_H = 173;
	private static final float CREATOR_X = 860;

	/** The maker's links under the figure; Discord has no profile URL for a username, so that one copies it. */
	private static final class Social {
		final Identifier icon;
		final String handle;
		final String url;
		float[] hit = new float[4];

		Social(String icon, String handle, String url) {
			this.icon = Identifier.fromNamespaceAndPath(ChickenAfk.MOD_ID, "textures/social/" + icon + ".png");
			this.handle = handle;
			this.url = url;
		}
	}

	private static final List<Social> SOCIALS = List.of(
			new Social("tiktok", "@mshai_1", "https://www.tiktok.com/@mshai_1"),
			new Social("github", "github.com/Msh4i", "https://github.com/Msh4i"),
			new Social("discord", "freakyms", null));
	private long copiedUntil;
	private float lastMx = -1;
	private float lastMy = -1;

	private final @Nullable Screen parent;
	private final Canvas c = new Canvas();
	private final AddDialog addDialog = new AddDialog(c, this::accountAdded);
	private final Drawer drawer = new Drawer(c, this::accountRemoved, addDialog::editPassword);
	private final Tour tour = new Tour(c, new Tour.Host() {
		@Override
		public float[] target(String id) {
			return switch (id) {
				case "row" -> rowRect;
				case "hero" -> heroRect;
				case "start" -> startRect;
				case "drawer" -> new float[] {Drawer.X, 0, Canvas.W, Canvas.H};
				case "creator" -> new float[] {CREATOR_X + 8, 184, Canvas.W - 8, 546};
				default -> null;
			};
		}

		@Override
		public void showDrawer(boolean open) {
			if (open && !accounts.isEmpty()) {
				openDrawer(selected, "GENEL");
			} else {
				drawer.close();
			}
		}
	});
	/** Where parts were drawn last frame, for the tour spotlight. */
	private float[] rowRect = new float[4];
	private float[] heroRect = new float[4];
	private float[] startRect = new float[4];
	private float[] addRect = new float[4];
	private float[] updateRect = new float[4];
	private final List<Account> accounts = Account.all();
	private final List<Figure> figures = new ArrayList<>();
	private final Figure maker;
	private int selected;

	public PanelScreen(@Nullable Screen parent) {
		super(Component.translatable("chickenafk.panel.title"));
		this.parent = parent;
		for (Account a : accounts) {
			figures.add(new Figure(a.slim, Pose.IDLE));
		}
		// the maker's pose is picked once per opening and held while the panel is open
		lastMakerPose = Pose.randomMaker(RANDOM, lastMakerPose);
		maker = new Figure(true, lastMakerPose);
		if (!Config.get().tourHidden) {
			tour.start();
		}
	}

	/** For the screenshot run. */
	public boolean tourActive() {
		return tour.active();
	}

	public int tourStep() {
		return tour.step();
	}

	public static int tourSteps() {
		return Tour.stepCount();
	}

	public void devKey(int key) {
		keyPressed(new KeyEvent(key, 0, 0));
	}

	public void devClickNeverBox() {
		float[] p = tour.neverBoxCenter();
		devClick(p[0], p[1]);
	}

	/** Used by the screenshot run to show every maker pose. */
	public void setMakerPose(Pose pose) {
		maker.setPose(pose);
	}

	/** Used by the screenshot run to show the drawer. */
	public void openDrawer(int account, String tab) {
		selected = account;
		drawer.open(accounts.get(account));
		drawer.showTab(tab);
	}

	/** Used by the screenshot run: a left click at a design-space point, through the normal handler. */
	public void devClick(float x, float y) {
		devClick(x, y, 0, false);
	}

	public void devClick(float x, float y, int button, boolean shift) {
		mouseClicked(new MouseButtonEvent(c.guiX(x), c.guiY(y),
				new MouseButtonInfo(button, shift ? org.lwjgl.glfw.GLFW.GLFW_MOD_SHIFT : 0)), false);
	}

	/** Clicks a named control (see {@link Drawer#center}) or "add" / "start"; false if it is not on screen. */
	public boolean devClick(String control, boolean shift) {
		float[] p = switch (control) {
			case "add" -> new float[] {(addRect[0] + addRect[2]) / 2, (addRect[1] + addRect[3]) / 2};
			case "start" -> new float[] {(startRect[0] + startRect[2]) / 2, (startRect[1] + startRect[3]) / 2};
			default -> drawer.center(control);
		};
		if (p == null) {
			return false;
		}
		devClick(p[0], p[1], 0, shift);
		return true;
	}

	public void devType(String text) {
		text.codePoints().forEach(cp -> charTyped(new CharacterEvent(cp, 0)));
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		g.fill(0, 0, width, height, 0xB4000000);
		c.begin(g, width, height);
		float mx = c.designX(mouseX);
		float my = c.designY(mouseY);
		lastMx = mx;
		lastMy = my;
		// while the drawer covers the right side, the stage underneath does not react to the mouse
		boolean stageMouse = !drawer.isOpen() || mx < Drawer.X;
		topBar();
		hero();
		row(stageMouse ? mx : -1, stageMouse ? my : -1);
		creator();
		c.textRight(Theme.MONO, "Resmî değildir · chickennw ile bağlantısı yoktur", 1078, 598, 11, Theme.MUTE);
		// the 3D figures are drawn in a later layer; step past them so the drawer covers them
		g.nextStratum();
		drawer.render(mx, my, mouseX, mouseY);
		if (tour.active()) {
			g.nextStratum();
			tour.render(mx, my);
		}
		if (addDialog.isOpen()) {
			g.nextStratum();
			addDialog.render(mx, my);
		}
		c.end();
		super.render(g, mouseX, mouseY, partialTick);
	}

	private void topBar() {
		float x = Bits.spaced(c, Theme.SEMI, "CHICKENAFK", 22, 26, 13, Theme.TEXT, 2.6f);
		x += 14 + c.text(Theme.MONO, "chickennw / smp", x + 14, 27, 12, Theme.SUB);
		String update = UpdateCheck.latest();
		if (update != null) {
			String label = "Yeni sürüm v" + update + " · indir";
			float w = c.width(Theme.SEMI, label, 12) + 30;
			updateRect = new float[] {x + 16, 18, x + 16 + w, 44};
			boolean over = Canvas.inside(lastMx, lastMy, updateRect[0], updateRect[1], updateRect[2], updateRect[3]);
			c.fill(updateRect[0], 18, updateRect[2], 44, over ? 0xFFD9D9D9 : Theme.TEXT);
			c.fill(updateRect[0] + 11, 28, updateRect[0] + 17, 34, Theme.OK);
			c.text(Theme.SEMI, label, updateRect[0] + 23, Canvas.middle(31, 12), 12, Theme.BLACK);
		} else {
			updateRect = new float[4];
		}

		c.box(1046, 16, 1078, 48, Theme.LINE2);
		for (int i = 0; i < 3; i++) {
			c.fill(1055, 26 + i * 5, 1069, 27 + i * 5, Theme.SUB);
		}
		boolean running = Bots.anyRunning();
		String label = running ? "Hepsini durdur" : "Hepsini başlat";
		float bw = c.width(Theme.SEMI, label, 13) + 40;
		float bx = 1040 - bw;
		startRect = new float[] {bx, 16, 1040, 48};
		if (running) {
			c.box(bx, 16, 1040, 48, Theme.LINE3);
			c.fill(bx + 14, 27, bx + 23, 36, Theme.TEXT);
			c.text(Theme.SEMI, label, bx + 27, Canvas.middle(32, 13), 13, Theme.TEXT);
		} else {
			c.fill(bx, 16, 1040, 48, Theme.TEXT);
			playIcon(bx + 14, 27, Theme.BLACK);
			c.text(Theme.SEMI, label, bx + 27, Canvas.middle(32, 13), 13, Theme.BLACK);
		}

		long afk = accounts.stream().filter(a -> a.state == Account.State.AFK).count();
		float sx = c.width(Theme.MONO, " afk", 12);
		c.textRight(Theme.MONO, " afk", bx - 14, 27, 12, Theme.SUB);
		c.textRight(Theme.MONO, afk + "/" + accounts.size(), bx - 14 - sx, 27, 12, Theme.TEXT);
	}

	private void hero() {
		if (accounts.isEmpty()) {
			c.text(Theme.DISPLAY, "Hoş geldin", 22, 72, 52, Theme.TEXT);
			c.text(Theme.SANS, "Henüz hesap eklemedin. Aşağıdaki kutudan ilk hesabını ekle.", 24, 136, 13, Theme.SUB);
			heroRect = new float[] {16, 66, 560, 156};
			return;
		}
		Account a = accounts.get(selected);
		float nameW = c.text(Theme.DISPLAY, a.name, 22, 72, 52, Theme.TEXT);
		float sx = Bits.status(c, a, 24, 136);
		if (a.state == Account.State.AFK) {
			c.text(Theme.MONO, "AFK " + a.afkTime(), sx + 14, 136, 12, Theme.SUB);
		}
		float dx = Math.max(22 + nameW, sx + 110) + 34;
		c.fill(dx, 82, dx + 1, 150, Theme.LINE2);
		float cx = dx + 22;
		Bits.spaced(c, Theme.MONO, "SONRAKİ SHARD", cx, 86, 11, Theme.MUTE, 1.3f);
		float tw = c.text(Theme.MONO, Bits.countdown(a), cx, 104, 30, Theme.TEXT);
		Bits.kristal(c, a, cx + tw + 10, 112, Theme.TEXT);
		c.fill(cx, 146, cx + 150, 148, Theme.LINE2);
		c.fill(cx, 146, cx + 150 * Bits.progress(a), 148, Theme.TEXT);
		heroRect = new float[] {16, 66, cx + 158, 156};
	}

	private float rowStart() {
		if (accounts.isEmpty()) {
			return (CREATOR_X - 120) / 2;
		}
		return (CREATOR_X - (accounts.size() * SLOT_W + (accounts.size() - 1) * SLOT_GAP + 130)) / 2;
	}

	private void accountAdded(Account a) {
		figures.add(new Figure(a.slim, Pose.IDLE));
		selected = accounts.size() - 1;
	}

	private void accountRemoved() {
		figures.clear();
		for (Account a : accounts) {
			figures.add(new Figure(a.slim, Pose.IDLE));
		}
		selected = Math.max(0, Math.min(selected, accounts.size() - 1));
	}

	private void row(float mx, float my) {
		float x = rowStart();
		rowRect = new float[] {x, 292, x + accounts.size() * (SLOT_W + SLOT_GAP) + 4 + 120, 566};
		for (int i = 0; i < accounts.size(); i++) {
			Account a = accounts.get(i);
			boolean sel = i == selected;
			boolean hover = Canvas.inside(mx, my, x, 296, x + SLOT_W, 566);
			float cx = x + SLOT_W / 2;
			float lift = hover && !sel ? -3 : 0;

			float tw = c.width(Theme.SANS, a.name, 12);
			c.fill(cx - tw / 2 - 7, 298 + lift, cx + tw / 2 + 7, 316 + lift, 0x99000000);
			c.textCentered(Theme.SANS, a.name, cx, 301 + lift, 12, Theme.TEXT);

			float turn = sel ? -18 + (float) Math.sin(System.currentTimeMillis() / 950.0) * 10 : (i < selected ? 18 : -30);
			figures.get(i).render(c.graphics(), a.skin,
					c.guiX(cx - CHAR_W / 2), c.guiY(330 + lift), c.guiX(cx + CHAR_W / 2), c.guiY(330 + CHAR_H + lift), turn);

			float iy = 516 + lift;
			Bits.status(c, a, cx - Bits.statusWidth(c, a) / 2, iy);
			Bits.kristal(c, a, cx - Bits.kristalWidth(c, a) / 2, iy + 18, a.state == Account.State.OFF ? Theme.MUTE : Theme.TEXT);
			c.fill(cx - 45, iy + 40, cx + 45, iy + 42, Theme.LINE2);
			c.fill(cx - 45, iy + 40, cx - 45 + 90 * Bits.progress(a), iy + 42, Theme.TEXT);
			x += SLOT_W + SLOT_GAP;
		}
		// dim everything that is not selected; the 3D figures are drawn in a later layer, so step past them
		c.graphics().nextStratum();
		x = rowStart();
		for (int i = 0; i < accounts.size(); i++) {
			boolean hover = Canvas.inside(mx, my, x, 296, x + SLOT_W, 566);
			if (i != selected) {
				int shade = accounts.get(i).state == Account.State.OFF ? 0xB4000000 : hover ? 0x40000000 : 0x8C000000;
				c.fill(x, 290, x + SLOT_W, 570, shade);
			}
			x += SLOT_W + SLOT_GAP;
		}
		float ex = accounts.isEmpty() ? x : x + 10 - SLOT_GAP;
		addRect = new float[] {ex, 318, ex + 120, 560};
		boolean overAdd = Canvas.inside(mx, my, ex, 318, ex + 120, 560);
		c.dashedBox(ex, 318, ex + 120, 560, overAdd ? Theme.TEXT : Theme.LINE3);
		c.textCentered(Theme.SANS, "+", ex + 60, 418, 22, overAdd ? Theme.TEXT : Theme.MUTE);
		c.textCentered(Theme.SANS, "Hesap ekle", ex + 60, 446, 12, overAdd ? Theme.TEXT : Theme.MUTE);
	}

	private void creator() {
		c.fill(CREATOR_X, 64, CREATOR_X + 1, 580, Theme.LINE);
		float cx = CREATOR_X + 120;
		float lw = Bits.spacedWidth(c, Theme.MONO, "YAPIMCI", 11, 2.2f);
		Bits.spaced(c, Theme.MONO, "YAPIMCI", cx - lw / 2, 196, 11, Theme.MUTE, 2.2f);
		Identifier skin = Identifier.fromNamespaceAndPath(ChickenAfk.MOD_ID, "textures/maker/" + maker.pose().face + ".png");
		maker.render(c.graphics(), skin, c.guiX(cx - 48), c.guiY(216), c.guiX(cx + 48), c.guiY(408), -28);
		c.textCentered(Theme.SEMI, "MsHai", cx, 430, 22, Theme.TEXT);
		c.textCentered(Theme.SANS, "Bu modu MsHai yaptı. Hata ya da", cx, 462, 12.5f, Theme.SUB);
		c.textCentered(Theme.SANS, "öneri için yaz.", cx, 478, 12.5f, Theme.SUB);
		float ix = cx - (SOCIALS.size() * 34 + (SOCIALS.size() - 1) * 8) / 2f;
		String hint = null;
		for (Social social : SOCIALS) {
			boolean over = Canvas.inside(lastMx, lastMy, ix, 500, ix + 34, 534);
			social.hit = new float[] {ix, 500, ix + 34, 534};
			c.box(ix, 500, ix + 34, 534, over ? Theme.TEXT : Theme.LINE2);
			c.icon(social.icon, ix + 8, 508, 18, over ? Theme.TEXT : Theme.SUB);
			if (over) {
				hint = social.handle;
			}
			ix += 42;
		}
		if (System.currentTimeMillis() < copiedUntil) {
			hint = "Discord adı kopyalandı";
		}
		if (hint != null) {
			c.textCentered(Theme.MONO, hint, cx, 546, 11.5f, Theme.SUB);
		}
	}

	private void playIcon(float x, float y, int color) {
		for (int i = 0; i < 5; i++) {
			c.fill(x + i * 1.6f, y + i, x + i * 1.6f + 1.6f, y + 10 - i, color);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		float mx = c.designX(event.x());
		float my = c.designY(event.y());
		if (addDialog.click(mx, my) || tour.click(mx, my)) {
			return true;
		}
		if (drawer.click(mx, my, event.button(), event.hasShiftDown())) {
			return true;
		}
		if (Canvas.inside(mx, my, updateRect[0], updateRect[1], updateRect[2], updateRect[3])) {
			Util.getPlatform().openUri(UpdateCheck.PAGE);
			return true;
		}
		for (Social social : SOCIALS) {
			if (Canvas.inside(mx, my, social.hit[0], social.hit[1], social.hit[2], social.hit[3])) {
				if (social.url != null) {
					Util.getPlatform().openUri(social.url);
				} else {
					minecraft.keyboardHandler.setClipboard(social.handle);
					copiedUntil = System.currentTimeMillis() + 2500;
				}
				return true;
			}
		}
		if (Canvas.inside(mx, my, addRect[0], addRect[1], addRect[2], addRect[3])) {
			addDialog.open();
			return true;
		}
		if (Canvas.inside(mx, my, startRect[0], startRect[1], startRect[2], startRect[3])) {
			if (accounts.isEmpty()) {
				addDialog.open();
			} else if (Bots.anyRunning()) {
				Bots.stopAll();
			} else {
				Bots.startAll();
			}
			return true;
		}
		float x = rowStart();
		for (int i = 0; i < accounts.size(); i++) {
			if (Canvas.inside(mx, my, x, 296, x + SLOT_W, 566)) {
				selected = i;
				drawer.open(accounts.get(i));
				return true;
			}
			x += SLOT_W + SLOT_GAP;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (addDialog.key(event) || tour.key(event)) {
			return true;
		}
		if (drawer.isOpen() && drawer.key(event)) {
			return true;
		}
		if (event.isEscape() && drawer.isOpen()) {
			drawer.close();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		return addDialog.typed(event) || drawer.isOpen() && drawer.typed(event) || super.charTyped(event);
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}
}
