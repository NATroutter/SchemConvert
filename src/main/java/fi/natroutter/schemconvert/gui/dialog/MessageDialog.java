package fi.natroutter.schemconvert.gui.dialog;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;

public class MessageDialog {

    private boolean shouldOpen = false;
    private String title = "";
    private String message = "";
    private MessageBoxType type = MessageBoxType.INFO;
    private Runnable onConfirm = null;
    private Runnable onCancel = null;

    public enum MessageBoxType {
        INFO,           // OK button only
        WARNING,        // OK button only
        ERROR,          // OK button only
        YES_NO,         // Yes/No buttons
        OK_CANCEL       // OK/Cancel buttons
    }

    public void show(String title, String message) {
        show(title, message, MessageBoxType.INFO);
    }

    public void show(String title, String message, MessageBoxType type) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.shouldOpen = true;
        this.onConfirm = null;
        this.onCancel = null;
    }

    public void show(String title, String message, MessageBoxType type, Runnable onConfirm) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.shouldOpen = true;
        this.onConfirm = onConfirm;
        this.onCancel = null;
    }

    public void show(String title, String message, MessageBoxType type, Runnable onConfirm, Runnable onCancel) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.shouldOpen = true;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    public void render() {
        if (shouldOpen) {
            ImGui.openPopup(title);
            shouldOpen = false;
        }

        if (ImGui.beginPopupModal(title, ImGuiWindowFlags.AlwaysAutoResize)) {
            ImGui.text(message);
            ImGui.separator();

            switch (type) {
                case INFO:
                case WARNING:
                case ERROR:
                    renderOkButton();
                    break;
                case YES_NO:
                    renderYesNoButtons();
                    break;
                case OK_CANCEL:
                    renderOkCancelButtons();
                    break;
            }

            ImGui.endPopup();
        }
    }

    private void renderOkButton() {
        if (ImGui.button("OK", 120, 0)) {
            if (onConfirm != null) {
                onConfirm.run();
            }
            ImGui.closeCurrentPopup();
        }
    }

    private void renderYesNoButtons() {
        if (ImGui.button("Yes", 120, 0)) {
            if (onConfirm != null) {
                onConfirm.run();
            }
            ImGui.closeCurrentPopup();
        }

        ImGui.sameLine();

        if (ImGui.button("No", 120, 0)) {
            if (onCancel != null) {
                onCancel.run();
            }
            ImGui.closeCurrentPopup();
        }
    }

    private void renderOkCancelButtons() {
        if (ImGui.button("OK", 120, 0)) {
            if (onConfirm != null) {
                onConfirm.run();
            }
            ImGui.closeCurrentPopup();
        }

        ImGui.sameLine();

        if (ImGui.button("Cancel", 120, 0)) {
            if (onCancel != null) {
                onCancel.run();
            }
            ImGui.closeCurrentPopup();
        }
    }
}