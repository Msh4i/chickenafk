package dev.chickenafk.mixin;

import dev.chickenafk.TextCapture;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.world.BossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Feeds server text to {@link TextCapture}. Handlers first run on the network thread and are then
 * re-queued on the main thread, so we only record the main-thread pass.
 */
@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerMixin {
	private static boolean onMainThread() {
		return Minecraft.getInstance().isSameThread();
	}

	@Inject(method = "setTitleText", at = @At("HEAD"))
	private void chickenafk$title(ClientboundSetTitleTextPacket packet, CallbackInfo ci) {
		if (onMainThread()) {
			TextCapture.text("TITLE", packet.text());
		}
	}

	@Inject(method = "setSubtitleText", at = @At("HEAD"))
	private void chickenafk$subtitle(ClientboundSetSubtitleTextPacket packet, CallbackInfo ci) {
		if (onMainThread()) {
			TextCapture.text("SUBTITLE", packet.text());
		}
	}

	@Inject(method = "setActionBarText", at = @At("HEAD"))
	private void chickenafk$actionBar(ClientboundSetActionBarTextPacket packet, CallbackInfo ci) {
		if (onMainThread()) {
			TextCapture.text("ACTIONBAR", packet.text());
		}
	}

	@Inject(method = "handleSystemChat", at = @At("HEAD"))
	private void chickenafk$systemChat(ClientboundSystemChatPacket packet, CallbackInfo ci) {
		if (onMainThread()) {
			if (packet.overlay()) {
				TextCapture.text("ACTIONBAR", packet.content());
			} else {
				TextCapture.ifRelevant("CHAT", packet.content());
			}
		}
	}

	@Inject(method = "handleBossUpdate", at = @At("HEAD"))
	private void chickenafk$bossBar(ClientboundBossEventPacket packet, CallbackInfo ci) {
		if (!onMainThread()) {
			return;
		}
		packet.dispatch(new ClientboundBossEventPacket.Handler() {
			@Override
			public void add(UUID id, Component name, float progress, BossEvent.BossBarColor color,
					BossEvent.BossBarOverlay overlay, boolean darkenScreen, boolean playMusic, boolean createWorldFog) {
				TextCapture.text("BOSSBAR", name);
			}

			@Override
			public void updateName(UUID id, Component name) {
				TextCapture.text("BOSSBAR", name);
			}
		});
	}

	@Inject(method = "handleSetScore", at = @At("HEAD"))
	private void chickenafk$score(ClientboundSetScorePacket packet, CallbackInfo ci) {
		if (onMainThread()) {
			String line = packet.owner() + " = " + packet.score()
					+ packet.display().map(d -> " (" + d.getString() + ")").orElse("");
			TextCapture.ifRelevant("SCORE", line);
		}
	}

	@Inject(method = "handleSetPlayerTeamPacket", at = @At("HEAD"))
	private void chickenafk$sidebarLine(ClientboundSetPlayerTeamPacket packet, CallbackInfo ci) {
		if (onMainThread()) {
			packet.getParameters().ifPresent(p -> TextCapture.ifRelevant("SIDEBAR",
					p.getPlayerPrefix().getString() + p.getPlayerSuffix().getString()));
		}
	}
}
