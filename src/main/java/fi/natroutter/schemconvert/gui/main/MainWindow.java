package fi.natroutter.schemconvert.gui.main;

import com.sk89q.worldedit.util.formatting.component.MessageBox;
import fi.natroutter.foxlib.FoxLib;
import fi.natroutter.foxlib.files.FileUtils;
import fi.natroutter.foxlib.files.WriteResponse;
import fi.natroutter.foxlib.logger.FoxLogger;
import fi.natroutter.schemconvert.SchemConvert;
import fi.natroutter.schemconvert.converters.ConversionResult;
import fi.natroutter.schemconvert.converters.hytale.prefab.HytalePrefab;
import fi.natroutter.schemconvert.converters.minecraft.schematic.SchematicConverter;
import fi.natroutter.schemconvert.gui.dialog.DialogButton;
import fi.natroutter.schemconvert.gui.dialog.MessageDialog;
import fi.natroutter.schemconvert.mappings.Mapping;
import fi.natroutter.schemconvert.mappings.MappingLoader;
import fi.natroutter.schemconvert.storage.DataStore;
import fi.natroutter.schemconvert.storage.StorageProvider;
import fi.natroutter.schemconvert.utilities.Utils;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MainWindow {

    private FoxLogger logger = SchemConvert.getLogger();
    private MappingLoader mappingLoader = SchemConvert.getMappingLoader();
    private SchematicConverter schematicConverter = SchemConvert.getSchematicConverter();
    private StorageProvider storage = SchemConvert.getStorageProvider();

    private final ImString inputPath = new ImString();
    private final ImString outputPath = new ImString();
    private final ImBoolean directoryMode = new ImBoolean(false);
    private final ImInt selectedMapping = new ImInt(0);

    private MainMenuBar menuBar = new MainMenuBar();
    private MessageDialog messageDialog = SchemConvert.getMessageDialog();

    private List<File> input_files = new ArrayList<>();
    private File output_dir;

    public MainWindow() {
        loadData();
    }

    public void render() {
        ImGui.setNextWindowPos(0, 0, ImGuiCond.Always);
        ImGui.setNextWindowSize(ImGui.getIO().getDisplaySizeX(), ImGui.getIO().getDisplaySizeY());

        int windowFlags =
                ImGuiWindowFlags.NoDocking |
                ImGuiWindowFlags.NoTitleBar |
                ImGuiWindowFlags.NoCollapse |
                ImGuiWindowFlags.NoResize |
                ImGuiWindowFlags.NoMove |
                ImGuiWindowFlags.NoBringToFrontOnFocus |
                ImGuiWindowFlags.MenuBar |
                ImGuiWindowFlags.NoNavFocus;

        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0.0f);

        if (ImGui.begin("SchemConvert", windowFlags)) {

            menuBar.render();

            ImVec2 buttonSize = ImGui.calcTextSize("...");
            float buttonWidth = buttonSize.x + ImGui.getStyle().getFramePaddingX() * 2;
            float spacing = ImGui.getStyle().getItemSpacingX();

            ImGui.text("Schematic " + (directoryMode.get() ? "directory" : "file"));
            ImGui.setNextItemWidth(ImGui.getContentRegionAvailX() - buttonWidth - spacing);
            ImGui.inputText("##schematic", inputPath, ImGuiInputTextFlags.CallbackResize | ImGuiInputTextFlags.ReadOnly);
            ImGui.sameLine();
            if (ImGui.button("...##schematic")) {
                selectInput();
            }

            ImGui.text("Output Directory");
            ImGui.setNextItemWidth(ImGui.getContentRegionAvailX() - buttonWidth - spacing);
            ImGui.inputText("##output", outputPath, ImGuiInputTextFlags.CallbackResize | ImGuiInputTextFlags.ReadOnly);
            ImGui.sameLine();
            if (ImGui.button("...##output")) {
                selectOutput();
            }

            ImGui.text("Mappings");
            ImGui.setNextItemWidth(ImGui.getContentRegionAvailX());
            if (ImGui.combo("##Mappings", selectedMapping, mappingLoader.names(), ImGuiInputTextFlags.CallbackResize)) {
                String name = mappingLoader.getNameByIndex(selectedMapping.get());
                if (name != null) {
                    storage.getData().setMapping(name.replace(".json", ""));
                    storage.save();
                }
            }

            ImGui.separator();
            ImGui.text("Options");
            if (ImGui.checkbox("Directory mode", directoryMode)) {
                storage.getData().setDirectoryMode(directoryMode.get());
                storage.getData().setInputPath(new ArrayList<>());
                inputPath.set("");
                input_files = new ArrayList<>();
                storage.save();
            }

            ImGui.separator();

            if (ImGui.button("Dump Data", new ImVec2(ImGui.getContentRegionAvailX(), 20))) {
                if (dump()) {
                    logger.info("File data dumped successfully!");
                    messageDialog.show("SchemConvert","File data dumped!", List.of(
                            new DialogButton("OK"),
                            new DialogButton("Open", ()-> {
                                logger.info("Opening file...");
                                try {
                                    FileUtils.openFileExplorer(Path.of(System.getProperty("user.dir"), "dumps").toFile());
                                } catch (IOException e) {
                                    logger.error("Failed to open file explorer : " + e.getMessage());
                                }
                            })
                    ));
                } else {
                    logger.error("File data dumped failed!");
                    messageDialog.show("SchemConvert", (directoryMode.get() ? "Files" : "file") + " dumping failed!");
                }
            }

            if (ImGui.button("Convert", new ImVec2(ImGui.getContentRegionAvailX(), 20))) {
                if (convert()) {
                    messageDialog.show("SchemConvert", (directoryMode.get() ? "Files" : "file") + " converted successfully!");
                } else {
                    messageDialog.show("SchemConvert", (directoryMode.get() ? "Files" : "file") + " converted failed!");
                }
            }

        }
        ImGui.end();
        ImGui.popStyleVar(1);
    }

    private void loadData() {
        DataStore data = storage.getData();
        if (!data.getInputPath().isEmpty()) {
            if (storage.getData().isDirectoryMode()) {
                File f = new File(data.getInputPath().getFirst());
                inputPath.set(f.getParentFile().getAbsolutePath());
            } else {
                inputPath.set(data.getInputPath().stream().map(File::new).map(File::getName).collect(Collectors.joining(",")));
            }
            input_files = data.getInputPath().stream().map(File::new).toList();
        }
        outputPath.set(data.getOutputPath());
        output_dir = new File(data.getOutputPath());

        String mappingName = storage.getData().getMapping();
        for (int i = 0; i < mappingLoader.names().length; i++) {
            String name = mappingLoader.names()[i];
            if (name.equals(mappingName)) {
                selectedMapping.set(i);
                break;
            }
        }
        directoryMode.set(data.isDirectoryMode());
    }

    private boolean convert() {
        Mapping mapping = mappingLoader.getMappingByIndex(selectedMapping.get());

        List<ConversionResult> results = schematicConverter.convertMultiple(input_files, output_dir, mapping);
        for (ConversionResult result : results) {
            try {
                HytalePrefab prefab = result.toHytalePrefab();
                if (prefab != null) {
                    prefab.save(result.getName(), output_dir);
                    return true;
                }
            } catch (IOException e) {
                logger.error("Failed to save hytale prefab (name:'"+result.getName()+"'|output:'"+output_dir+"') : " + e.getMessage());
            }
        }
        return false;
    }

    private boolean dump() {
        Mapping mapping = mappingLoader.getMappingByIndex(selectedMapping.get());

        List<ConversionResult> results = schematicConverter.convertMultiple(input_files, output_dir, mapping);
        for (ConversionResult result : results) {
            WriteResponse dump = result.dump();
            if (dump.success()) {
                return true;
            } else {
                logger.error(dump.message());
            }
        }
        return false;
    }

    private void selectInput(){
        File path = new File(inputPath.get());

        if (directoryMode.get()) {
            File selected = Utils.openDirectoryDialog(path.exists() ? path : Utils.AppDir(),"Select schematic Folder");
            if (selected != null) {
                inputPath.set(selected.getAbsolutePath());

                input_files = Arrays.stream(Objects.requireNonNull(selected.listFiles(file ->
                        file.getName().endsWith(".schematic") || file.getName().endsWith(".schem")
                ))).toList();
            }
        } else {
            File[] files = Utils.openFilesDialog(path.exists() ? path : Utils.AppDir(), "Select schematic Files");
            if (files != null) {
                input_files = List.of(files);
                inputPath.set(input_files.stream().map(File::getName).collect(Collectors.joining(",")));
            }
        }
        storage.getData().setInputPath(input_files.stream().map(File::getAbsolutePath).toList());
        storage.save();
    }

    private void selectOutput() {
        File path = new File(outputPath.get());
        output_dir = Utils.openDirectoryDialog(path.exists() ? path : Utils.AppDir(),"Select Output Folder");
        if (output_dir != null) {
            outputPath.set(output_dir.getAbsolutePath());
            storage.getData().setOutputPath(output_dir.getAbsolutePath());
        }
        storage.save();
    }

}
