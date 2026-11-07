package dev.chickenafk.ui;

import dev.chickenafk.Account;
import dev.chickenafk.MacroStep;
import dev.chickenafk.bot.Bot;
import dev.chickenafk.bot.Bots;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** The account detail panel that slides in from the right. */
final class Drawer {
	enum Tab {
		GENEL("Genel"), ENVANTER("Envanter"), MENU("Menü"), SOHBET("Sohbet"), MAKRO("Makro");

		final String label;

		Tab(String label) {
			this.label = label;
		}
	}

	static final float X = 680;
	private static final float W = 420;
	private static final float PAD = 20;
	private static final float L = X + PAD;
	private static final float R = X + W - PAD;
	private static final long CONFIRM_MS = 3000;

	private final Canvas c;
	private final Runnable removed;
	private final java.util.function.Consumer<Account> editPassword;
	private final TextInput chatInput = new TextInput("Mesaj ya da /komut");
	private final TextInput macroInput = new TextInput("/komut");
	private Account account;
	private Tab tab = Tab.GENEL;
	private boolean open;
	private float shown;
	private long lastNanos = System.nanoTime();
	private String chatError = "";
	private String macroError = "";
	/** Sil asks again within a few seconds before deleting. */
	private long confirmDeleteUntil;
	/** Clickable areas of the last frame, in design units: {x0, y0, x1, y1}. */
	private final List<float[]> tabHits = new ArrayList<>();
	private final List<float[]> switchHits = new ArrayList<>();
	/** Inventory index (Account.inventory) or menu slot to its rectangle. */
	private final Map<Integer, float[]> invHits = new HashMap<>();
	private final Map<Integer, float[]> menuHits = new HashMap<>();
	private float[] closeHit = new float[4];
	private float[] runHit = new float[4];
	private float[] deleteHit = new float[4];
	private float[] sendHit = new float[4];
	private float[] addHit = new float[4];
	private float[] menuCloseHit = new float[4];
	private float[] passwordHit = new float[4];
	private float lastMx;
	private float lastMy;

	Drawer(Canvas c, Runnable removed, java.util.function.Consumer<Account> editPassword) {
		this.c = c;
		this.removed = removed;
		this.editPassword = editPassword;
	}

	void open(Account a) {
		if (a != account) {
			chatError = "";
			confirmDeleteUntil = 0;
		}
		account = a;
		open = true;
	}

	void showTab(String name) {
		tab = Tab.valueOf(name);
	}

	void close() {
		open = false;
		chatInput.focused = false;
		macroInput.focused = false;
	}

	boolean isOpen() {
		return open;
	}

	boolean visible() {
		return shown > 0.001f;
	}

	/** Draws in design units; mouseX/mouseY are GUI coordinates, for item tooltips. */
	void render(float mx, float my, int mouseX, int mouseY) {
		long now = System.nanoTime();
		float dt = Math.min(0.1f, (now - lastNanos) / 1e9f);
		lastNanos = now;
		shown += ((open ? 1 : 0) - shown) * (1 - (float) Math.exp(-dt * 14));
		if (!visible() || account == null) {
			return;
		}
		float off = (1 - shown) * W;
		var g = c.graphics();
		g.pose().pushMatrix();
		g.pose().translate(off, 0);
		mx -= off;
		lastMx = mx;
		lastMy = my;

		c.fill(X, 0, X + W, Canvas.H, Theme.BLACK);
		c.fill(X, 0, X + 1, Canvas.H, Theme.LINE2);
		header(mx, my);

		tabHits.clear();
		float tx = L;
		for (Tab t : Tab.values()) {
			float w = c.width(Theme.SANS, t.label, 13);
			boolean on = t == tab;
			boolean over = Canvas.inside(mx, my, tx, 66, tx + w, 92);
			c.text(Theme.SANS, t.label, tx, 70, 13, on ? Theme.TEXT : over ? Theme.SUB : Theme.MUTE);
			if (t == Tab.MENU && account.menu != null) {
				c.fill(tx + w + 3, 70, tx + w + 8, 75, Theme.OK);
			}
			if (on) {
				c.fill(tx, 91, tx + w, 92, Theme.TEXT);
			}
			tabHits.add(new float[] {tx, 66, tx + w, 92});
			tx += w + 18;
		}
		c.fill(L, 92, R, 93, Theme.LINE);

		switch (tab) {
			case GENEL -> genel();
			case ENVANTER -> envanter(mx, my, mouseX, mouseY);
			case MENU -> menu(mx, my, mouseX, mouseY);
			case SOHBET -> sohbet(mx, my);
			case MAKRO -> makro(mx, my);
		}
		g.pose().popMatrix();
	}

