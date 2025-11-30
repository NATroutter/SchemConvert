package fi.natroutter.schemconvert.converters;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum DumpMode {

    EVERYTHING("Everything"),
    MATERIAL_ONLY("Materials Only")
    ;

    String title;

    public static String[] list() {
        return Arrays.stream(DumpMode.values()).map(DumpMode::getTitle).toArray(String[]::new);
    }

    public static DumpMode getByIndex(int index) {
        return Arrays.stream(DumpMode.values())
                .filter(m -> m.ordinal() == index)
                .findFirst()
                .orElse(null);
    }
}
