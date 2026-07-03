package lovexyn0827.chatlog.gui;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.lwjgl.glfw.GLFW;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lovexyn0827.chatlog.i18n.I18N;
import lovexyn0827.chatlog.mixin.TextFieldWidgetAccessor;
import lovexyn0827.chatlog.session.Session;
import lovexyn0827.chatlog.session.Session.Summary;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.advancements.criterion.MinMaxBounds.Ints;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public final class FilterSessionScreen extends Screen {
    private final Screen parent;
	private final List<Session.Summary> cachedSessions = Session.getSessionSummaries();
	private Predicate<Session.Summary> filterer;
	private EditBox saveName;
	private EditBox date;
	private EditBox size;
	private EditBox seconds;
	private EditBox contents;
	private Checkbox caseSenstive;
	private CycleButton<Scope> scopeBtn;
	
	protected FilterSessionScreen(Screen parent) {
		super(I18N.translateAsText("gui.filter.sessions"));
        this.parent = parent;
	}

	@Override
	public void onClose() {
		this.filterer = (s) -> {
			return s.saveName.contains(this.saveName.getValue());
		};
		selectDate:
		try {
			String dateStr = this.date.getValue();
			int[] dateComps = Arrays.stream(dateStr.split("[^0-9]"))
					.filter((s) -> s.matches("^\\d+$"))
					.mapToInt(Integer::parseInt)
					.toArray();
			if(dateComps.length > 0 && dateComps.length <= 3) {
				int y;
				int m;
				int d;
				switch(dateComps.length) {
				case 1:
					y = dateComps[0];
					m = 0;
					d = 0;
					break;
				case 2:
					y = dateComps[0];
					m = dateComps[1];
					d = 0;
					break;
				case 3:
					y = dateComps[0];
					m = dateComps[1];
					d = dateComps[2];
					break;
				default:
					break selectDate;
				}
				
				this.filterer = this.filterer.and((s) -> {
					ZonedDateTime start = ZonedDateTime.ofInstant(
							Instant.ofEpochMilli(s.startTime), s.timeZone.toZoneId());
					return start.getYear() == y
							&& (m == 0 || start.getMonthValue() == m)
							&& (d == 0 || start.getDayOfMonth() == d);
				});
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			Ints sizeRange = Ints.fromReader(new StringReader(this.size.getValue()));
			this.filterer = this.filterer.and((s) -> sizeRange.matches((int) s.size));
		} catch (CommandSyntaxException e) {
		}
		
		try {
			Ints sizeRange = Ints.fromReader(new StringReader(this.seconds.getValue()));
			this.filterer = this.filterer.and((s) -> sizeRange.matches((int) ((s.endTime - s.startTime) / 1000)));
		} catch (CommandSyntaxException e) {
		}
		
		if (!this.contents.getValue().isEmpty()) {
			this.minecraft.setScreen(new FullTextSearchProgressScreen(
					this.parent, this.filterer, this.contents.getValue(), this.caseSenstive.selected()));
		} else {
			this.filterer = this.filterer.and((s) -> this.scopeBtn.getValue().test(s));
			this.minecraft.setScreen(new SessionListScreen(this.parent,this.filterer));
		}
	}
	
	@Override
	public void init() {
		int width = this.minecraft.getWindow().getGuiScaledWidth();
		int height = this.minecraft.getWindow().getGuiScaledHeight();
		this.saveName = new TextFieldWithAutoCompletionWidget(this.font, 
				(int) (width * 0.35F), (int) (height * 0.25F), 
				(int) (width * 0.4F), 14, 
				I18N.translateAsText("gui.filter.savename"));
		this.saveName.setResponder((in) -> {
			this.saveName.setSuggestion(this.cachedSessions.stream()
					.map((s) -> s.saveName)
					.filter((n) -> n.startsWith(this.saveName.getValue()))
					.collect(Object2IntOpenHashMap<String>::new, (m, s) -> m.put(s, 0), (m1, m2) -> m1.putAll(m2))
					.object2IntEntrySet()
					.stream()
					.max(Comparator.comparing(Object2IntMap.Entry::getIntValue))
					.map(Map.Entry::getKey)
					.filter((n) -> n.length() > this.saveName.getValue().length())
					.map((n) -> n.substring(this.saveName.getValue().length()))
					.orElse(null));
		});
		this.date = new EditBox(this.font, 
				(int) (width * 0.35F), (int) (height * 0.25F) + 18, 
				(int) (width * 0.4F), 14, 
				I18N.translateAsText("gui.filter.date"));
		this.date.setHint(Component.literal("YYYY-MM-DD").withStyle(ChatFormatting.GRAY));
		this.size = new EditBox(this.font, 
				(int) (width * 0.35F), (int) (height * 0.25F) + 36, 
				(int) (width * 0.4F), 14, 
				I18N.translateAsText("gui.filter.messages"));
		this.seconds = new EditBox(this.font, 
				(int) (width * 0.35F), (int) (height * 0.25F) + 54, 
				(int) (width * 0.4F), 14, 
				I18N.translateAsText("gui.filter.seconds"));
		this.contents = new EditBox(this.font, 
				(int) (width * 0.35F), (int) (height * 0.25F) + 72, 
				(int) (width * 0.4F), 14, 
				I18N.translateAsText("gui.filter.fulltext"));
		this.caseSenstive = Checkbox.builder(Component.empty(), this.font)
				.selected(false)
				.pos((int) (width * 0.75F) - 18, (int) (height * 0.25F) + 90)
				.build();
		this.scopeBtn = CycleButton.<Scope>builder(Scope::getText, Scope.ALL)
				.withValues(Scope.values())
				.create((int) (width * 0.35F), (int) (height * 0.25F) + 108, (int) (width * 0.4F), 20, 
						Component.translatable("advMode.type"));
		this.addRenderableWidget(this.saveName);
		this.addRenderableWidget(this.date);
		this.addRenderableWidget(this.size);
		this.addRenderableWidget(this.seconds);
		this.addRenderableWidget(this.contents);
		this.addRenderableWidget(this.caseSenstive);
		this.addRenderableWidget(this.scopeBtn);
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (btn) -> this.onClose())
				.bounds(width / 2 - 40, (int) (height * 0.25F) + 132, 80, 20)
				.build());

	}
	
	@Override
	public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
		this.extractBackground(ctx, mouseY, mouseY, delta);
		int width = this.minecraft.getWindow().getGuiScaledWidth();
		int height = this.minecraft.getWindow().getGuiScaledHeight();
		ctx.centeredText(this.font, I18N.translateAsText("gui.filter.savename"), 
				(int) (width * 0.27F), (int) (height * 0.25F) + 2, 0xFFFFFFFF);
		ctx.centeredText(this.font, I18N.translateAsText("gui.filter.date"), 
				(int) (width * 0.27F), (int) (height * 0.25F) + 20, 0xFFFFFFFF);
		ctx.centeredText(this.font, I18N.translateAsText("gui.filter.messages"), 
				(int) (width * 0.27F), (int) (height * 0.25F) + 38, 0xFFFFFFFF);
		ctx.centeredText(this.font, I18N.translateAsText("gui.filter.seconds"), 
				(int) (width * 0.27F), (int) (height * 0.25F) + 56, 0xFFFFFFFF);
		ctx.centeredText(this.font, I18N.translateAsText("gui.filter.fulltext"), 
				(int) (width * 0.27F), (int) (height * 0.25F) + 74, 0xFFFFFFFF);
		ctx.centeredText(this.font, I18N.translateAsText("gui.filter.case"), 
				(int) (width * 0.27F), (int) (height * 0.25F) + 92, 0xFFFFFFFF);
		ctx.centeredText(this.font, Component.translatable("advMode.type"), 
				(int) (width * 0.27F), (int) (height * 0.25F) + 113, 0xFFFFFFFF);
		super.extractRenderState(ctx, mouseX, mouseY, delta);
	}
	
	private static enum Scope implements Predicate<Session.Summary> {
		SINGLE_PLAYER(Component.translatable("menu.singleplayer")) {
			@Override
			public boolean test(Summary s) {
				return !s.multiplayer;
			}
		}, 
		MULTI_PLAYER(Component.translatable("menu.multiplayer")) {
			@Override
			public boolean test(Summary s) {
				return s.multiplayer;
			}
		}, 
		ALL(Component.translatable("gui.all")) {
			@Override
			public boolean test(Summary s) {
				return true;
			}
		};
		
		private final Component text;

		private Scope(Component text) {
			this.text = text;
		}
		
		private final Component getText() {
			return this.text;
		}

		@Override
		public abstract boolean test(Summary t);
	}
	
	private static class TextFieldWithAutoCompletionWidget extends EditBox {
		public TextFieldWithAutoCompletionWidget(Font font, int x, int y, int width, 
				int height, Component text) {
			super(font, x, y, width, height, text);
		}
		
		@Override
		public boolean keyPressed(KeyEvent keyEvent) {
			if (keyEvent.key() == GLFW.GLFW_KEY_TAB && ((TextFieldWidgetAccessor) this).getSuggestion() != null) {
				this.setValue(this.getValue().concat(((TextFieldWidgetAccessor) this).getSuggestion()));
				return true;
			} else {
				return super.keyPressed(keyEvent);
			}
		}
	}
}