	private void header(float mx, float my) {
		c.face(account.skin, L, 18, 36);
		c.text(Theme.SEMI, account.name, L + 46, 19, 16, Theme.TEXT);
		Bits.status(c, account, L + 46, 39);

		closeHit = new float[] {R - 32, 20, R, 52};
		boolean overClose = in(closeHit, mx, my);
		c.box(R - 32, 20, R, 52, overClose ? Theme.LINE3 : Theme.LINE2);
		cross(R - 16, 36, overClose ? Theme.TEXT : Theme.SUB);

		boolean confirming = System.currentTimeMillis() < confirmDeleteUntil;
		String del = confirming ? "Emin misin?" : "Sil";
		float dw = c.width(Theme.SEMI, del, 12) + 22;
		deleteHit = new float[] {closeHit[0] - 8 - dw, 20, closeHit[0] - 8, 52};
		boolean overDel = in(deleteHit, mx, my);
		c.box(deleteHit[0], 20, deleteHit[2], 52, confirming || overDel ? 0xFFFF6B6B : Theme.LINE2);
		c.label(Theme.SEMI, del, (deleteHit[0] + deleteHit[2]) / 2, 36, 12, confirming ? 0xFFFF6B6B : Theme.SUB);

		boolean running = account.running();
		String run = running ? "Durdur" : "Başlat";
		float rw = c.width(Theme.SEMI, run, 12) + 26;
		runHit = new float[] {deleteHit[0] - 8 - rw, 20, deleteHit[0] - 8, 52};
		boolean overRun = in(runHit, mx, my);
		if (running) {
			c.box(runHit[0], 20, runHit[2], 52, overRun ? Theme.LINE3 : Theme.LINE2);
			c.label(Theme.SEMI, run, (runHit[0] + runHit[2]) / 2, 36, 12, Theme.TEXT);
		} else {
			c.fill(runHit[0], 20, runHit[2], 52, overRun ? 0xFFD9D9D9 : Theme.TEXT);
			c.label(Theme.SEMI, run, (runHit[0] + runHit[2]) / 2, 36, 12, Theme.BLACK);
		}
	}

