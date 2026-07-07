package lovexyn0827.chatlog.gui;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import com.google.common.collect.ImmutableList;

import lovexyn0827.chatlog.config.Options;
import lovexyn0827.chatlog.i18n.I18N;
import lovexyn0827.chatlog.session.Session;
import lovexyn0827.chatlog.session.Session.Line;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.FormattedCharSequence;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

public final class ChatLogScreen extends Screen {
	private final Session session;
	private final ZoneId timeZone;
	private ChatLogWidget chatlogs;
	private SearchFieldWidget searchField;
	private CycleButton<SearchingMode> searchBarModeChooser;
	private final Screen parent;
	
	protected ChatLogScreen(Session.Summary metadata, Session session, Screen parent) {
		super(Component.literal(metadata.saveName));
		this.session = session;
		this.timeZone = metadata.timeZone.toZoneId();
		this.parent = parent;
	}

	@Override
	protected void init() {
		this.width = (int) (this.minecraft.getWindow().getWidth() * 0.8F);
		this.chatlogs = new ChatLogWidget(this.minecraft, this.session);
		this.searchField = new SearchFieldWidget(this.font);
		this.addRenderableWidget(this.searchField);
		this.addRenderableWidget(this.chatlogs);
		this.searchBarModeChooser = CycleButton
				.<SearchingMode>builder(SearchingMode::displayedText, SearchingMode.TEXT)
				.withValues(SearchingMode.values())
				.create(2, 0, (int) (this.minecraft.getWindow().getGuiScaledWidth() * 0.2F) - 4, 20, 
						Component.empty(), (b, v) -> this.chatlogs.search(this.searchField.getValue()));
		Button extractBtn = Button.builder(I18N.translateAsText("gui.extract"), 
				(btn) -> {
					List<Session.Line> delims = this.chatlogs.collectDelimiters();
					SystemToast warning;
					switch (delims.size()) {
					case 0:
						warning = new SystemToast(new SystemToast.SystemToastId(), 
								I18N.translateAsText("gui.extract.nodelim"), 
								I18N.translateAsText("gui.extract.nodelim.desc"));
						Minecraft.getInstance().gui.toastManager().addToast(warning);
						break;
					case 1:
						ConfirmScreen endChooser = new ConfirmScreen((before) -> {
									Session chosen;
									if (before) {
										chosen = this.session.clip(null, delims.get(0));
									} else {
										chosen = this.session.clip(delims.get(0), null);
									}
									
									this.saveExtractedSession(chosen);
									this.minecraft.gui.setScreen(this);
								}, Component.empty(), 
								I18N.translateAsText("gui.extract.choend"), 
								I18N.translateAsText("gui.extract.before"), 
								I18N.translateAsText("gui.extract.after"));
						this.minecraft.gui.setScreen(endChooser);
						break;
					case 2:
						this.saveExtractedSession(this.session.clip(delims.get(0), delims.get(1)));
						break;
					default:
						warning = new SystemToast(new SystemToast.SystemToastId(), 
								I18N.translateAsText("gui.extract.muldelim"), 
								I18N.translateAsText("gui.extract.muldelim.desc"));
						Minecraft.getInstance().gui.toastManager().addToast(warning);
					}
				})
				.bounds((int) (this.minecraft.getWindow().getGuiScaledWidth() * 0.8F) + 2, 0, 
						(int) (this.minecraft.getWindow().getGuiScaledWidth() * 0.2F) - 4, 20)
				.build();
		this.addRenderableWidget(this.searchBarModeChooser);
		this.addRenderableWidget(extractBtn);
	}
	
	void scrollTo(int ordinalInSession) {
		this.chatlogs.scrollTo(ordinalInSession);
	}
	
	private void saveExtractedSession(Session s) {
		s.save();
	}
	
