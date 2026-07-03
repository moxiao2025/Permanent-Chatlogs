package lovexyn0827.chatlog.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import lovexyn0827.chatlog.session.SessionRecorder;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;

@Mixin(JoinMultiplayerScreen.class)
public class JoinMultiplayerScreenMixin {
	@Inject(method = "join(Lnet/minecraft/client/multiplayer/ServerData;)V", at = @At("HEAD"))
	private void onJoin(ServerData serverData, CallbackInfo ci) {
		if (serverData != null) {
			try {
				SessionRecorder.start(serverData.name, true);
			} catch (Exception e) {
				lovexyn0827.chatlog.PermanentChatLogMod.LOGGER.error("Failed to start session recording", e);
			}
		}
	}
}
