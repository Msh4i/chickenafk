package dev.chickenafk.mixin;

import dev.chickenafk.ui.MenuWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the ChickenAFK card to the main menu, where bots keep running without a world loaded. */
@Mixin(TitleScreen.class)
abstract class TitleScreenMixin extends Screen {
	private TitleScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void chickenafk$addCard(CallbackInfo ci) {
		addRenderableWidget(new MenuWidget(this));
	}
}
