package dev.chickenafk.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Single-line input drawn in the panel's own font. Vanilla's EditBox measures with the default font, so
 * its caret would drift under Geist; this one only edits at the end, which is all chat and macro need.
 */
final class TextInput {
	private static final int MAX = 256;

	private final String placeholder;
	private String value = "";
	boolean focused;
	/** Draw dots instead of the text (passwords). */
	boolean secret;
	float x0;
	float y0;
	float x1;
	float y1;

	TextInput(String placeholder) {
		this.placeholder = placeholder;
	}

	String value() {
		return value;
	}

	void clear() {
		value = "";
	}

	boolean contains(float x, float y) {
		return Canvas.inside(x, y, x0, y0, x1, y1);
	}

	void draw(Canvas c, float x0, float y0, float x1, float y1, float size) {
		this.x0 = x0;
		this.y0 = y0;
		this.x1 = x1;
		this.y1 = y1;
		float ty = Canvas.middle((y0 + y1) / 2, size);
		if (value.isEmpty() && !focused) {
			c.text(Theme.MONO, placeholder, x0, ty, size, Theme.MUTE);
			return;
		}
		// keep the end of a long value visible
		String shown = secret ? "•".repeat(value.length()) : value;
		while (shown.length() > 0 && c.width(Theme.MONO, shown, size) > x1 - x0 - 6) {
			shown = shown.substring(1);
		}
		float w = c.text(Theme.MONO, shown, x0, ty, size, Theme.TEXT);
		if (focused && (System.currentTimeMillis() / 530) % 2 == 0) {
			c.fill(x0 + w + 1, (y0 + y1) / 2 - size * 0.55f, x0 + w + 2, (y0 + y1) / 2 + size * 0.55f, Theme.TEXT);
		}
	}

	/** Returns true if the key was used. Enter runs onSubmit. */
	boolean key(KeyEvent e, Runnable onSubmit) {
		if (!focused) {
			return false;
		}
		if (e.key() == GLFW.GLFW_KEY_BACKSPACE) {
			if (!value.isEmpty()) {
				value = value.substring(0, value.offsetByCodePoints(value.length(), -1));
			}
		} else if (e.isConfirmation()) {
			onSubmit.run();
		} else if (e.isPaste()) {
			String clip = Minecraft.getInstance().keyboardHandler.getClipboard().lines().findFirst().orElse("");
			value = (value + clip).substring(0, Math.min(MAX, value.length() + clip.length()));
		} else if (e.isEscape()) {
			focused = false;
		}
		return true;
	}

	boolean typed(CharacterEvent e) {
		if (!focused || !e.isAllowedChatCharacter()) {
			return false;
		}
		if (value.length() < MAX) {
			value += e.codepointAsString();
		}
		return true;
	}
}
