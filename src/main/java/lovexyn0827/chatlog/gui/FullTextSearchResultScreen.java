package lovexyn0827.chatlog.gui;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.mojang.datafixers.util.Pair;

import lovexyn0827.chatlog.i18n.I18N;
import lovexyn0827.chatlog.session.Session;
import lovexyn0827.chatlog.session.Session.Line;
import lovexyn0827.chatlog.session.Session.Summary;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Util;
import net.minecraft.client.input.MouseButtonEvent;

public class FullTextSearchResultScreen extends Screen {
    private final Screen parent;
	private final ConcurrentHashMap<Summary, List<Pair<Integer, Line>>> results;
	private SessionList sessions;
	private MessageList messages;

	protected FullTextSearchResultScreen(
            Screen parent,
            ConcurrentHashMap<Summary, List<Pair<Integer, Line>>> results
    ) {
		super(I18N.translateAsText("gui.filter.result"));
		this.parent = parent;
        this.results = results;
	}
	
	@Override
	public void init() {
		this.sessions = new SessionList(this.minecraft, this.results.keySet());
		this.addRenderableWidget(sessions);
		this.messages = new MessageList(this.minecraft);
		this.addRenderableWidget(messages);
	}
	
	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}
	
	@Override
	public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
		super.extractRenderState(ctx, mouseX, mouseY, delta);
		ctx.centeredText(this.font, I18N.translateAsText("gui.filter.result"), 
				this.width / 2, 5, 0xFFFFFFFF);
	}
	
	private class SessionList extends ObjectSelectionList<SessionList.Entry> {
		public SessionList(Minecraft mc, Set<Session.Summary> sessions) {
			super(mc, (int) (FullTextSearchResultScreen.this.width * 0.38), 
					FullTextSearchResultScreen.this.height - 30, 
					20, 32);
			sessions.stream()
					.sorted((s1, s2) -> (int) (s2.startTime - s1.startTime))
					.map(Entry::new)
					.forEach(this::addEntry);
			this.setX(this.getRowLeft());
		}
		
		@Override
		public int getRowLeft() {
			return (int) (FullTextSearchResultScreen.this.width * 0.12 - 10);
		}
		
		@Override
		public int getRowWidth() {
			return this.width;
		}
		
		@Override
		protected int scrollBarX() {
			return FullTextSearchResultScreen.this.width / 2 - 10;
		}
		
		@Override
		public void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput builder) {
		}
		
		private class Entry extends ObjectSelectionList.Entry<Entry> {
			protected final Session.Summary summary;
			private final Component saveName;
			private final Component start;
			private final Component sizeAndTimeLength;
			private long lastClick = 0;
			
			public Entry(Session.Summary info) {
				this.summary = info;
				this.saveName = Component.literal(info.saveName);
				this.start = Component.literal(info.getFormattedStartTime())
						.withStyle(ChatFormatting.GRAY);
				long delta = (long) Math.floor((info.endTime - info.startTime) / 1000);
				this.sizeAndTimeLength = Component.literal(String.format(I18N.translate("gui.sizeandtime"), 
						(int) Math.floor(delta / 3600), (int) Math.floor((delta % 3600) / 60), delta % 60, info.size))
						.withStyle(ChatFormatting.GRAY);
			}
			
			@Override
			public boolean mouseClicked(MouseButtonEvent mouseEvent, boolean doubleClick) {
				FullTextSearchResultScreen.this.messages.setSession(
						this.summary, 
						FullTextSearchResultScreen.this.results.get(this.summary));
				if (this.isFocused() && Util.getMillis() - this.lastClick < 1000) {
					GuiUtils.loadSession(FullTextSearchResultScreen.this.minecraft, 
							this.summary, FullTextSearchResultScreen.this);
					return true;
				}

				SessionList.this.setFocused(this);
				this.lastClick = Util.getMillis();
				return true;
			}

			@Override
			public Component getNarration() {
				return this.saveName;
			}

			@Override
			public void extractContent(GuiGraphicsExtractor ctx, int mouseX, int mouseY, 
					boolean hovering, float partialTick) {
				int x = this.getContentX();
				int y = this.getContentY();
				Font tr = FullTextSearchResultScreen.this.font;
				ctx.text(tr, this.saveName, x, y, 0xFFFFFFFF);
				ctx.text(tr, this.start, x, y + 10, 0xFFFFFFFF);
				ctx.text(tr, this.sizeAndTimeLength, x, y + 20, 0xFFFFFFFF);
			}
			
		}
	}
	
	private class MessageList extends AbstractSelectionList<MessageList.Entry> {
		private Session.Summary currentSessionSummary;
		
		public MessageList(Minecraft mc) {
			super(mc, (int) (FullTextSearchResultScreen.this.width * 0.38), 
					FullTextSearchResultScreen.this.height - 30, 
					20, mc.font.lineHeight + 1);
			this.setX(this.getRowLeft());
		}
		
		@Override
		public int getRowLeft() {
			return (int) (FullTextSearchResultScreen.this.width * 0.5 + 4);
		}
		
		@Override
		public int getRowWidth() {
			return this.width;
		}
		
		@Override
		protected int scrollBarX() {
			return (int) (FullTextSearchResultScreen.this.width * 0.88);
		}
		
		@Override
		protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput builder) {
		}
		
		public void setSession(Session.Summary summary, List<Pair<Integer, Session.Line>> lines) {
			this.currentSessionSummary = summary;
			this.clearEntries();
			FormattedCharSequence title = I18N.translateAsText("gui.filter.matchcnt", lines.size()).getVisualOrderText();
			this.addEntry(new Entry(title, null, -1));
			for (Pair<Integer, Session.Line> e : lines) {
				this.addEntry(new Entry(Component.empty().getVisualOrderText(), null, -1));
				ComponentSplitter.wrapComponents(e.getSecond().message, 
						this.width - 10, 
						FullTextSearchResultScreen.this.font).forEach((t) -> {
							this.addEntry(new Entry(t, e.getSecond(), e.getFirst()));
						});
			}
		}

		private class Entry extends AbstractSelectionList.Entry<Entry> {
			private final FormattedCharSequence text;
			private final Line owner;
			private final int ordinalInSession;
			private long lastClick = 0;
			
			public Entry(FormattedCharSequence text, Session.Line owner, int ord) {
				this.text = text;
				this.owner = owner;
				this.ordinalInSession = ord;
			}
			
			public String getFormattedTime() {
				return (this.owner.time == 0L) ? I18N.translate("gui.unknowntime") : 
						Instant.ofEpochMilli(this.owner.time)
								.atZone(MessageList.this.currentSessionSummary.timeZone.toZoneId())
								.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
			}
			
			@Override
			public void extractContent(GuiGraphicsExtractor ctx, int mouseX, int mouseY, 
					boolean hovering, float partialTick) {
				int x = this.getContentX();
				int y = this.getContentY();
				Font tr = FullTextSearchResultScreen.this.font;
				ctx.text(tr, this.text, x + 4, y, 0xFFFFFFFF);
				ctx.fill(x + 1, y, x + 3, y + 10, this.owner == null ? 0 : this.owner.getMarkColor());
				// Note: hovering tooltip in extractContent can't use mouseX/mouseY directly in 26.1.2.
				// The tooltip for hover events in list entries is simplified here.
			}
			
			@Nullable
			private Component getToolTip(double mouseX, double mouseY) {
				Font tr = FullTextSearchResultScreen.this.font;
				double scale = FullTextSearchResultScreen.this.minecraft.getWindow().getGuiScale();
				int pos = (int) Math.floor(mouseX - (MessageList.this.getX() + 4) * scale);
				Style style = ComponentSplitter.styleAtWidth(tr, this.text, pos);
				if(style != null) {
					HoverEvent he;
					boolean hasHoverText = false;
					if((he = style.getHoverEvent()) != null && !Minecraft.getInstance().hasAltDown()) {
						if(he.action() == HoverEvent.Action.SHOW_TEXT) {
							hasHoverText = true;
							Component hoverText = ChatLogScreen.getHoverShowTextValue(he);
							if (hoverText != null) return hoverText;
						}
					}
					
					ClickEvent ce;
					if((ce = style.getClickEvent()) != null) {
						if(!hasHoverText) {
							return Component.literal(ce.toString());
						}
					}
				}
				
				return null;
			}
			
			@Override
			public boolean mouseClicked(MouseButtonEvent mouseEvent, boolean doubleClick) {
				double mouseX = mouseEvent.x();
				double mouseY = mouseEvent.y();
				if(Minecraft.getInstance().hasControlDown()) {
					Component tip = this.getToolTip(mouseX, mouseY);
					if(tip != null) {
						FullTextSearchResultScreen.this.minecraft.keyboardHandler.setClipboard(tip.getString());
						return true;
					}
				}
				
				if (this.owner != null && Util.getMillis() - this.lastClick < 1000) {
					GuiUtils.loadSession(FullTextSearchResultScreen.this.minecraft, 
							MessageList.this.currentSessionSummary, 
							FullTextSearchResultScreen.this, 
							this.ordinalInSession);
					return true;
				}
				
				this.lastClick = Util.getMillis();
				return false;
			}

		}
	}
}
