package fi.natroutter.schemconvert.converters;

import com.cryptomorin.xseries.XMaterial;
import fi.natroutter.schemconvert.converters.hytale.prefab.HytaleBlock;
import fi.natroutter.schemconvert.converters.hytale.prefab.HytalePrefab;
import fi.natroutter.schemconvert.mappings.Mapping;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class ConversionResult {

    private String name;
    private List<UniBlock> blocks;
    private Mapping mapping;

    public HytalePrefab toHytalePrefab() {
        List<HytaleBlock> hyBlocks = new ArrayList<>();
        for (UniBlock block : blocks) {
            Mapping.Entry mappingEntry = findMappingEntry(block, mapping);
            if (mappingEntry == null) {
                return null;
            }

            HytaleBlock hytaleBlock = new HytaleBlock();
            hytaleBlock.material = mappingEntry.getHy_material();
            hytaleBlock.x = block.getX();
            hytaleBlock.y = block.getY();
            hytaleBlock.z = block.getZ();

            applyConditionProperties(hytaleBlock, mappingEntry, block.getProperties());
            hyBlocks.add(hytaleBlock);
        }
        return new HytalePrefab(hyBlocks);
    }



    private Mapping.Entry findMappingEntry(UniBlock data, Mapping mapping) {
        for (Mapping.Entry entry : mapping.getEntries()) {
            if (entry.getMc_material().equalsIgnoreCase(data.getMaterial().name())) {
                if (matchesCondition(entry, data.getProperties())) {
                    return entry;
                }
            }
        }
        return null;
    }

    private void applyConditionProperties(HytaleBlock hytalBlock, Mapping.Entry entry, Map<String, String> blockProperties) {
        for (Mapping.Condition condition : entry.getConditions()) {
            if (condition.getCondition().isEmpty() || matchesConditionString(condition.getCondition(), blockProperties)) {
                applyProperties(hytalBlock, condition.getNewProperties());
                break;
            }
        }
    }

    private void applyProperties(HytaleBlock hytalBlock, String propertiesStr) {
        if (propertiesStr == null || propertiesStr.isEmpty()) {
            return;
        }

        String[] properties = propertiesStr.split(":");
        for (String property : properties) {
            String[] keyValue = property.split("=");
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                hytalBlock.properties.put(key, value);
            }
        }
    }

    private boolean matchesCondition(Mapping.Entry entry, Map<String, String> blockProperties) {
        if (entry.getConditions() == null || entry.getConditions().isEmpty()) {
            return true;
        }

        for (Mapping.Condition condition : entry.getConditions()) {
            if (condition.getCondition().isEmpty() || matchesConditionString(condition.getCondition(), blockProperties)) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesConditionString(String conditionStr, Map<String, String> blockProperties) {
        if (conditionStr.isEmpty()) {
            return true;
        }

        String[] conditionPairs = conditionStr.split(":");
        for (String pair : conditionPairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                if (!blockProperties.getOrDefault(key, "").equalsIgnoreCase(value)) {
                    return false;
                }
            }
        }
        return true;
    }

}
