package fi.natroutter.schemconvert.gui.main;

import fi.natroutter.foxlib.files.FileUtils;
import fi.natroutter.foxlib.files.WriteResponse;
import fi.natroutter.foxlib.logger.FoxLogger;
import fi.natroutter.schemconvert.SchemConvert;
import fi.natroutter.schemconvert.converters.*;
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
import imgui.type.ImFloat;
import imgui.type.ImInt;
import imgui.type.ImString;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private final ImInt selectedDumpMode = new ImInt(0);
    private final ImInt selectedConvertMode = new ImInt(0);
    private final ImFloat oneFileProgress = new ImFloat(0.0f);
    private final ImInt allFileProgress = new ImInt(0);
    private final ImInt allFileProgressTotal = new ImInt(0);
    private final ImInt currentFile = new ImInt(0);

    private MainMenuBar menuBar = new MainMenuBar();
    private MessageDialog messageDialog = SchemConvert.getMessageDialog();

    private List<File> input_files = new ArrayList<>();
    private File output_dir;

    private boolean disableControls = false;
    private ConvertStatus currentStatus = ConvertStatus.IDLE;

    @AllArgsConstructor @Getter
    private enum ConvertStatus {
        IDLE("Idle..."),
        PARSING_FILE("Parsing..."),
        CONVERTING("Converting..."),
        DUMPING("Dumping..."),
        SAVING("Saving...");
        String status;
    }

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

            ImGui.beginDisabled(disableControls);

            menuBar.render();

            // Create a child window for main content (leaves space for progress bars)
            float StatusDisplayChildHeight = ImGui.getStyle().getFramePaddingX() + ImGui.getStyle().getItemSpacingY() + 55;

            ImGui.beginChild("MainContent", 0, -StatusDisplayChildHeight, false, ImGuiWindowFlags.NoBackground);

            ImVec2 buttonSize = ImGui.calcTextSize("...");
            float buttonWidth = buttonSize.x + ImGui.getStyle().getFramePaddingX() * 2;
            float spacing = ImGui.getStyle().getItemSpacingX();

            ImGui.text("Input " + (directoryMode.get() ? "directory" : "file"));
            ImGui.setNextItemWidth(ImGui.getContentRegionAvailX() - buttonWidth - spacing);
            ImGui.inputText("##input", inputPath, ImGuiInputTextFlags.CallbackResize | ImGuiInputTextFlags.ReadOnly);
            ImGui.sameLine();
            if (ImGui.button("...##input")) {
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
            ImGui.spacing();
            ImGui.separator();
            ImGui.spacing();

            if (ImGui.checkbox("Directory mode", directoryMode)) {
                storage.getData().setDirectoryMode(directoryMode.get());
                storage.getData().setInputPath(new ArrayList<>());
                inputPath.set("");
                input_files = new ArrayList<>();
                storage.save();
            }
//            ImGui.sameLine();

            ImGui.text("Convert Mode");
            ImGui.setNextItemWidth(ImGui.getContentRegionAvailX());
            if (ImGui.combo("##convertmode", selectedConvertMode, ConvertMode.list(), ImGuiInputTextFlags.CallbackResize)) {
                storage.getData().setConvertMode(selectedConvertMode.get());
                storage.save();
            }

            ImGui.text("Dump Mode");
            ImGui.setNextItemWidth(ImGui.getContentRegionAvailX());
            if (ImGui.combo("##dumpmode", selectedDumpMode, DumpMode.list(), ImGuiInputTextFlags.CallbackResize)) {
                storage.getData().setDumpMode(selectedDumpMode.get());
                storage.save();
            }

            ImGui.spacing();
            ImGui.separator();
            ImGui.spacing();

            if (ImGui.button("Dump Data", new ImVec2((ImGui.getContentRegionAvailX() / 2) - spacing, 40))) {
                CompletableFuture<Boolean> dump = dump();
                dump.thenAccept(bool-> {
                    if (bool) {
                        logger.info("Files dumped successfully!");
                        messageDialog.show("SchemConvert", "Files dumped successfully!", List.of(
                                new DialogButton("OK"),
                                new DialogButton("Open", () -> {
                                    logger.info("Opening file...");
                                    try {
                                        FileUtils.openFileExplorer(Path.of(System.getProperty("user.dir"), "dumps").toFile());
                                    } catch (IOException e) {
                                        logger.error("Failed to open file explorer : " + e.getMessage());
                                    }
                                })
                        ));
                    } else {
                        logger.error("File dumping failed!");
                        messageDialog.show("SchemConvert", "File dumping failed!");
                    }
                });
            }

            ImGui.sameLine();

            if (ImGui.button("Convert", new ImVec2(ImGui.getContentRegionAvailX(), 40))) {
                CompletableFuture<Boolean> convert = convert();
                convert.thenAccept(bool -> {
                    if (bool) {
                        messageDialog.show("SchemConvert", (directoryMode.get() ? "Files" : "file") + " converted successfully!");
                    } else {
                        messageDialog.show("SchemConvert", (directoryMode.get() ? "Files" : "file") + " converted failed!");
                    }
                });
            }

            ImGui.endChild();

            ImGui.endDisabled();

//            Status Dispaly

            ImGui.text("Status: " + currentStatus.getStatus());

            ImGui.progressBar(oneFileProgress.get(), new ImVec2(ImGui.getContentRegionAvailX(), 20));

            String text = String.format("%d / %d files", allFileProgress.get(), allFileProgressTotal.get());
            float progress = allFileProgressTotal.get() > 0 ? (float) allFileProgress.get() / allFileProgressTotal.get() : 0.0f;
            ImGui.progressBar(progress, new ImVec2(ImGui.getContentRegionAvailX(), 20), text);


        }
        ImGui.end();
        ImGui.popStyleVar(1);
    }

    private void loadData() {
        DataStore data = storage.getData();

        if (!data.getInputPath().isEmpty()) {
            if (storage.getData().isDirectoryMode()) {
                File f = new File(data.getInputPath().getFirst());
                if (f.exists()) {
                    inputPath.set(f.getParentFile().getAbsolutePath());
                }
            } else {
                inputPath.set(data.getInputPath().stream().map(File::new).filter(File::exists).map(File::getName).collect(Collectors.joining(",")));
            }
            input_files = data.getInputPath().stream().map(File::new).filter(File::exists).toList();
        }

        output_dir = new File(data.getOutputPath());
        if (!output_dir.exists()) {
            output_dir = null;
        } else {
            outputPath.set(data.getOutputPath());
        }

        String mappingName = storage.getData().getMapping();
        for (int i = 0; i < mappingLoader.names().length; i++) {
            String name = mappingLoader.names()[i];
            if (name.equals(mappingName)) {
                selectedMapping.set(i);
                break;
            }
        }
        directoryMode.set(data.isDirectoryMode());
        selectedDumpMode.set(data.getDumpMode());
        selectedConvertMode.set(data.getConvertMode());
    }

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    //TODo multifile dump creates only one file!

    private CompletableFuture<Boolean> convert() {
        disableControls = true;

        return CompletableFuture.supplyAsync(() -> {
            ConvertMode mode = ConvertMode.getByIndex(selectedConvertMode.get());
            if (mode == null) return false;

            IConverter converter = switch (mode) {
                case SCHEMATIC -> schematicConverter;
                case LITEMATIC -> schematicConverter; //TODO implement th real converter
                case STRUCTURE -> schematicConverter; //TODO implement th real converter
            };

            allFileProgressTotal.set(input_files.size());
            currentStatus = ConvertStatus.PARSING_FILE;

            Mapping mapping = mappingLoader.getMappingByIndex(selectedMapping.get());
//            List<ConversionResult> results = converter.convertMultiple(input_files, output_dir, mapping, oneFileProgress::set, allFileProgress::set);

            currentStatus = ConvertStatus.CONVERTING;
            allFileProgressTotal.set(results.size());
            allFileProgress.set(0);
            oneFileProgress.set(0);

            HashMap<String,HytalePrefab> prefabs = new HashMap<>();

            for (int i = 0; i < results.size(); i++) {
                ConversionResult result = results.get(i);
                allFileProgress.set(i + 1);
                oneFileProgress.set(0);

                HytalePrefab prefab = result.toHytalePrefab(oneFileProgress::set);
                if (prefab != null) {
                    prefabs.put(result.getName(), prefab);
                }
            }

            currentStatus = ConvertStatus.SAVING;
            allFileProgressTotal.set(prefabs.size());
            allFileProgress.set(0);
            oneFileProgress.set(0);

            int saveIndex = 0;
            for (Map.Entry<String, HytalePrefab> entry : prefabs.entrySet()) {
                allFileProgress.set(saveIndex + 1);
                HytalePrefab prefab = entry.getValue();
                String fileName = entry.getKey();
                try {
                    logger.info("Saving : " + fileName);
                    prefab.save(fileName, output_dir, oneFileProgress::set);
                } catch (IOException e) {
                    logger.error("Failed to save hytale prefab (name:'" + fileName + "'|output:'" + output_dir + "') : " + e.getMessage());
                    return false;
                }
                saveIndex++;
            }


            allFileProgress.set(0);
            oneFileProgress.set(0);
            currentStatus = ConvertStatus.IDLE;
            disableControls = false;
            return true;

        }, executor);
    }



    private CompletableFuture<Boolean> dump() {
        disableControls = true;
        currentStatus = ConvertStatus.PARSING_FILE;

        return CompletableFuture.supplyAsync(() -> {
            Mapping mapping = mappingLoader.getMappingByIndex(selectedMapping.get());
            DumpMode mode = DumpMode.getByIndex(selectedDumpMode.get());
            if (mode == null) return false;

            List<ConversionResult> results = schematicConverter.convertMultiple(input_files, output_dir, mapping, oneFileProgress::set, allFileProgress::set);

            currentStatus = ConvertStatus.DUMPING;

            for (int i = 0; i < results.size(); i++) {
                ConversionResult result = results.get(i);
                allFileProgress.set(i + 1);
                oneFileProgress.set(0);

                WriteResponse dump = result.dump(mode, oneFileProgress::set);
                if (dump.success()) {
                    disableControls = false;
                } else {
                    logger.error(dump.message());
                }
            }

            allFileProgress.set(0);
            oneFileProgress.set(0);
            currentStatus = ConvertStatus.IDLE;
            return true;
        }, executor);
    }

    private void selectInput() {
        File path = new File(inputPath.get());

        if (directoryMode.get()) {
            File selected = Utils.openDirectoryDialog(path.exists() ? path : Utils.AppDir(), "Select schematic Folder");
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
        output_dir = Utils.openDirectoryDialog(path.exists() ? path : Utils.AppDir(), "Select Output Folder");
        if (output_dir != null) {
            outputPath.set(output_dir.getAbsolutePath());
            storage.getData().setOutputPath(output_dir.getAbsolutePath());
        }
        storage.save();
    }

}
