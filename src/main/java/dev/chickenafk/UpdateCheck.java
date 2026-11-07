package dev.chickenafk;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.VersionParsingException;

/**
 * Asks Modrinth once per game start whether a newer ChickenAFK exists for this Minecraft version. The panel and
 * the title-screen card show it; nothing is downloaded. Any failure (offline, not published yet) just means no notice.
 */
public final class UpdateCheck {
	public static final String PAGE = "https://modrinth.com/mod/chickenafk";
	private static final String PROJECT = "chickenafk";

	/** The newer version's number, or null while unknown or up to date. */
	private static volatile String latest;
	private static boolean started;

	private UpdateCheck() {
	}

	public static String latest() {
		return latest;
	}

	public static String current() {
		return FabricLoader.getInstance().getModContainer(ChickenAfk.MOD_ID)
				.map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("0");
	}

	public static void start() {
		if (started) {
			return;
		}
		started = true;
		// lets the UI test show the notice without a published release
		String fake = System.getProperty("chickenafk.fakeupdate");
		if (fake != null) {
			latest = fake;
			return;
		}
		Thread thread = new Thread(UpdateCheck::check, "ChickenAFK update check");
		thread.setDaemon(true);
		thread.start();
	}

	private static void check() {
		String game = FabricLoader.getInstance().getModContainer("minecraft")
				.map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("");
		String query = "loaders=" + encode("[\"fabric\"]") + "&game_versions=" + encode("[\"" + game + "\"]");
		HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.modrinth.com/v2/project/" + PROJECT + "/version?" + query))
				.header("User-Agent", "Msh4i/chickenafk/" + current())
				.timeout(Duration.ofSeconds(10))
				.build();
		try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()) {
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				return;
			}
			// Modrinth lists the newest version first
			JsonArray versions = JsonParser.parseString(response.body()).getAsJsonArray();
			if (versions.isEmpty()) {
				return;
			}
			String newest = versions.get(0).getAsJsonObject().get("version_number").getAsString();
			if (newer(newest, current())) {
				latest = newest;
				ChickenAfk.LOG.info("ChickenAFK {} is available ({} installed)", newest, current());
			}
		} catch (Exception e) {
			ChickenAfk.LOG.debug("Update check failed", e);
		}
	}

	static boolean newer(String candidate, String installed) {
		try {
			return SemanticVersion.parse(candidate).compareTo((net.fabricmc.loader.api.Version) SemanticVersion.parse(installed)) > 0;
		} catch (VersionParsingException e) {
			return !candidate.equals(installed);
		}
	}

	private static String encode(String s) {
		return URLEncoder.encode(s, StandardCharsets.UTF_8);
	}
}
