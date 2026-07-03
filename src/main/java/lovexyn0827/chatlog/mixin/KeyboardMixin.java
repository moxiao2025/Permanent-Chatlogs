package lovexyn0827.chatlog.mixin;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import lovexyn0827.chatlog.gui.NewEventMarkerScreen;
import lovexyn0827.chatlog.i18n.I18N;
import lovexyn0827.chatlog.session.Session;
import lovexyn0827.chatlog.session.SessionRecorder;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardMixin {
	@Shadow @Final Minecraft minecraft;
	
	@Inject(method = "keyPress", at = @At("RETURN"))
	private void handleKey(long window, int action, KeyEvent event, CallbackInfo ci) {
		boolean isBeingPressed = action == GLFW.GLFW_PRESS;
		if(event.key() == 'M' && Minecraft.getInstance().hasControlDown() && isBeingPressed && SessionRecorder.current() != null) {
			if (Minecraft.getInstance().hasAltDown()) {
				Component title = I18N.translateAsText("gui.marker.title");
				Session.Event sessionEvent = new Session.Event(title, 
						System.currentTimeMillis(), DyeColor.RED.getTextColor());
				SessionRecorder.current().addEvent(sessionEvent);
				this.minecraft.gui.setOverlayMessage(title, true);
				return;
			}
			
			Minecraft.getInstance().setScreen(new NewEventMarkerScreen());
		}
	}
}
