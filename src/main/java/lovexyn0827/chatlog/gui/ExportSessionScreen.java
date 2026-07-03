package lovexyn0827.chatlog.gui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.Charset;

import lovexyn0827.chatlog.export.ExportConfig;
import lovexyn0827.chatlog.export.FormatAdapter;
import lovexyn0827.chatlog.i18n.I18N;
import lovexyn0827.chatlog.session.Session;
import lovexyn0827.chatlog.session.Session.Summary;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.util.Util;

public class ExportSessionScreen extends Screen {
    private final Screen parent;
	private static final File EXPORT_FOLDER = Util.make(() -> {
		File f = new File("chatlogs/export");
		if (f.isDirectory()) {
			return f;
		}
		
		if (!f.mkdir()) {
			return null;
		} else {
			return f;
		}
	});
	private EditBox fileName;
	private CycleButton<FormatAdapter.Factory<?>> format;
	private CycleButton<Boolean> openAfterExport;
	private final Summary sessionMeta;
	
	protected ExportSessionScreen(Screen parent,Summary summary) {
		super(I18N.translateAsText("gui.export"));
        this.parent = parent;
		this.sessionMeta = summary;
	}
	
	@Override
	protected void init() {
		this.fileName = new EditBox(this.font, 
				(int) (width * 0.3F), (int) (height * 0.25F), 
				(int) (width * 0.4F), 14, 
				I18N.translateAsText("gui.export.name"));
		this.fileName.setValue(Util.getFilenameFormattedDateTime());
		this.format = CycleButton.<FormatAdapter.Factory<?>>builder(FormatAdapter.Factory::getDisplayedText, FormatAdapter.FORMAT_FACTORIES.get(0))
				.withValues(FormatAdapter.FORMAT_FACTORIES)
				.create((int) (width * 0.3F), (int) (height * 0.25F) + 25, 
						(int) (width * 0.4F), 20, I18N.translateAsText("gui.export.format"));
		this.openAfterExport = CycleButton.onOffBuilder(false)
				.create((int) (width * 0.3F), (int) (height * 0.25F) + 50, 
						(int) (width * 0.4F), 20, I18N.translateAsText("gui.export.open"));
		this.addRenderableWidget(this.fileName);
		this.addRenderableWidget(this.format);
		this.addRenderableWidget(this.openAfterExport);
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (btn) -> this.export())
				.bounds((int) (width * 0.3F), (int) (height * 0.25F) + 75, 
						(int) (width * 0.19F), 20)
				.build());
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, (btn) -> this.onClose())
				.bounds((int) (width * 0.51F), (int) (height * 0.25F) + 75, 
						(int) (width * 0.19F), 20)
				.build());
	}

	private void export() {
		if (EXPORT_FOLDER == null) {
			SystemToast warning = new SystemToast(new SystemToast.SystemToastId(), 
					I18N.translateAsText("gui.export.nodir"), 
					I18N.translateAsText("gui.export.nodir.desc"));
			Minecraft.getInstance().getToastManager().addToast(warning);
			return;
		}
		
		String extension = this.format.getValue().getExtension();
		Session session = this.sessionMeta.load();
		if (session == null) {
			SystemToast warning = new SystemToast(new SystemToast.SystemToastId(), 
					I18N.translateAsText("gui.sload.failure"), 
					I18N.translateAsText("gui.sload.failure.desc"));
			Minecraft.getInstance().getToastManager().addToast(warning);
		}
		
		File target = new File(EXPORT_FOLDER, this.fileName.getValue() + "." + extension);
		try (BufferedWriter w = new BufferedWriter(new FileWriter(target, Charset.forName("UTF-8")))) {
			FormatAdapter fmt = this.format.getValue().create(
					w, this.sessionMeta, session, new ExportConfig(false, true));
			fmt.write();
		} catch (Exception e) {
			e.printStackTrace();
			SystemToast warning = new SystemToast(new SystemToast.SystemToastId(), 
					I18N.translateAsText("gui.export.fail"), 
					I18N.translateAsText("gui.export.fail.desc"));
			Minecraft.getInstance().getToastManager().addToast(warning);
			return;
		}
		
		if (this.openAfterExport.getValue()) {
			Util.getPlatform().openFile(target);
		}
		
		this.onClose();
	}
	
	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}
}
