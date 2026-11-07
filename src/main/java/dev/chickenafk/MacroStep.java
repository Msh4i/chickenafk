package dev.chickenafk;

import java.util.ArrayList;
import java.util.List;

/**
 * A row in the panel's macro tab. Built-in steps mirror chickenafk.json (the chickennw SMP AFK flow) and
 * cannot be edited; user steps are commands run after joining SMP, before going to the AFK area.
 */
public final class MacroStep {
	public final String command;
	public final String label;
	public final boolean builtIn;
	public boolean enabled = true;

	private MacroStep(String command, String label, boolean builtIn) {
		this.command = command;
		this.label = label;
		this.builtIn = builtIn;
	}

	public static final List<MacroStep> BUILT_IN = List.of(
			new MacroStep("/gir şifre", "Giriş · şifre istenince", true),
			new MacroStep("/joinq smp-spawn", "SMP'ye katıl · girişten sonra", true),
			new MacroStep("/afk", "AFK menüsü · SMP'deyken", true),
			new MacroStep("lime_concrete", "Yeşil alana tıkla · menü açılınca", true),
			new MacroStep("afk bölgesine", "Onay · mesaj gelince", true));

	/** Commands the player added; empty until they add one. */
	public static final List<MacroStep> USER = new ArrayList<>();

	public static MacroStep user(String command) {
		return new MacroStep(command, "3 sn bekle", false);
	}
}
