package fi.natroutter.schemconvert.converters;

import fi.natroutter.foxlib.FoxLib;
import fi.natroutter.foxlib.files.FileUtils;
import fi.natroutter.foxlib.files.ReadResponse;
import fi.natroutter.foxlib.files.WriteResponse;
import fi.natroutter.schemconvert.converters.hytale.prefab.HytaleBlock;
import fi.natroutter.schemconvert.converters.hytale.prefab.HytalePrefab;
import fi.natroutter.schemconvert.mappings.Mapping;
import fi.natroutter.schemconvert.utilities.Utils;
import lombok.AllArgsConstructor;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

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
                continue;
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

    public String dump() {
        StringBuilder str = new StringBuilder();
        str.append("Name: ").append(name);
        str.append("Date: ").append(FoxLib.getTimestamp());
        str.append("--------------");
        for (UniBlock block : blocks) {
            String data = block.getProperties().entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(","));
            str.append(block.getMaterial()).append("[").append(data).append("]");
        }

        Path path = Path.of(System.getProperty("user.dir"), "dumps");
        File dump = new File(path.toFile(), "dump-"+name+"-"+FoxLib.getTimestamp()+".txt");
        WriteResponse resp = FileUtils.writeFile(dump, str.toString());
        if (resp.success()) {
            return dump.getAbsolutePath();
        }
        return null;
    }


    private Mapping.Entry findMappingEntry(UniBlock data, Mapping mapping) {
        for (Mapping.Entry entry : mapping.getEntries()) {
            if (entry.getMc_material().equalsIgnoreCase(data.getMaterial())) {
        //  if (entry.getMc_material().equalsIgnoreCase(data.getMaterial().name())) { //TODO material checking
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
