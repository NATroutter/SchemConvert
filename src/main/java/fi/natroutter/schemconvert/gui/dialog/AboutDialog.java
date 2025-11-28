package fi.natroutter.schemconvert.gui.dialog;

import fi.natroutter.foxlib.FoxLib;
import fi.natroutter.foxlib.logger.FoxLogger;
import fi.natroutter.foxlib.updates.GitHubVersionChecker;
import fi.natroutter.foxlib.updates.data.UpdateStatus;
import fi.natroutter.foxlib.updates.data.VersionInfo;
import fi.natroutter.schemconvert.SchemConvert;
import fi.natroutter.schemconvert.gui.GuiHelper;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiWindowFlags;

import java.io.IOException;


public class AboutDialog {

    private FoxLogger logger = SchemConvert.getLogger();

    private boolean open = false;

    private static final String AUTHOR = "NATroutter";
    private static final String REPO_OWNER = "NATroutter"; // Your GitHub username
    private static final String REPO_NAME = "SchemConvert"; // Your repo name
    private static final String GITHUB = "https://github.com/" + REPO_OWNER + "/" + REPO_NAME;

    private VersionInfo versionInfo = null;
    private boolean checkingForUpdates = false;
    private String updateStatus = "Checking for updates...";

    public void show() {
        this.open = true;
        checkForUpdates();
    }

    private void checkForUpdates() {
        if (!checkingForUpdates && versionInfo == null) {
            checkingForUpdates = true;
            updateStatus = "Checking for updates...";

            GitHubVersionChecker checker = new GitHubVersionChecker(REPO_OWNER, REPO_NAME, SchemConvert.VERSION);
            checker.setLogger(SchemConvert.getLogger());
            checker.checkForUpdates().thenAccept(info -> {
                versionInfo = info;
                checkingForUpdates = false;
                switch (info.getUpdateAvailable()) {
                    case YES -> updateStatus = "Update available!";
                    case NO ->  updateStatus = "Up to date";
                    case ERROR -> updateStatus = "Connection failed!";
                }
            }).exceptionally(ex -> {
                checkingForUpdates = false;
                updateStatus = "Failed to check for updates";
                System.err.println("Update check failed: " + ex.getMessage());
                return null;
            });
        }
    }

    public void render() {
        if (open) {
            ImGui.openPopup("About SchemConvert");
            open = false;
        }

        if (ImGui.beginPopupModal("About SchemConvert", ImGuiWindowFlags.AlwaysAutoResize)) {

            // Version information
            ImGui.text("Current Version: " + SchemConvert.VERSION);

            if (versionInfo != null) {
                ImGui.text("Latest Version:  " + versionInfo.getLatestVersion());
                ImGui.sameLine();

                switch (versionInfo.getUpdateAvailable()) {
                    case YES -> ImGui.textColored(1.0f, 0.8f, 0.0f, 1.0f, "(Update Available!)");
                    case NO ->  ImGui.textColored(0.0f, 1.0f, 0.0f, 1.0f, "(Up to date)");
                    case ERROR -> ImGui.textColored(1.0f, 0.0f, 0.0f, 1.0f, "(Connection failed!)");
                }

            } else {
                ImGui.text("Latest Version:  " + updateStatus);
            }

            ImGui.text("Build Date: " + SchemConvert.BUILD_DATE);
            ImGui.text("Author: " + AUTHOR);
            ImGui.spacing();

            // Description
            ImGui.separator();
            ImGui.spacing();
            ImGui.text("A powerful tool for converting schematics between");
            ImGui.text("different formats with customizable block mappings.");
            ImGui.spacing();

            // Links
            ImGui.text("GitHub: ");
            ImGui.sameLine();
            GuiHelper.renderClickableLink(GITHUB, GITHUB);

            ImGui.text("Website: ");
            ImGui.sameLine();
            GuiHelper.renderClickableLink("https://natroutter.fi", "https://natroutter.fi");

            ImGui.spacing();
            ImGui.separator();
            ImGui.spacing();

            // Features
            ImGui.text("Features:");
            ImGui.bulletText("Convert individual schematics or entire directories");
            ImGui.bulletText("Custom block mapping configurations");
            ImGui.bulletText("Support for multiple mapping presets");
            ImGui.bulletText("Cross-platform compatibility (Windows, Linux, Mac)");
            ImGui.bulletText("Real-time conversion progress tracking");

            ImGui.spacing();
            ImGui.separator();
            ImGui.spacing();

            // System information
            ImGui.text("System Information:");
            ImGui.text("  Java Version: " + System.getProperty("java.version"));
            ImGui.text("  OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
            ImGui.text("  Architecture: " + System.getProperty("os.arch"));

            ImGui.spacing();
            ImGui.separator();

            // Release notes (if available and update exists)
            if (versionInfo != null && versionInfo.getUpdateAvailable()==UpdateStatus.YES && !versionInfo.getReleaseNotes().isEmpty()) {
                ImGui.spacing();
                ImGui.text("Release Notes (" + versionInfo.getLatestVersion() + "):");
                ImGui.separator();

                String[] lines = versionInfo.getReleaseNotes().split("\n");
                int lineCount = 0;
                for (String line : lines) {
                    if (lineCount++ > 10) {
                        ImGui.text("... (see GitHub for full notes)");
                        break;
                    }
                    ImGui.textWrapped(line);
                }

                ImGui.spacing();
                ImGui.separator();
            }

            // Buttons
            if (versionInfo != null && versionInfo.getUpdateAvailable() == UpdateStatus.YES) {
                if (ImGui.button("Download Update", 150, 0)) {
                    try {
                        FoxLib.openURL(versionInfo.getReleaseUrl());
                    } catch (IOException e) {
                        logger.error("Failed to open URL '" + versionInfo.getReleaseUrl() + "': " + e.getMessage());
                    }
                }
                ImGui.sameLine();
            }

            if (ImGui.button("OK", new ImVec2(ImGui.getContentRegionAvailX(), 20))) {
                ImGui.closeCurrentPopup();
            }

            ImGui.endPopup();
        }
    }
}