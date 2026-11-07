package dev.chickenafk.mixin;

import dev.chickenafk.ui.MenuWidget;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The same card in the Esc menu, so the panel is one click away while playing too. */
@Mixin(PauseScreen.class)
abstract class PauseScreenMixin extends Screen {
	private PauseScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void chickenafk$addCard(CallbackInfo ci) {
		// F3+Esc pauses without the menu; leave that screen empty like vanilla does
		if (((PauseScreen) (Object) this).showsPauseMenu()) {
			addRenderableWidget(new MenuWidget(this));
		}
	}
}
