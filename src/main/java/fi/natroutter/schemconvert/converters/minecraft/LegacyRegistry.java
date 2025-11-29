package fi.natroutter.schemconvert.converters.minecraft;

import com.mongodb.Block;
import fi.natroutter.foxlib.files.FileUtils;
import fi.natroutter.foxlib.logger.FoxLogger;
import fi.natroutter.schemconvert.SchemConvert;
import fi.natroutter.schemconvert.converters.minecraft.schematic.data.SchematicBlock;
import lombok.Getter;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class LegacyRegistry {

    private FoxLogger logger = SchemConvert.getLogger();

    private final Map<String, SchematicBlock> BLOCK_CACHE = new HashMap<>();

    public LegacyRegistry() {
        BLOCK_CACHE.clear();

        InputStream stream = FileUtils.streamResource("legacy_mappings.txt");

        // 3. Read the file from the disk
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {

            reader.lines().forEach(line -> {
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    BLOCK_CACHE.put(parts[0], SchematicBlock.fromLegacy(parts[1]));
                }
            });

            logger.info("Loaded " + BLOCK_CACHE.size() + " mappings from legacy_mappings.txt");

        } catch (IOException e) {
            logger.error("Failed to load legacy_mappings.txt!");
            e.printStackTrace();
        }
    }

    public SchematicBlock get(int id, int data) {
        return BLOCK_CACHE.get(id + ":" + data);
    }
}