package dev.chickenafk.ui;

import dev.chickenafk.Account;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

/** "Hesap ekle": account name plus the password the server asks for with /gir. Also changes a saved password. */
final class AddDialog {
	private static final float W = 460;
	private static final float X0 = (Canvas.W - W) / 2;
	private static final float X1 = X0 + W;
	private static final float PAD = 24;

	private final Canvas c;
	private final Consumer<Account> added;
	private final TextInput name = new TextInput("MsHai_1");
	private final TextInput password = new TextInput("boş bırakılabilir");
	private boolean open;
	/** Set when changing an existing account's password instead of adding one. */
	private Account editing;
	private String error = "";
	private float[] addHit = new float[4];
	private float[] cancelHit = new float[4];

	AddDialog(Canvas c, Consumer<Account> added) {
		this.c = c;
		this.added = added;
		password.secret = true;
	}

	void open() {
		open = true;
		editing = null;
		error = "";
		name.clear();
		password.clear();
		name.focused = true;
		password.focused = false;
	}

	void editPassword(Account a) {
		open();
		editing = a;
		name.focused = false;
		password.focused = true;
	}

	boolean isOpen() {
		return open;
	}

	void render(float mx, float my) {
		if (!open) {
			return;
		}
		c.fill(-2000, -2000, 3100, 2619, 0xC8000000);
		List<String> note = c.wrap(Theme.SANS, "Sunucu giriş yaparken /gir ile şifre istiyorsa buraya yaz; "
				+ "bot şifre istendiğinde kendisi girer. Şifre sadece bu bilgisayarda, chickenafk/hesaplar.json "
				+ "dosyasında durur ve başka hiçbir yere gönderilmez. Kayıtsız hesapsa boş bırak.", 12, W - 2 * PAD);
		float h = 70 + 2 * 66 + note.size() * 18 + 24 + 34 + 24;
		float y0 = (Canvas.H - h) / 2;
		c.fill(X0, y0, X1, y0 + h, Theme.BLACK);
		c.box(X0, y0, X1, y0 + h, Theme.LINE3);
		float x = X0 + PAD;
		c.text(Theme.SEMI, editing == null ? "Hesap ekle" : "Şifreyi değiştir", x, y0 + 24, 20, Theme.TEXT);

		float y = y0 + 70;
		if (editing == null) {
			field("Kullanıcı adı", name, y);
		} else {
			c.text(Theme.MONO, "HESAP", x, y, 11, Theme.MUTE);
			c.box(x, y + 18, X1 - PAD, y + 52, Theme.LINE);
			c.text(Theme.MONO, editing.name, x + 12, Canvas.middle(y + 35, 13), 13, Theme.SUB);
		}
		y += 66;
		field("Şifre (/gir)", password, y);
		y += 66;
		for (String line : note) {
			c.text(Theme.SANS, line, x, y, 12, Theme.MUTE);
			y += 18;
		}
		y += 8;
		if (!error.isEmpty()) {
			c.text(Theme.SANS, error, x, y - 4, 12, 0xFFFF6B6B);
		}
		y += 16;
		String action = editing == null ? "Ekle" : "Kaydet";
		float aw = c.width(Theme.SEMI, action, 13) + 40;
		addHit = new float[] {X1 - PAD - aw, y, X1 - PAD, y + 34};
		float cw = c.width(Theme.SEMI, "İptal", 13) + 32;
		cancelHit = new float[] {addHit[0] - 10 - cw, y, addHit[0] - 10, y + 34};
		button("İptal", cancelHit, false, mx, my);
		button(action, addHit, true, mx, my);
	}

	private void field(String label, TextInput input, float y) {
		float x = X0 + PAD;
		c.text(Theme.MONO, label.toUpperCase(java.util.Locale.forLanguageTag("tr")), x, y, 11, Theme.MUTE);
		c.box(x, y + 18, X1 - PAD, y + 52, input.focused ? Theme.TEXT : Theme.LINE2);
		input.draw(c, x + 12, y + 18, X1 - PAD - 12, y + 52, 13);
	}

	private void button(String label, float[] r, boolean primary, float mx, float my) {
		boolean over = Canvas.inside(mx, my, r[0], r[1], r[2], r[3]);
		if (primary) {
			c.fill(r[0], r[1], r[2], r[3], over ? 0xFFD9D9D9 : Theme.TEXT);
		} else {
			c.box(r[0], r[1], r[2], r[3], over ? Theme.LINE3 : Theme.LINE2);
		}
		c.label(Theme.SEMI, label, (r[0] + r[2]) / 2, (r[1] + r[3]) / 2, 13, primary ? Theme.BLACK : Theme.TEXT);
	}

	/** Takes every click while open. */
	boolean click(float mx, float my) {
		if (!open) {
			return false;
		}
		if (Canvas.inside(mx, my, addHit[0], addHit[1], addHit[2], addHit[3])) {
			submit();
		} else if (Canvas.inside(mx, my, cancelHit[0], cancelHit[1], cancelHit[2], cancelHit[3])) {
			open = false;
		} else if (editing == null && name.contains(mx, my)) {
			name.focused = true;
			password.focused = false;
		} else if (password.contains(mx, my)) {
			password.focused = true;
			name.focused = false;
		}
		return true;
	}

	boolean key(KeyEvent e) {
		if (!open) {
			return false;
		}
		if (e.isEscape()) {
			open = false;
		} else if (e.key() == GLFW.GLFW_KEY_TAB && editing == null) {
			boolean toPassword = name.focused;
			name.focused = !toPassword;
			password.focused = toPassword;
		} else if (e.isConfirmation()) {
			submit();
		} else {
			name.key(e, this::submit);
			password.key(e, this::submit);
			error = "";
		}
		return true;
	}

	boolean typed(CharacterEvent e) {
		if (!open) {
			return false;
		}
		// names never contain spaces; passwords might
		if (name.focused && e.codepoint() != ' ') {
			name.typed(e);
		} else if (password.focused) {
			password.typed(e);
		}
		error = "";
		return true;
	}

	private void submit() {
		if (editing != null) {
			// an empty field removes the saved password
			editing.password = password.value();
			Account.save();
			editing.note(editing.password.isEmpty() ? "Şifre silindi." : "Şifre değiştirildi.");
			open = false;
			return;
		}
		String n = name.value().trim();
		if (!Account.validName(n)) {
			error = "Ad 3-16 karakter olmalı: harf, rakam ya da _.";
			return;
		}
		if (Account.find(n) != null) {
			error = "Bu hesap zaten ekli.";
			return;
		}
		Account a = new Account(n, password.value());
		Account.all().add(a);
		Account.save();
		open = false;
		added.accept(a);
	}
}
