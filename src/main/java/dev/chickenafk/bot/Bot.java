package dev.chickenafk.bot;

import dev.chickenafk.Account;
import dev.chickenafk.ChickenAfk;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.BitSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientRegistryLayer;
import net.minecraft.client.multiplayer.KnownPacksManager;
import net.minecraft.client.multiplayer.RegistryDataCollector;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.HashedStack;
import net.minecraft.network.PacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ClientboundTransferPacket;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.configuration.ClientConfigurationPacketListener;
import net.minecraft.network.protocol.configuration.ClientboundCodeOfConductPacket;
import net.minecraft.network.protocol.configuration.ClientboundFinishConfigurationPacket;
import net.minecraft.network.protocol.configuration.ClientboundRegistryDataPacket;
import net.minecraft.network.protocol.configuration.ClientboundSelectKnownPacks;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.configuration.ServerboundAcceptCodeOfConductPacket;
import net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket;
import net.minecraft.network.protocol.configuration.ServerboundSelectKnownPacks;
import net.minecraft.network.protocol.cookie.ClientboundCookieRequestPacket;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundChunkBatchFinishedPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundStartConfigurationPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundConfigurationAcknowledgedPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.login.ClientLoginPacketListener;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.network.protocol.login.ClientboundLoginCompressionPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.network.protocol.login.ClientboundLoginFinishedPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.network.protocol.login.ServerboundLoginAcknowledgedPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * One account's own connection to the server, built from the game's network code. It logs in offline,
 * goes through configuration and play like a real client, but keeps no world: chunks, entities and the
 * like are dropped unread, so a bot costs little more than its socket. Only what the panel and the
 * macros need is kept (chat, titles, sidebar kristal, inventory, open menu).
 */
public final class Bot {
	/** Reconnect waits. Proxies keep a dropped session ~30 s, so an early kick waits longer. */
	private static final long RECONNECT_MS = 10_000;
	private static final long COLLISION_RECONNECT_MS = 35_000;
	private static final Pattern REWARD = Pattern.compile("sonraki odul:\\s*(\\d+)\\s*([ds])");
	private static final Pattern KRISTAL = Pattern.compile("kristal\\s+([0-9][0-9.,]*\\s*[kmb]?)");
	/** No AFK countdown for this long means the account left the AFK area. */
	private static final long AFK_TIMEOUT_MS = 75_000;
	/** Test runs log what bots hear and send. */
	private static final boolean VERBOSE = Boolean.getBoolean("chickenafk.bottest");

	private enum Phase { LOGIN, CONFIG, PLAY }

	public final Account account;
	private final Minecraft mc = Minecraft.getInstance();
	private final UUID uuid;
	private final Macros macros;
	private final Function<Account, String> server;

	private volatile Connection connection;
	private volatile Phase phase = Phase.LOGIN;
	/** The player asked for this account to run; drops reconnect while true. */
	private boolean wanted;
	private boolean connecting;
	private long reconnectAt;
	private long connectedAt;
	private String disconnectReason;

	private RegistryDataCollector collector = new RegistryDataCollector();
	private KnownPacksManager knownPacks;
	private RegistryAccess.Frozen registries;
	private ClientGamePacketListener gameListener;

	private PositionMoveRotation position = new PositionMoveRotation(Vec3.ZERO, Vec3.ZERO, 0, 0);
	private boolean loadedSent;
	private boolean joined;
	private int sequence;
	private int ticks;
	private long lastCountdownAt;
	private int lastCountdown = -1;
	private int lastMinutes = -1;

	Bot(Account account, Function<Account, String> server) {
		this.account = account;
		this.server = server;
		this.uuid = UUIDUtil.createOfflinePlayerUUID(account.name);
		this.macros = new Macros(this);
	}

	// --- lifecycle --------------------------------------------------------------------------

	void start() {
		wanted = true;
		reconnectAt = 0;
		if (connection == null && !connecting) {
			connect();
		}
	}

