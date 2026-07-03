package lovexyn0827.chatlog.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import lovexyn0827.chatlog.gui.SessionListScreen;
import lovexyn0827.chatlog.i18n.I18N;
import lovexyn0827.chatlog.session.UnsavedChatlogRecovery;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {
	protected TitleScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("RETURN"))
	private void onInit(CallbackInfo ci) {
		this.addRenderableWidget(Button.builder(I18N.translateAsText("gui.chatlogs"), 
						(btn) -> this.minecraft.setScreen(new SessionListScreen(minecraft.screen)))
				.bounds(this.width / 2 - 100, (this.height / 4 + 48) + 92 + 12, 98, 20)
				.build());
		UnsavedChatlogRecovery.tryRestoreUnsaved();
	}
}
