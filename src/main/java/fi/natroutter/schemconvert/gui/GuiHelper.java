package fi.natroutter.schemconvert.gui;

import fi.natroutter.foxlib.FoxLib;
import fi.natroutter.foxlib.logger.FoxLogger;
import fi.natroutter.schemconvert.SchemConvert;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiMouseCursor;

import java.io.IOException;

public class GuiHelper {

    private static FoxLogger logger = SchemConvert.getLogger();

    public static void renderClickableLink(String label, String url) {
        ImGui.textColored(0.3f, 0.7f, 1.0f, 1.0f, label);

        if (ImGui.isItemHovered()) {
            ImGui.setMouseCursor(ImGuiMouseCursor.Hand);

            // Draw underline
            ImVec2 min = ImGui.getItemRectMin();
            ImVec2 max = ImGui.getItemRectMax();
            ImGui.getWindowDrawList().addLine(
                    min.x, max.y,
                    max.x, max.y,
                    ImGui.getColorU32(0.3f, 0.7f, 1.0f, 1.0f)
            );
        }

        if (ImGui.isItemClicked()) {
            try {
                FoxLib.openURL(url);
            } catch (IOException e) {
                logger.error("Failed to open URL '" + url + "': " + e.getMessage());
            }
        }
    }

}
