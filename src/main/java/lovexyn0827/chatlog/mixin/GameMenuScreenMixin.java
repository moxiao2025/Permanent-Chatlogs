package lovexyn0827.chatlog.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import lovexyn0827.chatlog.gui.SessionListScreen;
import lovexyn0827.chatlog.i18n.I18N;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@Mixin(PauseScreen.class)
public class GameMenuScreenMixin extends Screen {
	protected GameMenuScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void appendButtons(CallbackInfo ci) {
		Button chatlogBtn = Button.builder(I18N.translateAsText("gui.chatlogs"), (btn) -> {
			this.minecraft.setScreen(new SessionListScreen(minecraft.screen));
		}).bounds(this.width / 2 - 102, this.height / 4 + 144, 204, 20).build();
		this.addRenderableWidget(chatlogBtn);
	}
}
