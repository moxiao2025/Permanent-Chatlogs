package lovexyn0827.chatlog.gui;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import lovexyn0827.chatlog.config.Option;
import lovexyn0827.chatlog.config.OptionType;
import lovexyn0827.chatlog.config.Options;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class SettingScreen extends Screen {
    private final Screen parent;
	private OptionListWidget optionList;
	
	protected SettingScreen(Screen parent) {
		super(Component.literal("Settings"));
        this.parent = parent;
	}

	@Override
	public void init() {
		this.optionList = new OptionListWidget(this.minecraft);
		for(Field f : Options.class.getDeclaredFields()) {
			Option o = f.getAnnotation(Option.class);
			if(o == null) {
				continue;
			}
			
			this.optionList.addOption(f);
		}
		
		this.addRenderableWidget(this.optionList);
	}
	
	@Override
	public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
		super.extractRenderState(ctx, mouseX, mouseY, delta);
	}
	
	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}
	
	private final class OptionListWidget extends AbstractSelectionList<OptionListWidget.Entry> {
		public OptionListWidget(Minecraft client) {
			super(client, SettingScreen.this.width, SettingScreen.this.height - 32, 16, 18);
		}
		
		@Override
		public int getRowWidth() {
			return (int) (this.minecraft.getWindow().getGuiScaledWidth() * 0.8F);
		}
		
		protected int addOption(Field f) {
			if (f.getAnnotationsByType(Option.class)[0].type() == OptionType.BOOLEAN) {
				return this.addEntry(new BooleanEntry(f));
			} else {
				return this.addEntry(new TextEntry(f));
			}
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput var1) {
		}
		
		private class Entry extends AbstractSelectionList.Entry<Entry> {
			protected final Component name;
			
			protected Entry(Field f) {
				this.name = Component.literal(f.getName());
			}
			
			@Override
			public void extractContent(GuiGraphicsExtractor ctx, int mouseX, int mouseY, 
					boolean hovering, float partialTick) {
				int y = this.getContentY();
				int xOffset = ((int) (this.getContentWidth() * 0.25));
				ctx.text(SettingScreen.this.font, this.name, xOffset + this.getContentX(), y, 0xFF31F38B);
				// Note: hovering tooltip position uses the parent screen's mouseX/mouseY
				// which is not available in extractContent in 26.1.2.
				// Tooltips for hover events in list entries are handled differently.
			}
		}
		
		private class TextEntry extends Entry {
			private final EditBox valueSelector;
			
			protected TextEntry(Field f) {
				super(f);
				int width = SettingScreen.this.minecraft.getWindow().getGuiScaledWidth();
				this.valueSelector = new EditBox(SettingScreen.this.font, 
						(int) (width * 0.55), 1, 
						(int) (width * 0.20), 14, this.name);
				try {
					this.valueSelector.setValue(f.get(null).toString());
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
				
				this.valueSelector.setResponder((s) -> {
					Options.set(this.name.getString(), s);
				});
				SettingScreen.this.addRenderableWidget(this.valueSelector);
			}
			
			@Override
			public void extractContent(GuiGraphicsExtractor ctx, int mouseX, int mouseY, 
					boolean hovering, float partialTick) {
				int y = this.getContentY();
				super.extractContent(ctx, mouseX, mouseY, hovering, partialTick);
				this.valueSelector.setY(y);
				this.valueSelector.extractWidgetRenderState(ctx, this.getContentX(), y, partialTick);
			}
		}
		
		private class BooleanEntry extends Entry {
			private final Checkbox valueSelector;
			
			protected BooleanEntry(Field f) {
				super(f);
				boolean toggled;
				try {
					toggled = Boolean.valueOf(f.get(null).toString());
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
				
				int guiWidth = SettingScreen.this.minecraft.getWindow().getGuiScaledWidth();
				this.valueSelector = Checkbox.builder(Component.empty(), SettingScreen.this.font)
						.pos((int) (guiWidth * 0.75) - 16, 1)
						.selected(toggled)
						.onValueChange((widget, checked) -> {
							Options.set(BooleanEntry.this.name.getString(), Boolean.toString(checked));
						})
						.build();
				SettingScreen.this.addRenderableWidget(this.valueSelector);
			}
			
			@Override
			public void extractContent(GuiGraphicsExtractor ctx, int mouseX, int mouseY, 
					boolean hovering, float partialTick) {
				int y = this.getContentY();
				super.extractContent(ctx, mouseX, mouseY, hovering, partialTick);
				this.valueSelector.setY(y);
				this.valueSelector.extractContents(ctx, this.getContentX(), y, partialTick);
			}
		}
	}
}
