package fi.natroutter.schemconvert.converters;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum ConvertMode {

    SCHEMATIC("Schematic"),
    STRUCTURE("Structure (nbt)"),
    LITEMATIC("Litematic")
    ;

    String title;

    public static String[] list() {
        return Arrays.stream(ConvertMode.values()).map(ConvertMode::getTitle).toArray(String[]::new);
    }

    public static ConvertMode getByIndex(int index) {
        return Arrays.stream(ConvertMode.values())
                .filter(m -> m.ordinal() == index)
                .findFirst()
                .orElse(null);
    }
}
