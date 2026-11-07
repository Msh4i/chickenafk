package dev.chickenafk.mixin;

import dev.chickenafk.ChickenAfk;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class MinecraftMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void chickenafk$tick(CallbackInfo ci) {
		ChickenAfk.onClientTick((Minecraft) (Object) this);
	}
}
