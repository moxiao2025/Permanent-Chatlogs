package lovexyn0827.chatlog.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import lovexyn0827.chatlog.session.SessionRecorder;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.WorldStem;
import net.minecraft.world.level.storage.LevelStorageSource;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {
	@Inject(
			method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V", 
			at = @At(value = "HEAD")
	)
	private void onDisconnected(CallbackInfo ci) {
		// Pause instead of ending: BungeeCord/Velocity transfers will resume the 
		// same session on reconnect. Stale sessions are cleaned up after 30s.
		SessionRecorder.prepareForTransfer();
	}
	
	@Inject(method = "stop", at = @At(value = "HEAD"))
	private void onStop(CallbackInfo ci) {
		SessionRecorder.end(true);
	}
	
	@Inject(
			method= "doWorldLoad", 
			at = @At(
					value = "INVOKE", 
					target = "net/minecraft/client/Minecraft.disconnectWithProgressScreen()V", 
					shift = At.Shift.AFTER
			)
	)
	private void onStartSingleplayer(LevelStorageSource.LevelStorageAccess session, PackRepository dataPackManager, 
			WorldStem worldStem, java.util.Optional<net.minecraft.world.level.gamerules.GameRules> gameRules, 
			boolean newWorld, CallbackInfo ci) {
		SessionRecorder.start(session.getLevelId(), false);
	}
}