	private void genel() {
		Account a = account;
		Bits.spaced(c, Theme.MONO, "SONRAKİ SHARD", L, 108, 11, Theme.MUTE, 1.3f);
		float w = c.text(Theme.MONO, Bits.countdown(a), L, 126, 44, Theme.TEXT);
		Bits.kristal(c, a, L + w + 12, 146, Theme.TEXT);
		c.fill(L, 180, R, 182, Theme.LINE2);
		c.fill(L, 180, L + (R - L) * Bits.progress(a), 182, Theme.TEXT);
		c.text(Theme.MONO, a.state == Account.State.AFK ? "AFK alanında her 3 dakikada ödül" : "AFK alanına girince sayaç başlar",
				L, 190, 11, Theme.MUTE);

		// done steps are white, the one in progress amber; "SMP" counts from the macro's smp-de flag
		String[] steps = {"Bağlantı", "Giriş", "SMP", "AFK alanı"};
		int reached = switch (a.state) {
			case AFK -> 4;
			case JOINING -> Bots.of(a).flags().contains("smp-de") ? 3 : 1;
			case CONNECTING -> 0;
			case OFF -> -1;
		};
		float cw = (R - L) / 4;
		c.box(L, 216, R, 260, Theme.LINE);
		for (int i = 0; i < 4; i++) {
			float x = L + i * cw;
			if (i > 0) {
				c.fill(x, 216, x + 1, 260, Theme.LINE);
			}
			int color = i < reached ? Theme.TEXT : i == reached ? Theme.WAIT : Theme.MUTE;
			c.text(Theme.MONO, "0" + (i + 1), x + 10, 223, 10.5f, color);
			c.text(Theme.SANS, steps[i], x + 10, 238, 12, color);
		}

		float y = 272;
		row("AFK süresi", a.afkTime(), y);
		row("Ödül (bu oturum)", String.valueOf(a.rewards), y += 32);
		row("Ping", a.ping > 0 ? a.ping + " ms" : "—", y += 32);
		y += 32;
		c.text(Theme.SANS, "Şifre", L, y + 8, 12.5f, Theme.SUB);
		float ew = c.width(Theme.SEMI, "Değiştir", 11.5f) + 18;
		passwordHit = new float[] {R - ew, y + 4, R, y + 26};
		boolean overEdit = in(passwordHit, lastMx, lastMy);
		c.box(passwordHit[0], passwordHit[1], R, passwordHit[3], overEdit ? Theme.LINE3 : Theme.LINE2);
		c.label(Theme.SEMI, "Değiştir", R - ew / 2, y + 15, 11.5f, overEdit ? Theme.TEXT : Theme.SUB);
		c.textRight(Theme.MONO, a.password.isEmpty() ? "yok" : "kayıtlı", R - ew - 10, y + 9, 12, Theme.TEXT);
		c.fill(L, y + 31, R, y + 32, Theme.LINE);
		if (a.lastReason != null) {
			// kick messages are often long and say what to do, so show all of it
			y += 32;
			c.text(Theme.SANS, "Son kopma", L, y + 8, 12.5f, Theme.SUB);
			float ly = y + 28;
			for (String line : wrapAll(Theme.MONO, a.lastReason, 12, R - L)) {
				if (ly > 540) {
					break;
				}
				c.text(Theme.MONO, line, L, ly, 12, Theme.TEXT);
				ly += 17;
			}
		}
	}

	/** Word wrap that also breaks a single word wider than the line (links, long names). */
	private List<String> wrapAll(Theme.Font font, String s, float size, float width) {
		List<String> out = new ArrayList<>();
		for (String line : c.wrap(font, s.replace('\n', ' '), size, width)) {
			while (line.length() > 1 && c.width(font, line, size) > width) {
				int cut = line.length() - 1;
				while (cut > 1 && c.width(font, line.substring(0, cut), size) > width) {
					cut--;
				}
				out.add(line.substring(0, cut));
				line = line.substring(cut);
			}
			out.add(line);
		}
		return out;
	}

	private static String shorten(String s, int max) {
		s = s.replace('\n', ' ');
		return s.length() <= max ? s : s.substring(0, max - 1) + "…";
	}

	private void row(String key, String value, float y) {
		c.text(Theme.SANS, key, L, y + 8, 12.5f, Theme.SUB);
		c.textRight(Theme.MONO, value, R, y + 9, 12, Theme.TEXT);
		c.fill(L, y + 31, R, y + 32, Theme.LINE);
	}

