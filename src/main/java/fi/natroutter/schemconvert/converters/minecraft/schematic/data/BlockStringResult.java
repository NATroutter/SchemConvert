package fi.natroutter.schemconvert.converters.minecraft.schematic.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter @Setter
@AllArgsConstructor
public class BlockStringResult {
    private String id;
    private HashMap<String, String> properties;
}
