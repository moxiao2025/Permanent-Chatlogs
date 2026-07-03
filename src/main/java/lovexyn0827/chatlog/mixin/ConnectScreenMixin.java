package lovexyn0827.chatlog.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import lovexyn0827.chatlog.session.SessionRecorder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;

@Mixin(ConnectScreen.class)
public class ConnectScreenMixin {
	@Inject(method = "startConnecting(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/Minecraft;"
			+ "Lnet/minecraft/client/multiplayer/resolver/ServerAddress;Lnet/minecraft/client/multiplayer/ServerData;"
			+ "ZLnet/minecraft/client/multiplayer/TransferState;)V", at = @At("HEAD"))
	private void onConnect(net.minecraft.client.gui.screens.Screen parent, Minecraft mc, 
			ServerAddress addr, ServerData info, boolean isQuickPlay, 
			net.minecraft.client.multiplayer.TransferState transferState, CallbackInfo ci) {
		if (info != null) {
			SessionRecorder.start(info.name, true);
		}
	}
}
