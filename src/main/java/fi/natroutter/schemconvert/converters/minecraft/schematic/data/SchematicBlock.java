package fi.natroutter.schemconvert.converters.minecraft.schematic.data;

import fi.natroutter.foxlib.FoxLib;
import fi.natroutter.schemconvert.converters.UniBlock;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchematicBlock implements Cloneable {
    private int x;
    private int y;
    private int z;
    private String internalId;
    private HashMap<String, String> properties;

    public UniBlock toUniBlock() {
        if (internalId.startsWith("minecraft:")) {
            internalId = internalId.substring(10);
        }
        return new UniBlock(x, y, z, internalId, properties);
    }


    public static SchematicBlock fromLegacy(String input) {
        SchematicBlock block = new SchematicBlock();
        block.properties = new HashMap<>();
        int bracketIndex = input.indexOf('[');
        if (bracketIndex == -1) {
            block.internalId = input;
        } else {
            block.internalId = input.substring(0, bracketIndex);
            String props = input.substring(bracketIndex + 1, input.length() - 1);
            for (String pair : props.split(",")) {
                String[] kv = pair.split("=");
                if (kv.length == 2) block.properties.put(kv[0], kv[1]);
            }
        }
        return block;
    }

    @Override
    public SchematicBlock clone() {
        try {
            return (SchematicBlock) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
