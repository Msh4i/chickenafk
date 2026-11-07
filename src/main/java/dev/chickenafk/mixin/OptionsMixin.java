package dev.chickenafk.mixin;

import dev.chickenafk.ChickenAfk;
import java.util.Arrays;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds our key to the controls list before options.txt is read, so the saved binding applies. */
@Mixin(Options.class)
abstract class OptionsMixin {
	@Shadow @Final @Mutable public KeyMapping[] keyMappings;

	@Inject(method = "load", at = @At("HEAD"))
	private void chickenafk$addKeys(CallbackInfo ci) {
		KeyMapping key = ChickenAfk.Keys.OPEN_PANEL;
		if (Arrays.asList(keyMappings).contains(key)) {
			return;
		}
		KeyMapping[] grown = Arrays.copyOf(keyMappings, keyMappings.length + 1);
		grown[keyMappings.length] = key;
		keyMappings = grown;
	}
}
