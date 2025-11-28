package fi.natroutter.schemconvert.converters.hytale.prefab;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class HytaleBlock {
    public String material;
    public long x;
    public long y;
    public long z;
    public Map<String, String> properties = new LinkedHashMap<>();
}
