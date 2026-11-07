package dev.chickenafk.bot;

import dev.chickenafk.Account;
import java.util.HashMap;
import java.util.Map;

/** All running bots. Ticked from the client tick, so they keep going in the main menu too. */
public final class Bots {
	/** chickennw SMP; -Dchickenafk.server=host:port points the bots elsewhere (the test run uses a local server). */
	public static final String SERVER = System.getProperty("chickenafk.server", "oyna.chickennw.com");

	private static final Map<Account, Bot> BOTS = new HashMap<>();

	private Bots() {
	}

	public static Bot of(Account account) {
		return BOTS.computeIfAbsent(account, a -> new Bot(a, x -> SERVER));
	}

	public static void start(Account account) {
		of(account).start();
	}

	public static void stop(Account account) {
		Bot bot = BOTS.get(account);
		if (bot != null) {
			bot.stop();
		}
	}

	public static void startAll() {
		for (Account a : Account.all()) {
			if (!a.running()) {
				start(a);
			}
		}
	}

	public static void stopAll() {
		for (Account a : Account.all()) {
			stop(a);
		}
	}

	public static boolean anyRunning() {
		return Account.all().stream().anyMatch(Account::running);
	}

	/** Stops the bot and forgets the account for good. */
	public static void remove(Account account) {
		stop(account);
		BOTS.remove(account);
		Account.all().remove(account);
		Account.save();
	}

	public static void tick() {
		for (Bot bot : BOTS.values()) {
			bot.tick();
		}
	}
}