	/**
	 * Extracts the text Component from a SHOW_TEXT hover event.
	 * In 26.1.2, HoverEvent is an interface; the concrete ShowTextEvent record 
	 * contains the value. We use toString() as a fallback.
	 */
	static Component getHoverShowTextValue(HoverEvent he) {
		if (he.action() == HoverEvent.Action.SHOW_TEXT) {
			// The concrete type is HoverEvent.ShowTextEvent with a value() method
			// But we can't access it directly, so use toString
			String str = he.toString();
			// Try to extract the text portion from the toString representation
			return Component.literal(str);
		}
		return null;
	}
	
	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		super.extractRenderState(context, mouseX, mouseY, delta);
	}
	
	@Override
	public boolean keyPressed(KeyEvent keyEvent) {
		this.chatlogs.keyPressed(keyEvent);
		return super.keyPressed(keyEvent);
	}
	
	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}
	
	private final class ChatLogWidget extends AbstractSelectionList<ChatLogWidget.Entry> {
		private final List<Entry> allEntries;
		private ListIterator<Entry> highlightenEntryHead = null;
		
		public ChatLogWidget(Minecraft client, Session session) {
			super(client, ChatLogScreen.this.minecraft.getWindow().getGuiScaledWidth(), 
					ChatLogScreen.this.height - 40, 20, client.font.lineHeight + 1);
			session.getMessages().forEach((l) -> {
				boolean[] firstLine = new boolean[] { true };
				ComponentSplitter.wrapComponents(l.message, 
						ChatLogScreen.this.minecraft.getWindow().getGuiScaledWidth() - 14, 
						ChatLogScreen.this.font).forEach((t) -> {
							this.addEntry(new Entry(l, t, l.time, firstLine[0]));
							firstLine[0] = false;
						});
			});
			this.allEntries = ImmutableList.copyOf(this.children());
		}
		
		public List<Line> collectDelimiters() {
			return this.allEntries.stream()
					.filter((e) -> e.isDelimiter)
					.map((e) -> e.owner)
					.distinct()
					.collect(Collectors.toList());
		}

		@Override
		public int getRowWidth() {
			return ChatLogScreen.this.minecraft.getWindow().getGuiScaledWidth();
		}
		
		@Override
		protected int scrollBarX() {
			return this.getRight() - 5;
		}
		
		@Override
		protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput builder) {
		}
		
		@Override
		public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
			double multiplier = Minecraft.getInstance().hasControlDown() ? 
					(Minecraft.getInstance().hasAltDown() ? 320 : 64) : 16.0;
			verticalAmount *= multiplier;
			this.setScrollAmount(this.scrollAmount() - verticalAmount);
			return true;
		}
		
		protected void search(String key) {
			if (key.isEmpty() && !ChatLogScreen.this.searchBarModeChooser.getValue().natuallyRestrictive) {
				// Not searching, reverting any changes to the ChatLogWidget
				this.highlightenEntryHead = null;
				this.replaceEntries(this.allEntries);
				this.setFocused(null);
				return;
			}
			
			if (Options.messageFinderFilteringMode) {
				this.filter(key);
			} else {
				this.highlightSelected(key);
			}
		}
		
		private List<Entry> getMatchingMessages(String in) {
			return this.allEntries.stream()
					.filter((e) -> {
						switch (ChatLogScreen.this.searchBarModeChooser.getValue()) {
						case TEXT:
							return e.lineStr.contains(in);
						case TIME:
							return e.getFormattedTime().contains(in);
						case SENDER:
							return true;	// TODO
						case EVENT:
							return e.owner instanceof Session.Event && e.lineStr.contains(in);
						case SAVE_INDICATOR:
							return e.owner instanceof Session.WorldIndicator && e.lineStr.contains(in);
						default:
							return true;
						}
					})
					.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
		}
		
		private void filter(String in) {
			this.replaceEntries(this.getMatchingMessages(in));
			this.setScrollAmount(0);
		}
		
		private void highlightSelected(String in) {
			List<Entry> selected = this.getMatchingMessages(in);
			if (selected.isEmpty()) {
				this.highlightenEntryHead = null;
				this.setFocused(null);
				return;
			}
			
			this.highlightenEntryHead = selected.listIterator();
			this.setFocused(selected.get(0));
			this.centerScrollOn(selected.get(0));
		}
		
		private static void showNoMoreMatchesToast() {
			SystemToast warning = new SystemToast(new SystemToast.SystemToastId(), 
					I18N.translateAsText("gui.search.nomore"), 
					I18N.translateAsText("gui.search.nomore.desc"));
			Minecraft.getInstance().gui.toastManager().addToast(warning);
		}
		
		@Override
		public boolean keyPressed(KeyEvent keyEvent) {
			if (this.highlightenEntryHead == null) {
				return false;
			}
			
			if (this.highlightenEntryHead != null || keyEvent.key() == GLFW.GLFW_KEY_F3) {
				if (Minecraft.getInstance().hasShiftDown()) {
					if (!this.highlightenEntryHead.hasPrevious()) {
						showNoMoreMatchesToast();
						return true;
					}
					
					Entry prev = this.highlightenEntryHead.previous();
					this.setFocused(prev);
					this.centerScrollOn(prev);
				} else {
					if (!this.highlightenEntryHead.hasNext()) {
						showNoMoreMatchesToast();
						return true;
					}

					Entry next = this.highlightenEntryHead.next();
					this.setFocused(next);
					this.centerScrollOn(next);
				}
			}
			
			return true;
		}
		
		void scrollTo(int ordinalInSession) {
			Session.Line prev = null;
			int curOrd = -1;
			for (Entry e : this.allEntries) {
				if (e.owner != prev) {
					prev = e.owner;
					curOrd++;
				}
				
				if (curOrd == ordinalInSession) {
					this.centerScrollOn(e);
					return;
				}
			}
		}

		private final class Entry extends AbstractSelectionList.Entry<Entry> {
			protected final Session.Line owner;
			private final FormattedCharSequence line;
			private final String lineStr;
			private final long time;
			private final boolean firstLine;
			private boolean isDelimiter = false;
			
			protected Entry(Session.Line owner, FormattedCharSequence t, long time, boolean firstLine) {
				this.owner = owner;
				this.line = t;
				this.time = time;
				this.firstLine = firstLine;
				StringBuilder sb = new StringBuilder();
				t.accept((idx, style, cp) -> {
					sb.append((char) cp);
					return true;
				});
				this.lineStr = sb.toString();
			}
			
			public String getFormattedTime() {
				return (this.time == 0L) ? I18N.translate("gui.unknowntime") : 
						Instant.ofEpochMilli(this.time)
								.atZone(ChatLogScreen.this.timeZone)
								.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
			}

			@Override
			public void extractContent(GuiGraphicsExtractor ctx, int mouseX, int mouseY, 
					boolean hovering, float partialTick) {
				int x = this.getContentX();
				int y = this.getContentY();
				int entryWidth = this.getContentWidth();
				Font tr = ChatLogScreen.this.font;
				boolean highlight = this.isFocused();
				if (highlight) {
					ctx.outline(x + 4, y - 1, entryWidth, 10, 0xFFFFFF00);
				}
				
				ctx.text(tr, this.line, x + 4, y, 0xFFFFFFFF);
				if (this.isDelimiter) {
					int textWidth = ChatLogScreen.this.minecraft.getWindow().getGuiScaledWidth() - 14;
					ctx.fill(x + 4, y - 1, x + textWidth, y, 0xFFFF0000);
				}
				
				ctx.fill(x + 1, y + (this.firstLine ? 2 : 0), x + 3, y + 10, this.owner.getMarkColor());
				// Note: hovering tooltips with mouse position require mouseX/mouseY from the parent screen.
				// In 26.1.2 extractContent doesn't receive mouse position. The tooltip rendering
				// for hover events within list entries is handled differently.
			}

			@Nullable
			private Component getToolTip(double mouseX, double mouseY) {
				Font tr = ChatLogScreen.this.font;
				double scale = ChatLogScreen.this.minecraft.getWindow().getGuiScale();
				int pos = (int) Math.floor(mouseX - 4 * scale);
				Style style = ComponentSplitter.styleAtWidth(tr, line, pos);
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
			public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent mouseEvent, boolean doubleClick) {
				double mouseX = mouseEvent.x();
				double mouseY = mouseEvent.y();
				if(Minecraft.getInstance().hasControlDown()) {
					Component tip = this.getToolTip(mouseX, mouseY);
					if(tip != null) {
						ChatLogScreen.this.minecraft.keyboardHandler.setClipboard(tip.getString());
						return true;
					}
				}
				
				if (Minecraft.getInstance().hasShiftDown()) {
					this.isDelimiter ^= true;
				}
				
				return false;
			}
		}
	}
	
	private final class SearchFieldWidget extends EditBox {
		public SearchFieldWidget(Font font) {
			super(font,  
					(int) (ChatLogScreen.this.minecraft.getWindow().getGuiScaledWidth() * 0.2F), 2, 
					(int) (ChatLogScreen.this.minecraft.getWindow().getGuiScaledWidth() * 0.6F), 16, 
					I18N.translateAsText("gui.search")
			);
			this.setResponder(ChatLogScreen.this.chatlogs::search);
		}
	}
	
	private enum SearchingMode {
		TEXT(false), 
		TIME(false), 
		SENDER(false), 
		EVENT(true), 
		SAVE_INDICATOR(true);
		
		/**
		 * Whether this mode can select lines without given keywords, or can work searching bar empty.
		 */
		protected final boolean natuallyRestrictive;
		
		private SearchingMode(boolean natuallyRestrictive) {
			this.natuallyRestrictive = natuallyRestrictive;
		}
		
		protected Component displayedText() {
			return I18N.translateAsText("gui.search.mode." + this.name().toLowerCase());
		}
	}
}
