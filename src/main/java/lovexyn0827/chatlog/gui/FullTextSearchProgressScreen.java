package lovexyn0827.chatlog.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import com.mojang.datafixers.util.Pair;

import lovexyn0827.chatlog.i18n.I18N;
import lovexyn0827.chatlog.session.Session;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;

public class FullTextSearchProgressScreen extends Screen {
	private final Screen parent;
	private final Predicate<Session.Summary> metadataCriteria;
	private final String msgKeyword;
	private final boolean caseSensitive;
	private final int total = Session.getSessionSummaries().size();
	private final AtomicInteger doneCount = new AtomicInteger();
	private final ConcurrentHashMap<Session.Summary, List<Pair<Integer, Session.Line>>> results = new ConcurrentHashMap<>();
	
	protected FullTextSearchProgressScreen(Screen parent, Predicate<Session.Summary> criteria, 
			String msgKeyword, boolean caseSensitive) {
		super(I18N.translateAsText("gui.filter.progress"));
		this.parent = parent;
		this.metadataCriteria = criteria;
		this.msgKeyword = msgKeyword;
		this.caseSensitive = caseSensitive;
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}

	@Override
	public void init() {
		super.init();
		this.doWork();
	}
	
	@Override
	public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
		super.extractRenderState(ctx, mouseX, mouseY, delta);
		ctx.centeredText(this.font, I18N.translate("gui.filter.progress"), 
				this.width / 2, (int) (this.height * 0.4), 0xFFFFFFFF);
		this.drawProgressBar(ctx, (int) (this.height * 0.4) + 15, this.doneCount.get(), this.total);
		if (this.doneCount.get() == this.total) {
			this.minecraft.gui.setScreen(new ConfirmScreen(this::showSearchResults, 
					I18N.translateAsText("gui.filter.showmode"), 
					I18N.translateAsText("gui.filter.showmode.desc"),  
					I18N.translateAsText("gui.filter.showmode.message"), 
					I18N.translateAsText("gui.filter.showmode.session")));
		}
	}
	
	private void drawProgressBar(GuiGraphicsExtractor ctx, int y, int done, int total) {
		int barWidth = (int) (this.width * 0.6);
		int doneWidth = barWidth * done / total;
		int x = (this.width - barWidth) / 2;
		ctx.fill(x - 1, y - 1, x + barWidth + 1, y + 17, 0xFF7F7F7F);
		ctx.fill(x, y, x + doneWidth, y + 16, 0xFF00FF00);
		ctx.centeredText(this.font, String.format("%d / %d", done, total), 
				this.width / 2, y + 20, 0xFFFFFFFF);
	}
	
	private void showSearchResults(boolean showMessage) {
		if (showMessage) {
			this.minecraft.gui.setScreen(new FullTextSearchResultScreen(this.parent,this.results));
		} else {
			this.minecraft.gui.setScreen(new SessionListScreen(this.parent,this.results::containsKey));
		}
	}
	
	private void doWork() {
		Session.getSessionSummaries()
				.stream()
				.sorted((s0, s1) -> (int) (s1.size - s0.size))	// Larger first
				.forEach((s) -> {
					int[] currentOrd = new int[] { 0 };
					ForkJoinPool.commonPool().execute(() -> {
						try {
							if (!this.metadataCriteria.test(s)) {
								this.doneCount.incrementAndGet();
								return;
							}
							
							Session session = s.load();
							if (session == null) {
								this.doneCount.incrementAndGet();
								return;
							}

							if (this.caseSensitive) {
								String key = this.msgKeyword;
								for (Session.Line l : session.getMessages()) {
									if (l.message.getString().contains(key)) {
										Pair<Integer, Session.Line> item = new Pair<>(currentOrd[0], l);
										this.results.computeIfAbsent(s, (unused) -> new ArrayList<>()).add(item);
									}
								}
							} else {
								String keyUpper = this.msgKeyword.toUpperCase();
								for (Session.Line l : session.getMessages()) {
									if (l.message.getString().toUpperCase().contains(keyUpper)) {
										Pair<Integer, Session.Line> item = new Pair<>(currentOrd[0], l);
										this.results.computeIfAbsent(s, (unused) -> new ArrayList<>()).add(item);
									}
									
									currentOrd[0]++;
								}
							}
							
						} catch (Throwable e) {
							e.printStackTrace();
						} finally {
							this.doneCount.incrementAndGet();
						}
					});
				});
	}
}
