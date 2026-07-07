package lovexyn0827.chatlog.gui;

import com.google.common.collect.Lists;

import lovexyn0827.chatlog.i18n.I18N;
import lovexyn0827.chatlog.session.Session;
import lovexyn0827.chatlog.session.SessionRecorder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;

public class NewEventMarkerScreen extends Screen {
	private static final Component TITLE = I18N.translateAsText("gui.marker.title");
	private EditBox name;
	private CycleButton<DyeColor> color;

	public NewEventMarkerScreen() {
		super(TITLE);
	}

	@SuppressWarnings("resource")
	@Override
	protected void init() {
		Minecraft mc = Minecraft.getInstance();
		int width = mc.getWindow().getGuiScaledWidth();
		int height = mc.getWindow().getGuiScaledHeight();
		this.addRenderableOnly(new StringWidget((int) (width * 0.3), (int) (this.height * 0.25) - 25, 
				(int) (width * 0.4), 23, 
				TITLE, Minecraft.getInstance().font));
		this.name = new EditBox(mc.font, 
				(int) (width * 0.3), (int) (this.height * 0.25), 
				(int) (width * 0.4), 23, 
				I18N.translateAsText("gui.marker.name"));
		this.color = CycleButton.<DyeColor>builder((c) -> {
					return Component.translatable(c.getName().toUpperCase()).withColor(c.getTextColor());
				}, DyeColor.WHITE)
				.withValues(Lists.newArrayList(DyeColor.values()))
				.create((int) (width * 0.3), (int) (height * 0.25) + 27, 
						(int) (width * 0.4), 23, 
						I18N.translateAsText("gui.marker.color"));
		this.addRenderableWidget(this.name);
		this.addRenderableWidget(this.color);
		Button saveBtn = Button.builder(CommonComponents.GUI_DONE, (b) -> {
			String name = this.name.getValue();
			DyeColor color = this.color.getValue();
			Component title = Component.literal(name);
			Session.Event event = new Session.Event(title, System.currentTimeMillis(), color.getTextColor());
			if (SessionRecorder.current() != null) {
				SessionRecorder.current().addEvent(event);
			}
			
			mc.gui.hud.setOverlayMessage(title, true);
			this.onClose();
		}).bounds((int) (width * 0.3), (int) (this.height * 0.25) + 54, (int) (width * 0.4), 23).build();
		this.addRenderableWidget(saveBtn);
	}
}
