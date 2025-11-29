package fi.natroutter.schemconvert.converters.minecraft.schematic;

import fi.natroutter.foxlib.FoxLib;
import fi.natroutter.foxlib.files.FileUtils;
import fi.natroutter.foxlib.logger.FoxLogger;
import fi.natroutter.schemconvert.SchemConvert;
import fi.natroutter.schemconvert.converters.ConversionResult;
import fi.natroutter.schemconvert.converters.IConverter;
import fi.natroutter.schemconvert.converters.UniBlock;
import fi.natroutter.schemconvert.converters.minecraft.schematic.data.BlockStringResult;
import fi.natroutter.schemconvert.converters.minecraft.schematic.data.SchematicBlock;
import fi.natroutter.schemconvert.converters.minecraft.schematic.data.SchematicData;
import fi.natroutter.schemconvert.converters.minecraft.schematic.data.SchematicFormat;
import fi.natroutter.schemconvert.mappings.Mapping;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import com.sk89q.jnbt.*;

public class SchematicConverter implements IConverter {

    private static FoxLogger logger = SchemConvert.getLogger();
    private static LegacyRegistry legacyRegistry = SchemConvert.getLegacyRegistry();

    @Override
    public ConversionResult convertSingle(File input_file, File output_dir, Mapping mapping) {
        SchematicData schematic = loadSchematicManually(input_file);
        if (schematic == null) return null;

        String name = FileUtils.getBasename(input_file);
        List<UniBlock> blocks = schematicToUniBlocks(schematic);
        return new ConversionResult(name, blocks, mapping);
    }

    private List<UniBlock> schematicToUniBlocks(SchematicData schematic) {
        List<SchematicBlock> blocks = schematic.getBlocks();
        if (blocks == null) return null;

        return blocks.stream().map(SchematicBlock::toUniBlock).toList();
    }

