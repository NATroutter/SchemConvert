package fi.natroutter.schemconvert.converters.prefab;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor @NoArgsConstructor
public class HytalePrefab {

    public long x;
    public long y;
    public long z;
    public List<HytaleBlock> blocks;


    @Getter
    @AllArgsConstructor @NoArgsConstructor
    public static class HytaleBlock {
        public String material;
        public long x;
        public long y;
        public long z;
        public Map<String, String> properties = new LinkedHashMap<>();
    }

}