	void stop() {
		wanted = false;
		reconnectAt = 0;
		Connection c = connection;
		if (c != null) {
			c.disconnect(Component.literal("Durduruldu"));
		}
		account.state = Account.State.OFF;
	}

	/** In the world: past configuration and the server's join packet has arrived. */
	public boolean inPlay() {
		return connection != null && phase == Phase.PLAY && joined;
	}

	private void connect() {
		connecting = true;
		account.state = Account.State.CONNECTING;
		disconnectReason = null;
		String target = server.apply(account);
		account.note("Bağlanıyor: " + target);
		// name lookup and the TCP connect block, so they run off the game thread
		Thread thread = new Thread(() -> {
			try {
				ServerAddress address = ServerAddress.parseString(target);
				Optional<InetSocketAddress> resolved = ServerNameResolver.DEFAULT.resolveAddress(address)
						.map(ResolvedServerAddress::asInetSocketAddress);
				if (resolved.isEmpty()) {
					mc.execute(() -> failed("Sunucu adresi bulunamadı"));
					return;
				}
				InetSocketAddress socket = resolved.get();
				Connection c = Connection.connectToServer(socket, EventLoopGroupHolder.remote(mc.options.useNativeTransport()), null);
				c.initiateServerboundPlayConnection(socket.getHostName(), socket.getPort(), loginListener());
				c.send(new ServerboundHelloPacket(account.name, uuid));
				mc.execute(() -> {
					phase = Phase.LOGIN;
					connection = c;
					connecting = false;
					connectedAt = System.currentTimeMillis();
				});
			} catch (Exception e) {
				mc.execute(() -> failed(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
			}
		}, "ChickenAFK connect " + account.name);
		thread.setDaemon(true);
		thread.start();
	}

	private void failed(String reason) {
		connecting = false;
		disconnected(reason);
	}

	/** Runs every client tick on the game thread. */
	void tick() {
		ticks++;
		long now = System.currentTimeMillis();
		Connection c = connection;
		if (c != null) {
			if (c.isConnected()) {
				c.tick();
			} else {
				c.handleDisconnection();
				connection = null;
				disconnected(disconnectReason != null ? disconnectReason : "Bağlantı kapandı");
			}
		}
		if (connection == null && !connecting && wanted && reconnectAt > 0 && now >= reconnectAt) {
			reconnectAt = 0;
			connect();
		}
		if (inPlay()) {
			// stay "on the ground" so the server never counts the bot as flying
			if (ticks % 20 == 0) {
				send(new ServerboundMovePlayerPacket.StatusOnly(true, false));
			}
			if (account.state == Account.State.AFK && now - lastCountdownAt > AFK_TIMEOUT_MS) {
				account.state = Account.State.JOINING;
				account.afkSince = 0;
				account.note("AFK sayacı durdu, AFK alanında değil.");
			}
		}
		macros.tick(now);
	}

	private void disconnected(String reason) {
		boolean early = connectedAt > 0 && System.currentTimeMillis() - connectedAt < 15_000;
		connectedAt = 0;
		account.resetSession();
		account.lastReason = reason;
		phase = Phase.LOGIN;
		loadedSent = false;
		joined = false;
		lastCountdown = -1;
		lastMinutes = -1;
		collector = new RegistryDataCollector();
		knownPacks = null;
		if (reason != null) {
			account.note("Bağlantı koptu: " + reason);
		}
		macros.onDisconnect();
		if (wanted) {
			// a session still held by the proxy rejects us again; wait it out instead of hammering
			boolean collision = early || Text.contains(reason == null ? "" : reason, "zaten") || Text.contains(reason == null ? "" : reason, "already");
			long wait = collision ? COLLISION_RECONNECT_MS : RECONNECT_MS;
			reconnectAt = System.currentTimeMillis() + wait;
			account.state = Account.State.CONNECTING;
			account.note((wait / 1000) + " sn sonra yeniden bağlanacak.");
		} else {
			account.state = Account.State.OFF;
		}
	}

	// --- sending ----------------------------------------------------------------------------

	void send(Packet<?> packet) {
		Connection c = connection;
		if (c != null && c.isConnected()) {
			c.send(packet);
		}
	}

	/** A chat line, or a command when it starts with "/". */
	public void say(String text) {
		if (!inPlay() || text.isBlank()) {
			return;
		}
		if (VERBOSE) {
			ChickenAfk.LOG.info("[{}] send: {}", account.name, text);
		}
		if (text.startsWith("/")) {
			macros.recordCommand(text);
			// show what the bot typed, so a wrong /gir or /joinq can be seen in the panel
			account.note(text);
			send(new ServerboundChatCommandPacket(text.substring(1)));
		} else {
			send(new ServerboundChatPacket(text, Instant.now(), 0L, null, new LastSeenMessages.Update(0, new BitSet(), (byte) 0)));
		}
	}

	/** Clicks a slot of the open menu (or the player inventory when containerId is 0). */
	public void click(int containerId, int slot, int button, ClickType type) {
		int state = containerId == 0 || account.menu == null ? 0 : account.menu.stateId;
		send(new ServerboundContainerClickPacket(containerId, state, (short) slot, (byte) button, type,
				Int2ObjectMaps.emptyMap(), HashedStack.EMPTY));
	}

	public void closeMenu() {
		if (account.menu != null) {
			send(new ServerboundContainerClosePacket(account.menu.id));
			account.menu = null;
		}
	}

	/** Holds a hotbar slot and uses the item in it, like a right click in the air. */
	public void useHotbar(int slot) {
		send(new ServerboundSetCarriedItemPacket(slot));
		account.selectedSlot = slot;
		send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, ++sequence, position.yRot(), position.xRot()));
	}