	private void envanter(float mx, float my, int mouseX, int mouseY) {
		invHits.clear();
		float cell = (R - L) / 9;
		int hover = -1;
		for (int i = 0; i < 4; i++) {
			if (slot(account.inventory[Account.ARMOR + i], L + i * 34, 108, 34, mx, my, false, invHits, Account.ARMOR + i)) {
				hover = Account.ARMOR + i;
			}
		}
		for (int r = 0; r < 3; r++) {
			for (int col = 0; col < 9; col++) {
				int s = Account.MAIN + r * 9 + col;
				if (slot(account.inventory[s], L + col * cell, 158 + r * cell, cell, mx, my, false, invHits, s)) {
					hover = s;
				}
			}
		}
		float hy = 158 + 3 * cell + 10;
		boolean live = Bots.of(account).inPlay();
		for (int col = 0; col < 9; col++) {
			if (slot(account.inventory[col], L + col * cell, hy, cell, mx, my, live && col == account.selectedSlot, invHits, col)) {
				hover = col;
			}
		}
		c.text(Theme.SANS, account.running() ? "Tıkla: al / bırak · Shift: taşı · Sağ tık (hotbar): kullan"
				: "Hesap kapalı. Başlatınca envanteri burada görünür.", L, hy + cell + 16, 11.5f, Theme.MUTE);
		if (hover >= 0 && !account.inventory[hover].isEmpty()) {
			tooltip(account.inventory[hover], mouseX, mouseY);
		}
	}

	private void menu(float mx, float my, int mouseX, int mouseY) {
		menuHits.clear();
		Account.Menu menu = account.menu;
		if (menu == null) {
			c.text(Theme.SANS, "Açık menü yok.", L, 112, 13, Theme.SUB);
			float y = 136;
			for (String line : c.wrap(Theme.SANS, "Bot bir sandık ya da sunucu menüsü (örneğin /afk) açınca burada görünür; "
					+ "tıklayıp kullanabilirsin.", 12, R - L)) {
				c.text(Theme.SANS, line, L, y, 12, Theme.MUTE);
				y += 18;
			}
			return;
		}
		String title = menu.title.getString();
		c.text(Theme.SEMI, title.isBlank() ? "Menü" : shorten(title, 30), L, 108, 15, Theme.TEXT);
		float cw = c.width(Theme.SEMI, "Kapat", 12) + 24;
		menuCloseHit = new float[] {R - cw, 104, R, 130};
		boolean over = in(menuCloseHit, mx, my);
		c.box(menuCloseHit[0], 104, R, 130, over ? Theme.LINE3 : Theme.LINE2);
		c.label(Theme.SEMI, "Kapat", (menuCloseHit[0] + R) / 2, 117, 12, Theme.TEXT);

		float cell = (R - L) / 9;
		int hover = -1;
		ItemStack[] slots = menu.slots;
		for (int i = 0; i < slots.length; i++) {
			float x = L + (i % 9) * cell;
			float y = 142 + (float) (i / 9) * cell;
			if (y + cell > Canvas.H - 40) {
				break;
			}
			if (slot(slots[i], x, y, cell, mx, my, false, menuHits, i)) {
				hover = i;
			}
		}
		float rows = (float) Math.ceil(slots.length / 9.0);
		c.text(Theme.SANS, "Tıkla: al / bırak · Sağ tık: yarısını al · Shift: taşı", L, 142 + rows * cell + 12, 11.5f, Theme.MUTE);
		if (hover >= 0 && slots[hover] != null && !slots[hover].isEmpty()) {
			tooltip(slots[hover], mouseX, mouseY);
		}
	}

	/** Draws one slot and records where it is; returns true when the mouse is over it. */
	private boolean slot(ItemStack stack, float x, float y, float size, float mx, float my, boolean current,
			Map<Integer, float[]> hits, int index) {
		boolean over = Canvas.inside(mx, my, x, y, x + size, y + size);
		hits.put(index, new float[] {x, y, x + size, y + size});
		if (over) {
			c.fill(x, y, x + size, y + size, Theme.S3);
		}
		c.box(x, y, x + size + 1, y + size + 1, Theme.LINE);
		if (current) {
			c.fill(x + 6, y + size - 3, x + size - 5, y + size - 1, Theme.TEXT);
		}
		if (stack != null && !stack.isEmpty()) {
			float icon = size * 0.62f;
			c.item(stack, x + (size - icon) / 2, y + (size - icon) / 2, icon);
			if (stack.getCount() > 1) {
				c.textRight(Theme.MONO, String.valueOf(stack.getCount()), x + size - 3, y + size - 13, 10.5f, Theme.TEXT);
			}
		}
		return over;
	}

