package fi.natroutter.schemconvert.converters.minecraft.schematic.data;

import com.cryptomorin.xseries.XMaterial;
import fi.natroutter.schemconvert.converters.UniBlock;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.material.MaterialData;

import java.util.HashMap;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchematicBlock {
    private int x;
    private int y;
    private int z;
    private String internalId;
    private HashMap<String, String> properties;

    public UniBlock toUniBlock() {
        XMaterial material = XMaterial.matchXMaterial(internalId).orElse(null);
        return new UniBlock(x,y,z,material,properties);
    }

}