	/** Right clicks a block, e.g. to open a chest next to the bot. */
	public void useBlock(BlockPos pos) {
		send(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false), ++sequence));
	}

	public BlockPos blockPos() {
		return BlockPos.containing(position.position());
	}

	/** Clicks the first menu slot whose item id or name contains the text. */
	boolean clickMenuItem(String text) {
		Account.Menu menu = account.menu;
		if (menu == null) {
			return false;
		}
		for (int i = 0; i < menu.slots.length; i++) {
			if (matches(menu.slots[i], text)) {
				click(menu.id, i, 0, ClickType.PICKUP);
				return true;
			}
		}
		return false;
	}

	boolean menuHas(String text) {
		Account.Menu menu = account.menu;
		if (menu == null) {
			return false;
		}
		for (ItemStack stack : menu.slots) {
			if (matches(stack, text)) {
				return true;
			}
		}
		return false;
	}

	private static boolean matches(ItemStack stack, String text) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}
		return Text.contains(stack.getItemHolder().getRegisteredName(), text) || Text.contains(stack.getHoverName().getString(), text);
	}

	// --- listeners --------------------------------------------------------------------------

	/**
	 * The vanilla listener interfaces have over a hundred methods; a proxy answers all of them. Login
	 * packets are handled right away on the network thread (compression must switch before the next
	 * packet is read); later phases are queued to the game thread like vanilla does.
	 *
	 * Methods are told apart by their signature, never by name: in a normal game Minecraft's method
	 * names are obfuscated (method_12345), so names only match in the development environment.
	 */
	private <T extends PacketListener> T proxy(Class<T> type, ConnectionProtocol protocol, boolean immediate) {
		InvocationHandler handler = (self, method, args) -> {
			if (method.getDeclaringClass() == Object.class) {
				// java.lang.Object's own methods keep their names
				return switch (method.getName()) {
					case "equals" -> self == args[0];
					case "hashCode" -> System.identityHashCode(self);
					default -> "ChickenAFK " + protocol + " listener for " + account.name;
				};
			}
			Class<?>[] params = method.getParameterTypes();
			Class<?> returns = method.getReturnType();
			// handleXxx(SomePacket): the packet handlers
			if (params.length == 1 && returns == void.class && !method.isDefault() && args[0] instanceof Packet<?> packet) {
				if (packet instanceof BundlePacket<?> bundle) {
					for (Packet<?> sub : bundle.subPackets()) {
						handleVia(sub, self);
					}
				} else if (immediate) {
					onLogin(packet);
				} else {
					mc.execute(() -> onPacket(packet));
				}
				return null;
			}
			// onDisconnect(DisconnectionDetails)
			if (params.length == 1 && params[0] == DisconnectionDetails.class) {
				Component reason = ((DisconnectionDetails) args[0]).reason();
				mc.execute(() -> {
					if (disconnectReason == null) {
						disconnectReason = reason.getString();
					}
				});
				return null;
			}
			// onPacketError(Packet, Exception): log instead of vanilla's crash report
			if (params.length == 2 && Packet.class.isAssignableFrom(params[0]) && Throwable.class.isAssignableFrom(params[1])) {
				ChickenAfk.LOG.warn("[{}] packet error in {}", account.name, args[0], (Throwable) args[1]);
				return null;
			}
			// flow(), protocol(), shouldHandleMessage(...) and the rest have sensible defaults
			if (method.isDefault()) {
				return InvocationHandler.invokeDefault(self, method, args);
			}
			// isAcceptingMessages(): the only abstract no-argument boolean
			if (params.length == 0 && returns == boolean.class) {
				return connection == null || connection.isConnected();
			}
			if (returns == PacketFlow.class) {
				return PacketFlow.CLIENTBOUND;
			}
			if (returns == ConnectionProtocol.class) {
				return protocol;
			}
			return defaultValue(returns);
		};
		return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
	}

	@SuppressWarnings("unchecked")
	private static <L extends PacketListener> void handleVia(Packet<?> packet, Object listener) {
		((Packet<L>) packet).handle((L) listener);
	}

	private static Object defaultValue(Class<?> type) {
		if (type == boolean.class) {
			return false;
		}
		if (type == int.class || type == long.class || type == short.class || type == byte.class) {
			return 0;
		}
		if (type == float.class || type == double.class) {
			return 0f;
		}
		return null;
	}

	private ClientLoginPacketListener loginListener() {
		return proxy(ClientLoginPacketListener.class, ConnectionProtocol.LOGIN, true);
	}

	private ClientConfigurationPacketListener configListener() {
		return proxy(ClientConfigurationPacketListener.class, ConnectionProtocol.CONFIGURATION, false);
	}

	private ClientGamePacketListener gameListener() {
		if (gameListener == null) {
			gameListener = proxy(ClientGamePacketListener.class, ConnectionProtocol.PLAY, false);
		}
		return gameListener;
	}

	// --- login (network thread) -------------------------------------------------------------

	private void onLogin(Packet<?> packet) {
		Connection c = connection;
		if (c == null) {
			// connection is set on the game thread a moment after the socket opens
			c = waitForConnection();
		}
		switch (packet) {
			case ClientboundLoginCompressionPacket p -> c.setupCompression(p.getCompressionThreshold(), false);
			case ClientboundLoginFinishedPacket p -> {
				c.setupInboundProtocol(ConfigurationProtocols.CLIENTBOUND, configListener());
				c.send(ServerboundLoginAcknowledgedPacket.INSTANCE);
				c.setupOutboundProtocol(ConfigurationProtocols.SERVERBOUND);
				c.send(new ServerboundCustomPayloadPacket(new BrandPayload("vanilla")));
				c.send(new ServerboundClientInformationPacket(ClientInformation.createDefault()));
				mc.execute(() -> {
					phase = Phase.CONFIG;
					account.state = Account.State.JOINING;
					account.note("Giriş tamam, sunucuya bağlanıldı.");
				});
			}
			case ClientboundHelloPacket p -> c.disconnect(Component.literal("Sunucu online-mode istiyor; offline hesap giremez"));
			case ClientboundCustomQueryPacket p -> c.send(new ServerboundCustomQueryAnswerPacket(p.transactionId(), null));
			case ClientboundLoginDisconnectPacket p -> {
				String reason = p.reason().getString();
				mc.execute(() -> disconnectReason = reason);
			}
			default -> {
			}
		}
	}

	private Connection waitForConnection() {
		for (int i = 0; i < 200 && connection == null; i++) {
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		return connection;
	}

	// --- configuration and play (game thread) ----------------------------------------------

	private void onPacket(Packet<?> packet) {
		if (connection == null) {
			return;
		}
		switch (packet) {
			// shared by configuration and play
			case ClientboundKeepAlivePacket p -> send(new ServerboundKeepAlivePacket(p.getId()));
			case ClientboundPingPacket p -> send(new ServerboundPongPacket(p.getId()));
			case ClientboundResourcePackPushPacket p -> {
				// accept without downloading; the bot never draws anything
				send(new ServerboundResourcePackPacket(p.id(), ServerboundResourcePackPacket.Action.ACCEPTED));
				send(new ServerboundResourcePackPacket(p.id(), ServerboundResourcePackPacket.Action.DOWNLOADED));
				send(new ServerboundResourcePackPacket(p.id(), ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED));
			}
			case ClientboundCookieRequestPacket p -> send(new ServerboundCookieResponsePacket(p.key(), null));
			case ClientboundDisconnectPacket p -> disconnectReason = p.reason().getString();
			case ClientboundTransferPacket p -> account.note("Sunucu başka bir adrese aktarmak istedi; desteklenmiyor.");
			case ClientboundUpdateTagsPacket p -> {
				if (phase == Phase.CONFIG) {
					collector.appendTags(p.getTags());
				}
			}

			// configuration
			case ClientboundRegistryDataPacket p -> collector.appendContents(p.registry(), p.entries());
			case ClientboundSelectKnownPacks p -> {
				knownPacks = new KnownPacksManager();
				send(new ServerboundSelectKnownPacks(knownPacks.trySelectingPacks(p.knownPacks())));
			}
			case ClientboundCodeOfConductPacket p -> send(ServerboundAcceptCodeOfConductPacket.INSTANCE);
			case ClientboundFinishConfigurationPacket p -> finishConfiguration();

			// play
			case ClientboundLoginPacket p -> {
				joined = true;
				loadedSent = false;
				account.menu = null;
			}
			case ClientboundRespawnPacket p -> {
				loadedSent = false;
				account.menu = null;
			}
			case ClientboundStartConfigurationPacket p -> {
				// a proxy moving us to another backend server: back to configuration, then play again
				phase = Phase.CONFIG;
				joined = false;
				account.menu = null;
				collector = new RegistryDataCollector();
				knownPacks = null;
				connection.setupInboundProtocol(ConfigurationProtocols.CLIENTBOUND, configListener());
				send(ServerboundConfigurationAcknowledgedPacket.INSTANCE);
				connection.setupOutboundProtocol(ConfigurationProtocols.SERVERBOUND);
			}
			case ClientboundPlayerPositionPacket p -> {
				position = PositionMoveRotation.calculateAbsolute(position, p.change(), p.relatives());
				send(new ServerboundAcceptTeleportationPacket(p.id()));
				send(new ServerboundMovePlayerPacket.PosRot(position.position(), position.yRot(), position.xRot(), true, false));
				if (!loadedSent) {
					loadedSent = true;
					send(new ServerboundPlayerLoadedPacket());
				}
			}
			case ClientboundChunkBatchFinishedPacket p -> send(new ServerboundChunkBatchReceivedPacket(2f));
			case ClientboundSetHealthPacket p -> {
				if (p.getHealth() <= 0) {
					send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
				}
			}
			case ClientboundSystemChatPacket p -> {
				if (VERBOSE) {
					ChickenAfk.LOG.info("[{}] system{}: {}", account.name, p.overlay() ? " (action bar)" : "", p.content().getString());
				}
				if (!p.overlay()) {
					String text = p.content().getString();
					account.addChat(null, text);
					macros.onMessage(text);
				}
			}
			case ClientboundPlayerChatPacket p -> {
				String sender = p.chatType().name().getString();
				String text = p.body().content();
				account.addChat(sender, text);
				macros.onMessage(sender + " » " + text);
			}
			case ClientboundDisguisedChatPacket p -> {
				String sender = p.chatType().name().getString();
				String text = p.message().getString();
				account.addChat(sender, text);
				macros.onMessage(sender + " » " + text);
			}
			case ClientboundSetTitleTextPacket p -> {
				if (Text.contains(p.text().getString(), "afk durumdasiniz")) {
					enterAfk();
				}
			}
			case ClientboundSetSubtitleTextPacket p -> countdown(p.text().getString());
			case ClientboundSetPlayerTeamPacket p -> p.getParameters().ifPresent(params ->
					kristal(params.getPlayerPrefix().getString() + params.getPlayerSuffix().getString()));
			case ClientboundPlayerInfoUpdatePacket p -> {
				for (ClientboundPlayerInfoUpdatePacket.Entry e : p.entries()) {
					if (uuid.equals(e.profileId()) && (p.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY)
							|| p.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER))) {
						account.ping = e.latency();
					}
				}
			}
			case ClientboundOpenScreenPacket p -> account.menu = new Account.Menu(p.getContainerId(), p.getTitle());
			case ClientboundContainerSetContentPacket p -> content(p);
			case ClientboundContainerSetSlotPacket p -> slot(p.getContainerId(), p.getSlot(), p.getItem(), p.getStateId());
			case ClientboundContainerClosePacket p -> account.menu = null;
			case ClientboundSetHeldSlotPacket p -> account.selectedSlot = p.slot();
			default -> {
			}
		}
	}

	/** Builds the game registries from what the server sent plus the client's own data, then enters play. */
	private void finishConfiguration() {
		RegistryAccess.Frozen base = ClientRegistryLayer.createRegistryAccess().compositeAccess();
		registries = withResources(resources -> collector.collectGameRegistries(resources, base, false));
		collector = new RegistryDataCollector();
		connection.setupInboundProtocol(GameProtocols.CLIENTBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(registries)), gameListener());
		send(ServerboundFinishConfigurationPacket.INSTANCE);
		connection.setupOutboundProtocol(GameProtocols.SERVERBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(registries), () -> false));
		phase = Phase.PLAY;
		account.state = Account.State.JOINING;
	}

	private <T> T withResources(Function<ResourceProvider, T> body) {
		if (knownPacks == null) {
			return body.apply(ResourceProvider.EMPTY);
		}
		try (CloseableResourceManager resources = knownPacks.createResourceManager()) {
			return body.apply(resources);
		}
	}

	// --- what the panel shows ---------------------------------------------------------------

	private void enterAfk() {
		lastCountdownAt = System.currentTimeMillis();
		if (account.state != Account.State.AFK) {
			account.state = Account.State.AFK;
			account.afkSince = System.currentTimeMillis();
			macros.setFlag("smp-afkte");
		}
	}

	/** "sonraki ödül: 2d" (minutes) or "…: 37s"; the seconds count only shows in the last minute. */
	private void countdown(String subtitle) {
		Matcher m = REWARD.matcher(Text.plain(subtitle));
		if (!m.find()) {
			return;
		}
		int value = Integer.parseInt(m.group(1));
		boolean minutes = m.group(2).equals("d");
		// "Nd" is shown while N:00 to N:59 is left; switching to it means N:59 is left
		int seconds = minutes ? value * 60 + 59 : value;
		long now = System.currentTimeMillis();
		// the counter jumping back up (…1s → 3d) is the moment a reward arrived
		if (lastCountdown >= 0 && lastCountdown <= 5 && seconds > 60) {
			account.rewards++;
			account.note("Ödül alındı (" + account.rewards + ". kez bu oturumda).");
		}
		if (!minutes || value != lastMinutes) {
			account.nextRewardAt = now + seconds * 1000L;
		}
		lastMinutes = minutes ? value : -1;
		lastCountdown = seconds;
		enterAfk();
	}

	private void kristal(String line) {
		String plain = Text.plain(line);
		Matcher m = KRISTAL.matcher(plain);
		if (m.find()) {
			// plain() keeps positions, so the value can be cut from the original text
			account.kristal = line.substring(m.start(1), m.end(1)).trim();
		}
	}

	private void content(ClientboundContainerSetContentPacket p) {
		if (p.containerId() == 0) {
			for (int i = 0; i < p.items().size(); i++) {
				playerSlot(i, p.items().get(i));
			}
			return;
		}
		Account.Menu menu = account.menu;
		if (menu == null || menu.id != p.containerId()) {
			return;
		}
		// the server sends the menu's slots followed by the 36 player inventory slots
		int own = Math.max(0, p.items().size() - 36);
		menu.slots = p.items().subList(0, own).toArray(ItemStack[]::new);
		menu.stateId = p.stateId();
		for (int i = own; i < p.items().size(); i++) {
			menuPlayerSlot(i - own, p.items().get(i));
		}
		if (!menu.filled) {
			menu.filled = true;
			macros.onMenuOpened();
		}
	}

	private void slot(int containerId, int slot, ItemStack stack, int stateId) {
		if (containerId == 0) {
			playerSlot(slot, stack);
			return;
		}
		Account.Menu menu = account.menu;
		if (menu == null || menu.id != containerId) {
			return;
		}
		menu.stateId = stateId;
		if (slot >= 0 && slot < menu.slots.length) {
			menu.slots[slot] = stack;
		} else if (slot >= menu.slots.length) {
			menuPlayerSlot(slot - menu.slots.length, stack);
		}
	}

	/** Inventory menu slots: 5-8 armor, 9-35 main, 36-44 hotbar. */
	private void playerSlot(int slot, ItemStack stack) {
		if (slot >= 5 && slot <= 8) {
			account.inventory[Account.ARMOR + slot - 5] = stack;
		} else if (slot >= 9 && slot <= 35) {
			account.inventory[Account.MAIN + slot - 9] = stack;
		} else if (slot >= 36 && slot <= 44) {
			account.inventory[Account.HOTBAR + slot - 36] = stack;
		}
	}

	/** Player part of an open menu: 27 main slots then 9 hotbar slots. */
	private void menuPlayerSlot(int index, ItemStack stack) {
		if (index < 27) {
			account.inventory[Account.MAIN + index] = stack;
		} else if (index < 36) {
			account.inventory[Account.HOTBAR + index - 27] = stack;
		}
	}

	/** Slot number of a player inventory index (as in {@link Account#inventory}) inside the open menu or the inventory menu. */
	public int menuSlotFor(int inventoryIndex) {
		Account.Menu menu = account.menu;
		if (menu == null) {
			if (inventoryIndex >= Account.ARMOR) {
				return 5 + inventoryIndex - Account.ARMOR;
			}
			return inventoryIndex < Account.MAIN ? 36 + inventoryIndex : inventoryIndex;
		}
		if (inventoryIndex >= Account.ARMOR) {
			return -1;
		}
		int base = menu.slots.length;
		return inventoryIndex < Account.MAIN ? base + 27 + inventoryIndex : base + inventoryIndex - Account.MAIN;
	}

	public RegistryAccess registries() {
		return registries;
	}

	/** Macro flags, e.g. "smp-afkte". For the test run. */
	public Set<String> flags() {
		return macros.flags();
	}

	/** Whether the bot sent a command containing the text recently. For the test run. */
	public boolean sentCommand(String text) {
		return macros.sent(text);
	}

	/** How many recent commands contained the text. For the test run. */
	public int sentCount(String text) {
		return macros.sentCount(text);
	}
}