	/** The item's own tooltip (name, enchantments, lore), read with the bot's registries. */
	private void tooltip(ItemStack stack, int mouseX, int mouseY) {
		List<Component> lines;
		RegistryAccess registries = Bots.of(account).registries();
		if (registries != null) {
			lines = stack.getTooltipLines(Item.TooltipContext.of(registries), null, TooltipFlag.NORMAL);
		} else {
			lines = new ArrayList<>(List.of(stack.getHoverName()));
		}
		List<Component> withId = new ArrayList<>(lines);
		withId.add(Component.literal(stack.getItemHolder().getRegisteredName()).withStyle(ChatFormatting.DARK_GRAY));
		c.graphics().setComponentTooltipForNextFrame(Minecraft.getInstance().font, withId, mouseX, mouseY);
	}

	private void sohbet(float mx, float my) {
		// newest at the bottom; long lines wrap instead of being cut
		float y = 524;
		float x = L + 44;
		for (int i = account.chat.size() - 1; i >= 0 && y > 104; i--) {
			Account.ChatLine line = account.chat.get(i);
			String text = line.note() ? "› " + line.text() : line.fromServer() ? line.text() : line.sender() + "  " + line.text();
			int color = line.note() ? Theme.MUTE : line.fromServer() ? Theme.SUB : Theme.TEXT;
			List<String> rows = wrapAll(Theme.SANS, text, 12.5f, R - x);
			float top = y - (rows.size() - 1) * 17;
			for (int r = 0; r < rows.size(); r++) {
				float ry = top + r * 17;
				if (ry <= 104) {
					continue;
				}
				String row = rows.get(r);
				if (r == 0 && !line.note() && !line.fromServer() && row.startsWith(line.sender())) {
					float sw = c.text(Theme.SEMI, line.sender(), x, ry, 12.5f, Theme.TEXT);
					c.text(Theme.SANS, row.substring(line.sender().length()).stripLeading(), x + sw + 6, ry, 12.5f, color);
				} else {
					c.text(Theme.SANS, row, x, ry, 12.5f, color);
				}
			}
			if (top > 104) {
				c.text(Theme.MONO, line.time(), L, top + 2, 11, Theme.MUTE);
			}
			y = top - 22;
		}
		if (account.chat.isEmpty()) {
			c.text(Theme.SANS, "Henüz mesaj yok.", L, 112, 13, Theme.MUTE);
		}

		c.fill(L, 552, R, 553, Theme.LINE);
		c.text(Theme.MONO, ">", L, 565, 12, Theme.MUTE);
		float bw = c.width(Theme.SEMI, "Gönder", 13) + 24;
		sendHit = new float[] {R - bw, 559, R, 587};
		chatInput.draw(c, L + 16, 559, R - bw - 10, 587, 12);
		button("Gönder", sendHit, true, mx, my);
		if (!chatError.isEmpty()) {
			c.text(Theme.SANS, chatError, L, 594, 11.5f, 0xFFFF6B6B);
		}
	}

	private void makro(float mx, float my) {
		switchHits.clear();
		Bits.spaced(c, Theme.MONO, "AFK'YE GİRİŞ · HAZIR", L, 108, 11, Theme.MUTE, 1.3f);
		float y = 124;
		for (int i = 0; i < MacroStep.BUILT_IN.size(); i++) {
			macroRow("0" + (i + 1), MacroStep.BUILT_IN.get(i), y);
			lock(R - 8, y + 11);
			y += 34;
		}
		y += 10;
		Bits.spaced(c, Theme.MONO, "SMP'YE GİRDİKTEN SONRA", L, y, 11, Theme.MUTE, 1.3f);
		y += 16;
		if (MacroStep.USER.isEmpty()) {
			c.text(Theme.SANS, "Henüz komut eklemedin. AFK alanına gitmeden önce çalışır.", L, y + 10, 11.5f, Theme.MUTE);
			c.fill(L, y + 33, R, y + 34, Theme.LINE);
			y += 34;
		}
		for (int i = 0; i < MacroStep.USER.size(); i++) {
			MacroStep s = MacroStep.USER.get(i);
			macroRow("+" + (i + 1), s, y);
			float[] hit = {R - 30, y + 9, R, y + 25};
			toggle(hit, s.enabled);
			switchHits.add(hit);
			y += 34;
		}
		y += 10;
		float bw = c.width(Theme.SEMI, "Ekle", 13) + 28;
		c.box(L, y, R - bw - 8, y + 32, macroInput.focused ? Theme.TEXT : Theme.LINE2);
		macroInput.draw(c, L + 10, y, R - bw - 14, y + 32, 12);
		addHit = new float[] {R - bw, y, R, y + 32};
		button("Ekle", addHit, false, mx, my);
		if (!macroError.isEmpty()) {
			c.text(Theme.SANS, macroError, L, y + 40, 11.5f, 0xFFFF6B6B);
		}
	}

