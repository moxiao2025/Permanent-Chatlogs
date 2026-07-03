package lovexyn0827.chatlog.gui;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

/**
 * Helper class to replace ChatMessages.breakRenderedChatMessageLines() which was removed in 26.1.
 */
public final class ComponentSplitter {
	
	/**
	 * Wraps a Component into multiple FormattedCharSequences suitable for rendering within the given width.
	 * Equivalent to ChatMessages.breakRenderedChatMessageLines() in pre-26.1 versions.
	 */
	public static List<FormattedCharSequence> wrapComponents(Component text, int maxWidth, Font font) {
		return font.split(text, maxWidth);
	}
	
	/**
	 * Wraps a FormattedText into multiple FormattedCharSequences suitable for rendering within the given width.
	 */
	public static List<FormattedCharSequence> wrapComponents(FormattedText text, int maxWidth, Font font) {
		return font.split(text, maxWidth);
	}
	
	/**
	 * Finds the Style at a given pixel position within a FormattedCharSequence.
	 * Replaces StringSplitter.componentStyleAtWidth() which was removed in 26.1.
	 */
	@Nullable
	public static Style styleAtWidth(Font font, FormattedCharSequence text, int targetWidth) {
		float[] currentWidth = new float[] { 0 };
		Style[] foundStyle = new Style[] { null };
		
		text.accept((index, style, codepoint) -> {
			float charWidth = font.getSplitter().stringWidth(new String(Character.toChars(codepoint)));
			if (currentWidth[0] <= targetWidth && currentWidth[0] + charWidth > targetWidth) {
				foundStyle[0] = style;
				return false;
			}
			currentWidth[0] += charWidth;
			return true;
		});
		
		return foundStyle[0];
	}
}
