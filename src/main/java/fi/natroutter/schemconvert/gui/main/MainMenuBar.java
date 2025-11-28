package fi.natroutter.schemconvert.gui.main;

import fi.natroutter.foxlib.logger.FoxLogger;
import fi.natroutter.schemconvert.SchemConvert;
import fi.natroutter.schemconvert.gui.dialog.AboutDialog;
import fi.natroutter.schemconvert.mappings.MappingLoader;
import imgui.ImGui;

public class MainMenuBar {

    private FoxLogger logger = SchemConvert.getLogger();
    private MappingLoader mappingLoader = SchemConvert.getMappingLoader();
    private AboutDialog aboutDialog = SchemConvert.getAboutDialog();

    public void render() {
        if (ImGui.beginMenuBar()) {
            if (ImGui.beginMenu("File")) {
                if (ImGui.menuItem("Reload")) {
                    logger.info("Reloading files!");
                    mappingLoader.reload();
                }
                ImGui.separator();
                if (ImGui.menuItem("Exit")) {
                    System.exit(0);
                }
                ImGui.endMenu();
            }

            if (ImGui.beginMenu("Help")) {
                if (ImGui.menuItem("About")) {
                    aboutDialog.show();
                }
                ImGui.endMenu();
            }
            ImGui.endMenuBar();
        }
    }

}