    private static SchematicData loadSchematicManually(File file) {
        try (NBTInputStream nbt = new NBTInputStream(new GZIPInputStream(new FileInputStream(file)))) {
            // Handle Generic Tag<?> Issue
            NamedTag rootNamed = nbt.readNamedTag();

            if (!(rootNamed.getTag() instanceof CompoundTag)) {
                throw new RuntimeException("Root is not a CompoundTag");
            }

            CompoundTag rootTag = (CompoundTag) rootNamed.getTag();
            Map<String, Tag<?,?>> data = rootTag.getValue();

            // 1. Handle "Schematic" Wrapper
            if (data.containsKey("Schematic")) {
                data = ((CompoundTag) data.get("Schematic")).getValue();
            }

            // 2. ROUTING LOGIC based on Inspector Output
            Tag blocksTag = data.get("Blocks");

            if (blocksTag instanceof CompoundTag) {
                // "Blocks" is a Container -> It's Sponge V3
                logger.info("Parsing Schematic: " + FileUtils.getBasename(file) + " | Version: " + SchematicFormat.SPONGE_V3.name());
                return parseSpongeV3(data, (CompoundTag) blocksTag);
            } else if (blocksTag instanceof ByteArrayTag) {
                // "Blocks" is a ByteArray -> It's Legacy
                logger.info("Parsing Schematic: " + FileUtils.getBasename(file) + " | Version: " + SchematicFormat.LEGACY.name());
                return parseLegacy(data);
            } else if (data.containsKey("Palette")) {
                // "Palette" at top level -> Sponge V1/V2
                logger.info("Parsing Schematic: " + FileUtils.getBasename(file) + " | Version: " + SchematicFormat.SPONGE_V2.name());
                return parseSpongeV2(data);
            }

            logger.error("Unknown Structure. Keys: " + data.keySet());
            return null;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // --- PARSER: SPONGE V3 ---
    private static SchematicData parseSpongeV3(Map<String, Tag<?,?>> rootData, CompoundTag blocksContainer) {
        // Dimensions are at the ROOT level
        int width = ((ShortTag) rootData.get("Width")).getValue();
        int height = ((ShortTag) rootData.get("Height")).getValue();
        int length = ((ShortTag) rootData.get("Length")).getValue();

        Map<String, Tag<?,?>> blocksMap = blocksContainer.getValue();

        // 1. Palette is inside "Blocks"
        Map<String, Tag<?,?>> paletteTag = ((CompoundTag) blocksMap.get("Palette")).getValue();
        Map<Integer, String> palette = new HashMap<>();
        for (Map.Entry<String, Tag<?,?>> entry : paletteTag.entrySet()) {
            palette.put(((IntTag) entry.getValue()).getValue(), entry.getKey());
        }

        // 2. Data is inside "Blocks"
        byte[] blockDataBytes = ((ByteArrayTag) blocksMap.get("Data")).getValue();

        // Sponge uses VarInts packed into a ByteArray
        int[] indices = readVarIntArray(blockDataBytes, width * height * length);

        List<SchematicBlock> blockList = new ArrayList<>();

        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    int index = (y * length + z) * width + x;
                    if (index >= indices.length) continue;

                    int paletteId = indices[index];
                    String rawString = palette.getOrDefault(paletteId, "minecraft:air");

                    BlockStringResult result = parseBlockString(rawString);
                    blockList.add(new SchematicBlock(x, y, z, result.getId(), result.getProperties()));
                }
            }
        }
        return new SchematicData(width, height, length, SchematicFormat.SPONGE_V3, blockList);
    }

    // --- PARSER: LEGACY ---
    private static SchematicData parseLegacy(Map<String, Tag<?,?>> data) {
        int width = ((ShortTag) data.get("Width")).getValue();
        int height = ((ShortTag) data.get("Height")).getValue();
        int length = ((ShortTag) data.get("Length")).getValue();

        byte[] blocks = ((ByteArrayTag) data.get("Blocks")).getValue();
        byte[] meta = data.containsKey("Data")
                ? ((ByteArrayTag) data.get("Data")).getValue()
                : new byte[blocks.length];

        List<SchematicBlock> blockList = new ArrayList<>();

        // Standard Legacy Order: Y -> Z -> X
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    int index = (y * length + z) * width + x;

                    // Convert signed byte to unsigned int
                    int id = blocks[index] & 0xFF;
                    int dataVal = meta[index] & 0xF;

                    SchematicBlock schematicBlock = legacyRegistry.get(id, dataVal);
                    schematicBlock.setX(x);
                    schematicBlock.setY(y);
                    schematicBlock.setZ(z);
                    blockList.add(schematicBlock);
                }
            }
        }
        return new SchematicData(width, height, length, SchematicFormat.LEGACY, blockList);
    }

    // --- PARSER: SPONGE V2 (Fallback) ---
    private static SchematicData parseSpongeV2(Map<String, Tag<?,?>> data) {
        int width = ((ShortTag) data.get("Width")).getValue();
        int height = ((ShortTag) data.get("Height")).getValue();
        int length = ((ShortTag) data.get("Length")).getValue();

        Map<String, Tag<?,?>> paletteTag = ((CompoundTag) data.get("Palette")).getValue();
        Map<Integer, String> palette = new HashMap<>();
        for (Map.Entry<String, Tag<?,?>> entry : paletteTag.entrySet()) {
            palette.put(((IntTag) entry.getValue()).getValue(), entry.getKey());
        }

        // V2 usually calls it "BlockData", V3 calls it "Data"
        String key = data.containsKey("BlockData") ? "BlockData" : "Data";
        byte[] blockDataBytes = ((ByteArrayTag) data.get(key)).getValue();
        int[] indices = readVarIntArray(blockDataBytes, width * height * length);

        List<SchematicBlock> blockList = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    int index = (y * length + z) * width + x;
                    if (index >= indices.length) continue;

                    BlockStringResult result = parseBlockString(
                            palette.getOrDefault(indices[index], "minecraft:air")
                    );
                    blockList.add(new SchematicBlock(x, y, z, result.getId(), result.getProperties()));
                }
            }
        }
        return new SchematicData(width, height, length, SchematicFormat.SPONGE_V2, blockList);
    }

    private static BlockStringResult parseBlockString(String fullString) {
        if (!fullString.contains("[")) {
            return new BlockStringResult(fullString, new HashMap<>());
        }

        String id = fullString.substring(0, fullString.indexOf("["));
        String propsRaw = fullString.substring(fullString.indexOf("[") + 1, fullString.indexOf("]"));

        HashMap<String, String> props = new HashMap<>();
        for (String prop : propsRaw.split(",")) {
            String[] parts = prop.split("=");
            if (parts.length == 2) {
                props.put(parts[0], parts[1]);
            } else {
                props.put(prop, "true");
            }
        }
        return new BlockStringResult(id, props);
    }

    // --- HELPER: Decode VarInt Stream ---
    private static int[] readVarIntArray(byte[] buf, int expectedSize) {
        int[] result = new int[expectedSize];
        int index = 0;
        int i = 0;

        while (i < buf.length && index < expectedSize) {
            int value = 0;
            int shift = 0;

            while (true) {
                int b = buf[i++] & 0xFF;
                value = value | ((b & 0x7F) << shift);
                if ((b & 0x80) == 0) break;
                shift += 7;
            }
            result[index++] = value;
        }
        return result;
    }
}