package fi.natroutter.schemconvert.gui.dialog;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class DialogButton {
    public String label;
    public Runnable action;

    public DialogButton(String label, Runnable action) {
        this.label = label;
        this.action = action;
    }

    public DialogButton(String label) {
        this(label, null);
    }
}