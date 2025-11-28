package fi.natroutter.schemconvert.gui.main;

import fi.natroutter.schemconvert.SchemConvert;
import fi.natroutter.schemconvert.converters.SchematicConverter;
import fi.natroutter.schemconvert.mappings.Mapping;
import fi.natroutter.schemconvert.mappings.MappingLoader;
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
import java.util.Arrays;
import java.util.stream.Collectors;

public class MainWindow {

    private MappingLoader mappingLoader = SchemConvert.getMappingLoader();
    private SchematicConverter schematicConverter = SchemConvert.getSchematicConverter();

    private final ImString inputPath = new ImString();
    private final ImString outputPath = new ImString();
    private final ImBoolean directoryMode = new ImBoolean(false);
    private final ImInt selectedMapping = new ImInt(0);

    private MainMenuBar menuBar = new MainMenuBar();

    private File[] input_files;
    private File output_dir;

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
            ImGui.inputText("##schematic", inputPath, ImGuiInputTextFlags.CallbackResize);
            ImGui.sameLine();
            if (ImGui.button("...##schematic")) {
                selectInput();
            }

            ImGui.text("Output Directory");
            ImGui.setNextItemWidth(ImGui.getContentRegionAvailX() - buttonWidth - spacing);
            ImGui.inputText("##output", outputPath, ImGuiInputTextFlags.CallbackResize);
            ImGui.sameLine();
            if (ImGui.button("...##output")) {
                selectOutput();
            }

            ImGui.text("Mappings");
            ImGui.setNextItemWidth(ImGui.getContentRegionAvailX());
            ImGui.combo("##Mappings", selectedMapping, mappingLoader.names(), ImGuiInputTextFlags.CallbackResize);

            ImGui.separator();
            ImGui.text("Options");
            if (ImGui.checkbox("Directory mode", directoryMode)) {
                inputPath.set("");
            }

            ImGui.separator();
            if (ImGui.button("Convert", new ImVec2(ImGui.getContentRegionAvailX(), 20))) {
                convert();
            }

        }
        ImGui.end();
        ImGui.popStyleVar(1);
    }

    private void convert() {
        Mapping mapping = mappingLoader.getMappingByIndex(selectedMapping.get());
        schematicConverter.convert(input_files, output_dir, mapping);
    }

    private void selectInput(){
        File path = new File(inputPath.get());

        if (directoryMode.get()) {
            File selected = Utils.openDirectoryDialog(path.exists() ? path : Utils.AppDir(),"Select schematic Folder");
            if (selected != null) {
                inputPath.set(selected.getAbsolutePath());

                input_files = selected.listFiles(file ->
                        file.getName().endsWith(".schematic") || file.getName().endsWith(".schem")
                );
            }
        } else {
            input_files = Utils.openFilesDialog(path.exists() ? path : Utils.AppDir(),"Select schematic Files");
            if (input_files != null) {
                inputPath.set(Arrays.stream(input_files).map(File::getName).collect(Collectors.joining(",")));
            }
        }
    }

    private void selectOutput() {
        File path = new File(outputPath.get());
        output_dir = Utils.openDirectoryDialog(path.exists() ? path : Utils.AppDir(),"Select Output Folder");
        if (output_dir != null) {
            outputPath.set(output_dir.getAbsolutePath());
        }
    }

}
