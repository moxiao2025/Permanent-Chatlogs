package lovexyn0827.chatlog.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import lovexyn0827.chatlog.config.Options;
import net.minecraft.client.gui.components.ChatComponent;

@Mixin(value = ChatComponent.class, priority = 408)
public class ChaHudMixin_overrideMaxMessages {
	@ModifyConstant(
			method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;"
					+ "Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V", 
			constant = @Constant(intValue = 100), 
			require = 0
	)
	private int overrideMaxMessages(int initial) {
		return Options.visibleLineCount;
	}
}