	private void macroRow(String no, MacroStep s, float y) {
		c.text(Theme.MONO, no, L, y + 10, 11, Theme.MUTE);
		float w = c.text(Theme.MONO, s.command, L + 30, y + 9, 12, s.enabled ? Theme.TEXT : Theme.MUTE);
		c.text(Theme.SANS, s.label, L + 30 + w + 10, y + 10, 11.5f, Theme.MUTE);
		c.fill(L, y + 33, R, y + 34, Theme.LINE);
	}

	private void toggle(float[] r, boolean on) {
		c.box(r[0], r[1], r[2], r[3], on ? Theme.TEXT : Theme.LINE3);
		float k = on ? r[2] - 13 : r[0] + 3;
		c.fill(k, r[1] + 3, k + 10, r[3] - 3, on ? Theme.TEXT : Theme.MUTE);
	}

	private void button(String label, float[] r, boolean primary, float mx, float my) {
		boolean over = in(r, mx, my);
		if (primary) {
			c.fill(r[0], r[1], r[2], r[3], over ? 0xFFD9D9D9 : Theme.TEXT);
		} else {
			if (over) {
				c.fill(r[0], r[1], r[2], r[3], Theme.S2);
			}
			c.box(r[0], r[1], r[2], r[3], over ? Theme.LINE3 : Theme.LINE2);
		}
		c.label(Theme.SEMI, label, (r[0] + r[2]) / 2, (r[1] + r[3]) / 2, 13, primary ? Theme.BLACK : Theme.TEXT);
	}

	private void cross(float cx, float cy, int color) {
		for (int i = -5; i <= 5; i++) {
			c.fill(cx + i, cy + i, cx + i + 1.4f, cy + i + 1.4f, color);
			c.fill(cx + i, cy - i, cx + i + 1.4f, cy - i + 1.4f, color);
		}
	}

	private void lock(float cx, float cy) {
		c.fill(cx - 4, cy - 1, cx + 4, cy + 6, Theme.MUTE);
		c.box(cx - 3, cy - 6, cx + 3, cy, Theme.MUTE);
	}

