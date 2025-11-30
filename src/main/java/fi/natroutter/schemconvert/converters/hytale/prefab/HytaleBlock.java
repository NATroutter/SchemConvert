package fi.natroutter.schemconvert.converters.hytale.prefab;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public class HytaleBlock {
    private String material;
    private long x;
    private long y;
    private long z;
    private Map<String, String> properties = new LinkedHashMap<>();

    public HytaleBlock(String material,long x,long y,long z) {
        this.material = material;
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
