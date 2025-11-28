package fi.natroutter.schemconvert.converters;

import com.google.gson.GsonBuilder;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.cli.CLIRegistries;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.Preference;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.io.ResourceLoader;
import com.sk89q.worldedit.util.translation.TranslationManager;
import com.sk89q.worldedit.world.DataFixer;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.registry.Registries;
import fi.natroutter.foxlib.logger.FoxLogger;
import fi.natroutter.schemconvert.SchemConvert;
import fi.natroutter.schemconvert.converters.prefab.HytalePrefab;
import fi.natroutter.schemconvert.mappings.Mapping;
import org.enginehub.piston.CommandManager;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SchematicConverter implements IConverter {

    private FoxLogger logger = SchemConvert.getLogger();
    private Mapping mapping;
    private static boolean worldEditInitialized = false;

    @Override
    public void convert(File[] input_files, File output_dir, Mapping mapping) {
        logger.info("Processing Files: " + Arrays.stream(input_files).map(File::getName).collect(Collectors.joining(",")));
        logger.info("Outputting Files: " + output_dir.getAbsolutePath());

        this.mapping = mapping;

        initializeWorldEdit();

        for (File schematicFile : input_files) {
            try {
                convertSchematic(schematicFile, output_dir);
            } catch (Exception e) {
                logger.error("Failed to convert " + schematicFile.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void initializeWorldEdit() {
        if (worldEditInitialized) {
            return;
        }

        try {
            WorldEdit worldEdit = WorldEdit.getInstance();

            worldEditInitialized = true;
            logger.info("WorldEdit initialized successfully");
        } catch (Exception e) {
            logger.warn("WorldEdit initialization: " + e.getMessage());
            worldEditInitialized = true;
        }
    }


    private void convertSchematic(File schematicFile, File output_dir) throws IOException {
        logger.info("Converting: " + schematicFile.getName());

        Clipboard clipboard;

        ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
        try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
            clipboard = reader.read();
        }

        HytalePrefab prefab = extractPrefabFromClipboard(clipboard);
        saveHytalePrefab(prefab, schematicFile, output_dir);

        logger.info("Successfully converted: " + schematicFile.getName());
    }

    private HytalePrefab extractPrefabFromClipboard(Clipboard clipboard) {
        HytalePrefab prefab = new HytalePrefab();
        prefab.x = 0;
        prefab.y = 0;
        prefab.z = 0;

        List<HytalePrefab.HytaleBlock> blocks = new ArrayList<>();

        for (BlockVector3 pos : clipboard.getRegion()) {
            BlockState blockState = clipboard.getBlock(pos);
            HytalePrefab.HytaleBlock hytalBlock = convertBlock(blockState, pos);
            if (hytalBlock != null) {
                blocks.add(hytalBlock);
            }
        }

        prefab.blocks = blocks;
        return prefab;
    }

    private HytalePrefab.HytaleBlock convertBlock(BlockState blockState, BlockVector3 pos) {
        String minecraftMaterial = blockState.getBlockType().getMaterial().toString();
        Map<String, String> blockProperties = getBlockProperties(blockState);

        Mapping.Entry mappingEntry = findMappingEntry(minecraftMaterial, blockProperties);
        if (mappingEntry == null) {
            return null;
        }

        HytalePrefab.HytaleBlock hytaleBlock = new HytalePrefab.HytaleBlock();
        hytaleBlock.material = mappingEntry.getHy_material();
        hytaleBlock.x = pos.x();
        hytaleBlock.y = pos.y();
        hytaleBlock.z = pos.z();

        applyConditionProperties(hytaleBlock, mappingEntry, blockProperties);

        return hytaleBlock;
    }

    private Map<String, String> getBlockProperties(BlockState blockState) {
        Map<String, String> properties = new HashMap<>();
        blockState.getStates().forEach((key, value) ->
            properties.put(key.getName(), value.toString())
        );
        return properties;
    }

    private Mapping.Entry findMappingEntry(String material, Map<String, String> blockProperties) {
        for (Mapping.Entry entry : mapping.getEntries()) {
            if (entry.getMc_material().equalsIgnoreCase(material)) {
                if (matchesCondition(entry, blockProperties)) {
                    return entry;
                }
            }
        }
        return null;
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

    private void applyConditionProperties(HytalePrefab.HytaleBlock hytalBlock, Mapping.Entry entry, Map<String, String> blockProperties) {
        for (Mapping.Condition condition : entry.getConditions()) {
            if (condition.getCondition().isEmpty() || matchesConditionString(condition.getCondition(), blockProperties)) {
                applyProperties(hytalBlock, condition.getNewProperties());
                break;
            }
        }
    }

    private void applyProperties(HytalePrefab.HytaleBlock hytalBlock, String propertiesStr) {
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

    private void saveHytalePrefab(HytalePrefab prefab, File inputFile, File output_dir) throws IOException {
        String outputFileName = inputFile.getName().replaceAll("\\.[^.]+$", ".prefab.json");
        File outputFile = new File(output_dir, outputFileName);

        try (FileWriter writer = new FileWriter(outputFile)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(prefab, writer);
        }
    }

}