	/** Design-unit click; button 0 left, 1 right. Returns true if the drawer used it. */
	boolean click(float mx, float my, int button, boolean shift) {
		if (!open || mx < X) {
			return false;
		}
		chatInput.focused = false;
		macroInput.focused = false;
		if (in(closeHit, mx, my)) {
			close();
			return true;
		}
		if (in(runHit, mx, my)) {
			if (account.running()) {
				Bots.stop(account);
			} else {
				Bots.start(account);
			}
			return true;
		}
		if (in(deleteHit, mx, my)) {
			if (System.currentTimeMillis() < confirmDeleteUntil) {
				Bots.remove(account);
				close();
				removed.run();
			} else {
				confirmDeleteUntil = System.currentTimeMillis() + CONFIRM_MS;
			}
			return true;
		}
		for (int i = 0; i < tabHits.size(); i++) {
			if (in(tabHits.get(i), mx, my)) {
				tab = Tab.values()[i];
				return true;
			}
		}
		if (tab == Tab.GENEL && in(passwordHit, mx, my)) {
			editPassword.accept(account);
			return true;
		}
		Bot bot = Bots.of(account);
		switch (tab) {
			case ENVANTER -> invHits.forEach((index, r) -> {
				if (in(r, mx, my)) {
					if (button == 1 && index < Account.MAIN) {
						bot.useHotbar(index);
					} else {
						int slot = bot.menuSlotFor(index);
						int id = account.menu == null ? 0 : account.menu.id;
						if (slot >= 0) {
							bot.click(id, slot, 0, shift ? ClickType.QUICK_MOVE : ClickType.PICKUP);
						}
					}
				}
			});
			case MENU -> {
				if (in(menuCloseHit, mx, my)) {
					bot.closeMenu();
				} else if (account.menu != null) {
					int id = account.menu.id;
					menuHits.forEach((slot, r) -> {
						if (in(r, mx, my)) {
							bot.click(id, slot, button == 1 ? 1 : 0, shift ? ClickType.QUICK_MOVE : ClickType.PICKUP);
						}
					});
				}
			}
			case SOHBET -> {
				if (in(sendHit, mx, my)) {
					send();
				} else if (chatInput.contains(mx, my)) {
					chatInput.focused = true;
				}
			}
			case MAKRO -> {
				for (int i = 0; i < switchHits.size(); i++) {
					if (in(switchHits.get(i), mx, my)) {
						MacroStep s = MacroStep.USER.get(i);
						s.enabled = !s.enabled;
					}
				}
				if (in(addHit, mx, my)) {
					addStep();
				} else if (macroInput.contains(mx, my)) {
					macroInput.focused = true;
				}
			}
			default -> {
			}
		}
		return true;
	}

	boolean key(KeyEvent e) {
		if (tab == Tab.SOHBET && chatInput.key(e, this::send)) {
			chatError = "";
			return true;
		}
		if (tab == Tab.MAKRO && macroInput.key(e, this::addStep)) {
			macroError = "";
			return true;
		}
		return false;
	}

	boolean typed(CharacterEvent e) {
		if (tab == Tab.SOHBET && chatInput.typed(e)) {
			chatError = "";
			return true;
		}
		if (tab == Tab.MAKRO && macroInput.typed(e)) {
			macroError = "";
			return true;
		}
		return false;
	}

	/** Sends what is typed through the bot; the server's echo shows it in the log. */
	private void send() {
		String v = chatInput.value().trim();
		if (v.isEmpty()) {
			chatError = "Önce bir şey yaz.";
			return;
		}
		Bot bot = Bots.of(account);
		if (!bot.inPlay()) {
			chatError = "Hesap sunucuda değil.";
			return;
		}
		bot.say(v);
		if (v.startsWith("/")) {
			account.note("Komut gönderildi: " + v);
		}
		chatInput.clear();
	}

	private void addStep() {
		String v = macroInput.value().trim();
		if (!v.startsWith("/") || v.length() < 2) {
			macroError = "Komut / ile başlamalı.";
			return;
		}
		MacroStep.USER.add(MacroStep.user(v));
		macroInput.clear();
	}

	/** Centre of a control, for the test run: "run", "delete", "send", "chat", "menu:N" (slot N). */
	float[] center(String what) {
		float[] r = switch (what) {
			case "run" -> runHit;
			case "delete" -> deleteHit;
			case "send" -> sendHit;
			case "password" -> passwordHit;
			case "chat" -> new float[] {chatInput.x0, chatInput.y0, chatInput.x1, chatInput.y1};
			default -> what.startsWith("menu:") ? menuHits.get(Integer.parseInt(what.substring(5))) : null;
		};
		return r == null ? null : new float[] {(r[0] + r[2]) / 2, (r[1] + r[3]) / 2};
	}

	private static boolean in(float[] r, float x, float y) {
		return Canvas.inside(x, y, r[0], r[1], r[2], r[3]);
	}

}
