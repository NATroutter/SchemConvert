package fi.natroutter.schemconvert.converters.minecraft.schematic.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchematicData {
    private int width;
    private int height;
    private int length;
    private SchematicFormat format;
    private List<SchematicBlock> blocks;
}
