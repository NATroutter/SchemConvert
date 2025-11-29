package fi.natroutter.schemconvert;


import fi.natroutter.foxlib.logger.FoxLogger;
import fi.natroutter.schemconvert.converters.minecraft.LegacyRegistry;
import fi.natroutter.schemconvert.converters.minecraft.schematic.SchematicConverter;
import fi.natroutter.schemconvert.gui.GuiTheme;
import fi.natroutter.schemconvert.gui.dialog.AboutDialog;
import fi.natroutter.schemconvert.gui.dialog.MessageDialog;
import fi.natroutter.schemconvert.gui.main.MainWindow;
import fi.natroutter.schemconvert.mappings.MappingLoader;
import fi.natroutter.schemconvert.storage.StorageProvider;
import imgui.*;
import imgui.app.Application;
import imgui.app.Configuration;
import imgui.flag.*;
import lombok.Getter;
import org.lwjgl.glfw.GLFW;

public class SchemConvert extends Application {

    // Format: month-day-year
    public static String BUILD_DATE = "11-27-2025";
    public static String VERSION = "1.0.0";

    //TODO dump data from schem feature


    @Getter
    private static SchematicConverter schematicConverter;

    @Getter
    private static MappingLoader mappingLoader;

    @Getter
    private static StorageProvider storageProvider;

    @Getter
    private static FoxLogger logger;

    @Getter
    private static LegacyRegistry legacyRegistry;

    @Getter
    private static final MessageDialog messageDialog = new MessageDialog();

    @Getter
    private static final AboutDialog aboutDialog = new AboutDialog();
    private final MainWindow mainWindow = new MainWindow();


    public static void main(final String[] args) {
        logger = new FoxLogger.Builder()
                .setDebug(false)
                .setPruneOlderThanDays(35)
                .setSaveIntervalSeconds(300)
                .setLoggerName("SchemConvert")
                .build();
        legacyRegistry = new LegacyRegistry();
        storageProvider = new StorageProvider();
        schematicConverter = new SchematicConverter();
        mappingLoader = new MappingLoader();

        launch(new SchemConvert());
        System.exit(0);
    }

    @Override
    public void process() {
        mainWindow.render();
        messageDialog.render();
        aboutDialog.render();
    }

    @Override
    protected void configure(final Configuration config) {
        config.setTitle("SchemConvert | v"+VERSION);
        config.setWidth(700);
        config.setHeight(500);
    }

    @Override
    protected void initWindow(final Configuration config) {
        super.initWindow(config);
        GLFW.glfwSetWindowSizeLimits(getHandle(), 400, 300, GLFW.GLFW_DONT_CARE, GLFW.GLFW_DONT_CARE );
    }

    @Override
    protected void initImGui(final Configuration config) {
        super.initImGui(config);

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Saving storage...");
            storageProvider.save();
        }));

        final ImGuiIO io = ImGui.getIO();
        io.setIniFilename(null);
        io.addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard);
//        GuiTheme.apply();
        GuiTheme.applyDarkRuda();
    }
}