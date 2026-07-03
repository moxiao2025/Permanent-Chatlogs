package lovexyn0827.chatlog.gui;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import it.unimi.dsi.fastutil.ints.IntLinkedOpenHashSet;
import lovexyn0827.chatlog.config.Options;
import lovexyn0827.chatlog.i18n.I18N;
import lovexyn0827.chatlog.session.Session;
import lovexyn0827.chatlog.session.Session.Summary;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class SessionListScreen extends Screen {
    private final Screen parent;
	private SessionList displayedSessions;
	private final Predicate<Session.Summary> filterer;
	private final boolean enablePaging = Options.sessionListPaging;
	
	public SessionListScreen(Screen parent) {
		super(I18N.translateAsText("gui.chatlogs"));
        this.parent = parent;
		this.filterer = (s) -> true;
	}
	
	public SessionListScreen(
            Screen parent,
            Predicate<Session.Summary> filterer
    ) {
		super(I18N.translateAsText("gui.filter.result"));
        this.parent = parent;
		this.filterer = filterer;
	}
	
	@Override
	protected void init() {
		this.displayedSessions = new SessionList(this.minecraft);
		this.addRenderableWidget(this.displayedSessions);
		int openBtnYPos = this.height - (this.enablePaging ? 46 : 23);
		Button openBtn = Button.builder(I18N.translateAsText("gui.open"), 
				(btn) -> {
					SessionList.SessionEntry entry = this.displayedSessions.getFocused();
					if (entry != null ) {
						GuiUtils.loadSession(this.minecraft, entry.summary, this);
					}
				})
				.bounds(this.width / 2 - 128, openBtnYPos, 80, 20)
				.build();
		Button exportBtn = Button.builder(I18N.translateAsText("gui.export"), 
				(btn) -> {
					SessionList.SessionEntry entry = this.displayedSessions.getFocused();
					if (entry != null ) {
						this.minecraft.setScreen(new ExportSessionScreen(minecraft.screen, entry.summary));
					}
				})
				.bounds(this.width / 2 - 40, openBtnYPos, 80, 20)
				.build();
		Button deleteBtn = Button.builder(I18N.translateAsText("gui.del"), 
				(btn) -> {
					SessionList.SessionEntry entry = this.displayedSessions.getFocused();
					if (entry != null ) {
						this.minecraft.setScreen(new ConfirmScreen((confirmed) -> {
							if (confirmed) {
								IntLinkedOpenHashSet ids = new IntLinkedOpenHashSet();
								ids.add(entry.summary.id);
								Session.delete(ids);
							}
							
							this.minecraft.setScreen(this);
						}, I18N.translateAsText("gui.del.title"), I18N.translateAsText("gui.del.desc")));
					}
				})
				.bounds(this.width / 2 + 48, openBtnYPos, 80, 20)
				.build();
		Button filterBtn = Button.builder(I18N.translateAsText("gui.filter"), 
						(btn) -> this.minecraft.setScreen(new FilterSessionScreen(minecraft.screen)))
				.bounds(this.width / 2 - 128, 20, 80, 20)
				.build();
		Button settingBtn = Button.builder(I18N.translateAsText("gui.settings"), 
						(btn) -> this.minecraft.setScreen(new SettingScreen(minecraft.screen)))
				.bounds(this.width / 2 - 40, 20, 80, 20)
				.build();
		Button exitBtn = Button.builder(CommonComponents.GUI_BACK, 
						(btn) -> this.minecraft.setScreen(this.parent))
				.bounds(this.width / 2 + 48, 20, 80, 20)
				.build();
		if (this.enablePaging) {
			Button prevBtn = Button.builder(I18N.translateAsText("gui.prev"), 
							(btn) -> this.displayedSessions.turnPage(false))
					.bounds(this.width / 2 - 128, this.height - 23, 124, 20)
					.build();
			Button nextBtn = Button.builder(I18N.translateAsText("gui.next"), 
							(btn) -> this.displayedSessions.turnPage(true))
					.bounds(this.width / 2 + 4, this.height - 23, 124, 20)
					.build();
			this.addRenderableWidget(prevBtn);
			this.addRenderableWidget(nextBtn);
		}
		
		this.addRenderableWidget(openBtn);
		this.addRenderableWidget(exportBtn);
		this.addRenderableWidget(deleteBtn);
		this.addRenderableWidget(filterBtn);
		this.addRenderableWidget(settingBtn);
		this.addRenderableWidget(exitBtn);
	}
	
	@Override
	public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
		this.extractBackground(ctx, mouseY, mouseY, delta);
        ctx.centeredText(
                this.minecraft.font,
                this.title,
                this.width / 2,
                8,
                0xFFFFFF
        );
		super.extractRenderState(ctx, mouseX, mouseY, delta);
	}

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
	
	private final class SessionList extends ObjectSelectionList<SessionList.SessionEntry> {
		private final List<Session.Summary> allSessions = Session.getSessionSummaries()
				.stream()
				.filter(SessionListScreen.this.filterer)
				.sorted((s1, s2) -> (int) Math.signum((double) (s2.startTime - s1.startTime)))
				.collect(Collectors.toList());
		private List<Session.Summary> visibleSessions;
		private int currentPage = 0;
		
		public SessionList(Minecraft mc) {
			super(mc, SessionListScreen.this.width, 
					SessionListScreen.this.height - (SessionListScreen.this.enablePaging ? 104 : 81), 50, 32);
			this.toPage(0);
		}
		
		private void toPage(int i) {
			this.currentPage = i;
			this.visibleSessions = this.sessionsInPage(i);
			this.clearEntries();
			this.visibleSessions.stream().map(SessionEntry::new).forEach(this::addEntry);
			this.setFocused(null);
		}

		private List<Summary> sessionsInPage(int i) {
			if (!SessionListScreen.this.enablePaging) {
				return this.allSessions;
			}
			
			int itemPerPage = Options.sessionsPerPage;
			int pageStart = i * itemPerPage;
			int pageEnd = Math.min(i * itemPerPage + itemPerPage - 1, this.allSessions.size());
			return this.allSessions.subList(pageStart, pageEnd);
		}

		public void turnPage(boolean next) {
			int target = this.currentPage + (next ? 1 : -1);
			int totalPages = (int) Math.ceil(((double) this.allSessions.size()) / Options.sessionsPerPage);
			this.toPage(Mth.clamp(target, 0, totalPages - 1));
		}
		
		@Override
		public void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput builder) {
		}
		
		private final class SessionEntry extends ObjectSelectionList.Entry<SessionEntry> {
			private final Session.Summary summary;
			private final Component saveName;
			private final Component start;
			private final Component sizeAndTimeLength;
			
			public SessionEntry(Session.Summary info) {
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
			public Component getNarration() {
				return this.saveName;
			}
			
			@Override
			public void extractContent(GuiGraphicsExtractor ctx, int mouseX, int mouseY, 
					boolean hovering, float partialTick) {
				int x = this.getContentX();
				int y = this.getContentY();
				Font tr = SessionListScreen.this.minecraft.font;
				ctx.text(tr, this.saveName, x, y, 0xFFFFFFFF);
				ctx.text(tr, this.start, x, y + 10, 0xFFFFFFFF);
				ctx.text(tr, this.sizeAndTimeLength, x, y + 20, 0xFFFFFFFF);
			}
			
			@Override
			public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent mouseEvent, boolean doubleClick) {
				SessionList.this.setFocused(this);
				return true;
			}
		}
	}
}
