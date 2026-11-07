package dev.chickenafk.bot;

import java.util.Locale;

/** Text matching helpers. Server text is often small caps and Turkish; macros match it in plain form. */
public final class Text {
	private static final String SMALL_CAPS = "ᴀʙᴄᴅᴇꜰғɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱsᴛᴜᴠᴡxʏᴢ";
	private static final String SMALL_LATIN = "abcdeffghijklmnopqrsstuvwxyz";
	private static final String TURKISH = "çğıöşüâîû";
	private static final String TURKISH_PLAIN = "cgiosuaiu";

	private Text() {
	}

	/** Small caps to letters, lower case; keeps Turkish letters (for regexes written with them). */
	public static String lower(String s) {
		StringBuilder b = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			int k = SMALL_CAPS.indexOf(c);
			b.append(k >= 0 ? SMALL_LATIN.charAt(k) : c);
		}
		return b.toString().replace('İ', 'i').toLowerCase(Locale.ROOT);
	}

	/** {@link #lower} plus Turkish letters folded to ASCII, for "contains" checks. Same length as the input. */
	public static String plain(String s) {
		String lower = lower(s);
		StringBuilder b = new StringBuilder(lower.length());
		for (int i = 0; i < lower.length(); i++) {
			char c = lower.charAt(i);
			int k = TURKISH.indexOf(c);
			b.append(k >= 0 ? TURKISH_PLAIN.charAt(k) : c);
		}
		return b.toString();
	}

	public static boolean contains(String haystack, String needle) {
		return plain(haystack).contains(plain(needle));
	}

	/** "1500ms", "12s", "3d" (minutes), plain numbers as ms. */
	public static long millis(String s) {
		if (s == null || s.isBlank()) {
			return 0;
		}
		s = s.trim().toLowerCase(Locale.ROOT);
		try {
			if (s.endsWith("ms")) {
				return Long.parseLong(s.substring(0, s.length() - 2).trim());
			}
			if (s.endsWith("s")) {
				return Math.round(Double.parseDouble(s.substring(0, s.length() - 1).trim()) * 1000);
			}
			if (s.endsWith("dk") || s.endsWith("d") || s.endsWith("m")) {
				return Math.round(Double.parseDouble(s.replaceAll("[a-z]+$", "").trim()) * 60_000);
			}
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
