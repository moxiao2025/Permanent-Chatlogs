package lovexyn0827.chatlog.mixin;

import lovexyn0827.chatlog.config.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import lovexyn0827.chatlog.session.SessionRecorder;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;

@Mixin(Hud.class)
public class InGameHudMixin {
	@Inject(
			method = "setOverlayMessage", 
			at = @At("HEAD")
	)
	private void onOverlayMessage(Component message, boolean tinted, CallbackInfo ci) {
		if (Options.saveOverlays && SessionRecorder.current() != null) {
			SessionRecorder.current().addOverlayMessage(message, tinted, System.currentTimeMillis());
		}
	}
	
	@Inject(
			method = "setTitle", 
			at = @At("HEAD")
	)
	private void onTitle(Component message, CallbackInfo ci) {
		if (Options.saveTitles && SessionRecorder.current() != null) {
			SessionRecorder.current().addTitle(message, System.currentTimeMillis());
		}
	}
	
	@Inject(
			method = "setSubtitle", 
			at = @At("HEAD")
	)
	private void onSubtitle(Component message, CallbackInfo ci) {
		if (Options.saveSubtitles && SessionRecorder.current() != null) {
			SessionRecorder.current().addSubtitle(message, System.currentTimeMillis());
		}
	}
}
