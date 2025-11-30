package fi.natroutter.schemconvert.converters;

import fi.natroutter.foxlib.FoxLib;
import fi.natroutter.foxlib.files.FileUtils;
import fi.natroutter.foxlib.files.WriteResponse;
import fi.natroutter.schemconvert.converters.hytale.prefab.HytaleBlock;
import fi.natroutter.schemconvert.converters.hytale.prefab.HytalePrefab;
import fi.natroutter.schemconvert.mappings.Mapping;
import lombok.AllArgsConstructor;

import lombok.Data;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class ConversionResult {

    private String name;
    private List<UniBlock> blocks;
    private Mapping mapping;

    public HytalePrefab toHytalePrefab(Consumer<Float> progress) {
        List<HytaleBlock> hyBlocks = new ArrayList<>();
        int totalBlocks = blocks.size();

        for (int i = 0; i < totalBlocks; i++) {
            UniBlock block = blocks.get(i);

            // Report progress between 0.0-1.0
            float progressValue = (float) i / totalBlocks;
            progress.accept(progressValue);

            Mapping.Entry mappingEntry = null;

            //find Mapping Entry
            for (Mapping.Entry entry : mapping.getEntries()) {
                if (entry.getMc_material().equalsIgnoreCase(block.getMaterial())) {
                    if (matchesCondition(entry, block.getProperties())) {
                        mappingEntry = entry;
                        break;
                    }
                }
            }

            //Specific material mapping not found, skipping and continuing to next material!
            if (mappingEntry == null) {
                continue;
            }

            HytaleBlock hytaleBlock = new HytaleBlock(
                    mappingEntry.getHy_material(),
                    block.getX(),
                    block.getY(),
                    block.getZ()
            );

            //apply Condition Properties
            for (Mapping.Condition condition : mappingEntry.getConditions()) {
                if (condition.getCondition().isEmpty() || matchesConditionString(condition.getCondition(), block.getProperties())) {

                    //apply Properties
                    if (condition.getNewProperties() == null || condition.getNewProperties().isEmpty()) continue;
                    String[] properties = condition.getNewProperties().split(":");
                    for (String property : properties) {
                        String[] keyValue = property.split("=");
                        if (keyValue.length == 2) {
                            String key = keyValue[0].trim();
                            String value = keyValue[1].trim();
                            hytaleBlock.getProperties().put(key, value);
                        }
                    }
                    break;
                }
            }

            hyBlocks.add(hytaleBlock);
        }

        // Report completion
        progress.accept(1.0f);
        return new HytalePrefab(hyBlocks);
    }

    public WriteResponse dump(DumpMode mode, Consumer<Float> progress) {
        StringBuilder str = new StringBuilder();
        str.append("Name: ").append(name).append("\n");
        str.append("Date: ").append(FoxLib.getTimestamp()).append("\n");
        str.append("Block Count: ").append(blocks.size()).append("\n");
        str.append("--------------").append("\n");

        List<String> materials = new ArrayList<>();
        int totalBlocks = blocks.size();

        for (int i = 0; i < totalBlocks; i++) {
            UniBlock block = blocks.get(i);

            // Report progress between 0.0-1.0
            float progressValue = (float) i / totalBlocks;
            progress.accept(progressValue);

            switch (mode) {
                case EVERYTHING -> {
                    String data = block.getProperties().entrySet().stream()
                            .map(e -> e.getKey() + "=" + e.getValue())
                            .collect(Collectors.joining(","));

                    str.append(block.getMaterial());
                    str.append("(").append(block.getX()).append(",").append(block.getY()).append(",").append(block.getZ()).append(")");
                    if (!data.isEmpty()) {
                        str.append("[").append(data).append("]");
                    }
                    str.append("\n");
                }
                case MATERIAL_ONLY -> {
                    if (!materials.contains(block.getMaterial())) {
                        str.append(block.getMaterial());
                        str.append("\n");
                        materials.add(block.getMaterial());
                    }
                }
            }
        }

        // Report completion
        progress.accept(1.0f);

        Path path = Path.of(System.getProperty("user.dir"), "dumps");
        path.toFile().mkdirs();
        File dump = new File(path.toFile(), "dump-"+name+"-"+FoxLib.getTimestamp()+".txt");
        return FileUtils.writeFile(dump, str.toString(), (p)-> {
            FoxLib.println(name + " : " + p);
        });
    }



    private boolean matchesCondition(Mapping.Entry entry, Map<String, String> blockProperties) {
        if (entry.getConditions() == null || entry.getConditions().isEmpty()) return true;

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
