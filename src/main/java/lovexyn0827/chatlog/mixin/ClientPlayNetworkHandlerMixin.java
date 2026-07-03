package lovexyn0827.chatlog.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import lovexyn0827.chatlog.session.SessionRecorder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;

/**
 * Captures ALL multiplayer server connections, including:
 * - Manual join via server list (JoinMultiplayerScreenMixin already handles start)
 * - Direct connect / Quick Play (ConnectScreenMixin already handles start)
 * - BungeeCord / Velocity sub-server transfers
 * - Automatic reconnections
 * 
 * handleLogin is called once per connection when the client enters PLAY state.
 * Priority: existing session > paused transfer session > new session.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPlayNetworkHandlerMixin {
	@Inject(method = "handleLogin(Lnet/minecraft/network/protocol/game/ClientboundLoginPacket;)V", 
			at = @At("HEAD"))
	private void onHandleLogin(ClientboundLoginPacket packet, CallbackInfo ci) {
		// 1. Session already active (created by GUI mixins) — nothing to do
		if (SessionRecorder.current() != null) {
			return;
		}
		// 2. Try to resume a paused transfer session (BungeeCord/Velocity sub-server switch)
		if (SessionRecorder.resumeFromTransfer()) {
			return;
		}
		// 3. No session at all — create a new one (fallback for uncommon paths)
		ServerData serverData = Minecraft.getInstance().getCurrentServer();
		String name = serverData != null ? serverData.name : "Unknown Server";
		SessionRecorder.start(name, true);
	}
}
