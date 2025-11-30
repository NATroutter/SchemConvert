package fi.natroutter.schemconvert.converters.minecraft.schematic;

import fi.natroutter.foxlib.FoxLib;
import fi.natroutter.foxlib.files.FileUtils;
import fi.natroutter.foxlib.logger.FoxLogger;
import fi.natroutter.schemconvert.SchemConvert;
import fi.natroutter.schemconvert.converters.ConversionResult;
import fi.natroutter.schemconvert.converters.IConverter;
import fi.natroutter.schemconvert.converters.UniBlock;
import fi.natroutter.schemconvert.converters.minecraft.LegacyRegistry;
import fi.natroutter.schemconvert.converters.minecraft.schematic.data.BlockStringResult;
import fi.natroutter.schemconvert.converters.minecraft.schematic.data.SchematicBlock;
import fi.natroutter.schemconvert.converters.minecraft.schematic.data.SchematicData;
import fi.natroutter.schemconvert.converters.minecraft.schematic.data.SchematicFormat;
import fi.natroutter.schemconvert.mappings.Mapping;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

import com.sk89q.jnbt.*;

public class SchematicConverter implements IConverter {

    private static FoxLogger logger = SchemConvert.getLogger();
    private static LegacyRegistry legacyRegistry = SchemConvert.getLegacyRegistry();


    @Override
    public ConversionResult convertSingle(File input_file, File output_dir, Mapping mapping, Consumer<Float> currentProgress) {

        SchematicData schematic = loadSchematic(input_file, currentProgress);
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


    private static SchematicData loadSchematic(File file, Consumer<Float> progress) {
        try (NBTInputStream nbt = new NBTInputStream(new GZIPInputStream(new FileInputStream(file)))) {
            // Handle Generic Tag<?> Issue
            NamedTag rootNamed = nbt.readNamedTag();

            if (!(rootNamed.getTag() instanceof CompoundTag)) {
                throw new RuntimeException("Root is not a CompoundTag");
            }

            CompoundTag rootTag = (CompoundTag) rootNamed.getTag();
            Map<String, Tag<?, ?>> data = rootTag.getValue();

            // 1. Handle "Schematic" Wrapper
            if (data.containsKey("Schematic")) {
                data = ((CompoundTag) data.get("Schematic")).getValue();
            }

            // 2. ROUTING LOGIC based on Inspector Output
            Tag blocksTag = data.get("Blocks");

            if (blocksTag instanceof CompoundTag) {
                // "Blocks" is a Container -> It's Sponge V3
                logger.info("Parsing Schematic: " + FileUtils.getBasename(file) + " | Version: " + SchematicFormat.SPONGE_V3.name());
                return parseSpongeV3(data, (CompoundTag) blocksTag, progress);
            } else if (blocksTag instanceof ByteArrayTag) {
                // "Blocks" is a ByteArray -> It's Legacy
                logger.info("Parsing Schematic: " + FileUtils.getBasename(file) + " | Version: " + SchematicFormat.LEGACY.name());
                return parseLegacy(data, progress);
            } else if (data.containsKey("Palette")) {
                // "Palette" at top level -> Sponge V1/V2
                logger.info("Parsing Schematic: " + FileUtils.getBasename(file) + " | Version: " + SchematicFormat.SPONGE_V2.name());
                return parseSpongeV2(data, progress);
            }

            logger.error("Unknown Structure. Keys: " + data.keySet());
            return null;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // --- PARSER: SPONGE V2 ---
    private static SchematicData parseSpongeV2(Map<String, Tag<?, ?>> data, Consumer<Float> progress) {
        int width = ((ShortTag) data.get("Width")).getValue();
        int height = ((ShortTag) data.get("Height")).getValue();
        int length = ((ShortTag) data.get("Length")).getValue();

        // V2: Palette is at Root
        Map<String, Tag<?, ?>> palette = ((CompoundTag) data.get("Palette")).getValue();

        // V2: Data is at Root (named BlockData or Data)
        String key = data.containsKey("BlockData") ? "BlockData" : "Data";
        byte[] blockData = ((ByteArrayTag) data.get(key)).getValue();

        return parseSpongeCommon(width, height, length, palette, blockData, SchematicFormat.SPONGE_V2, progress);
    }

    // --- PARSER: SPONGE V3 ---
    private static SchematicData parseSpongeV3(Map<String, Tag<?, ?>> rootData, CompoundTag blocksContainer, Consumer<Float> progress) {
        // Dimensions are at Root
        int width = ((ShortTag) rootData.get("Width")).getValue();
        int height = ((ShortTag) rootData.get("Height")).getValue();
        int length = ((ShortTag) rootData.get("Length")).getValue();

        Map<String, Tag<?, ?>> blocksMap = blocksContainer.getValue();

        // V3: Palette is inside "Blocks" container
        Map<String, Tag<?, ?>> palette = ((CompoundTag) blocksMap.get("Palette")).getValue();

        // V3: Data is inside "Blocks" container
        byte[] blockData = ((ByteArrayTag) blocksMap.get("Data")).getValue();

        return parseSpongeCommon(width, height, length, palette, blockData, SchematicFormat.SPONGE_V3, progress);
    }

    private static SchematicData parseSpongeCommon(int width, int height, int length,
                                                   Map<String, Tag<?, ?>> paletteTag,
                                                   byte[] blockDataBytes,
                                                   SchematicFormat format,
                                                   Consumer<Float> progress) {

        // 1. PALETTE CACHING
        Map<Integer, BlockStringResult> paletteCache = new HashMap<>();
        int airPaletteId = -1;

        for (Map.Entry<String, Tag<?, ?>> entry : paletteTag.entrySet()) {
            int id = ((IntTag) entry.getValue()).getValue();
            String blockRaw = entry.getKey();

            // Helper method handles caching internally now
            BlockStringResult parsed = parseBlockString(blockRaw);
            paletteCache.put(id, parsed);

            if (blockRaw.contains("minecraft:air")) {
                airPaletteId = id;
            }
        }

        // 2. SETUP PROGRESS & LOOP
        long total = (long) width * height * length;
        List<SchematicBlock> blockList = new ArrayList<>();

        int byteIndex = 0;
        long currentBlockIndex = 0;
        long onePercentStep = Math.max(1, total / 100);
        long nextThreshold = onePercentStep;

        progress.accept(0.0f);

        // 3. MAIN LOOP (Optimized for V2/V3)
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {

                    // Progress Update (Fast Int Math)
                    currentBlockIndex++;
                    if (currentBlockIndex >= nextThreshold) {
                        progress.accept((float) ((double) currentBlockIndex / total));
                        nextThreshold += onePercentStep;
                    }

                    // Safety Break
                    if (byteIndex >= blockDataBytes.length) break;

                    // Inline VarInt Reading
                    int paletteId = 0;
                    int shift = 0;
                    while (true) {
                        byte b = blockDataBytes[byteIndex++];
                        paletteId |= (b & 0x7F) << shift;
                        if ((b & 0x80) == 0) break;
                        shift += 7;
                    }

                    // Skip Air
                    if (paletteId == airPaletteId) continue;

                    // Add Block
                    BlockStringResult result = paletteCache.get(paletteId);
                    if (result != null) {
                        blockList.add(new SchematicBlock(x, y, z, result.getId(), result.getProperties()));
                    }
                }
            }
        }

        progress.accept(1.0f);
        return new SchematicData(width, height, length, format, blockList);
    }

    private static SchematicData parseLegacy(Map<String, Tag<?, ?>> data, Consumer<Float> progress) {
        int width = ((ShortTag) data.get("Width")).getValue();
        int height = ((ShortTag) data.get("Height")).getValue();
        int length = ((ShortTag) data.get("Length")).getValue();

        byte[] blocks = ((ByteArrayTag) data.get("Blocks")).getValue();

        // Optimization: Flag check instead of empty array allocation
        boolean hasMeta = data.containsKey("Data");
        byte[] meta = hasMeta ? ((ByteArrayTag) data.get("Data")).getValue() : null;

        List<SchematicBlock> blockList = new ArrayList<>();
        long total = (long) width * height * length;

        long currentBlockIndex = 0;
        long onePercentStep = Math.max(1, total / 100);
        long nextThreshold = onePercentStep;

        // Linear index is faster than (y*len+z)*width+x inside loop
        int index = 0;

        progress.accept(0.0f);

        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {

                    // Progress Update
                    currentBlockIndex++;
                    if (currentBlockIndex >= nextThreshold) {
                        progress.accept((float) ((double) currentBlockIndex / total));
                        nextThreshold += onePercentStep;
                    }

                    int id = blocks[index] & 0xFF;

                    // Optimization: Fast Skip Air (Legacy 0 is always Air)
                    if (id == 0) {
                        index++;
                        continue;
                    }

                    int dataVal = hasMeta ? (meta[index] & 0xF) : 0;
                    SchematicBlock template = legacyRegistry.get(id, dataVal);

                    if (template != null) {
                        SchematicBlock sb = template.clone();
                        sb.setX(x);
                        sb.setY(y);
                        sb.setZ(z);
                        blockList.add(sb);
                    }

                    index++;
                }
            }
        }

        progress.accept(1.0f);
        return new SchematicData(width, height, length, SchematicFormat.LEGACY, blockList);
    }

    private static final Map<String, BlockStringResult> BLOCK_CACHE = new HashMap<>();

    public static BlockStringResult parseBlockString(String fullString) {
        // Check Cache
        BlockStringResult cached = BLOCK_CACHE.get(fullString);
        if (cached != null) {
            return cached;
        }

        int bracketIndex = fullString.indexOf('[');

        // Case 1: Simple Block (e.g., "minecraft:stone")
        if (bracketIndex == -1) {
            // Optimization: Use new HashMap() if your constructor requires it.
            // If you change constructor to Map<String, String>, use Collections.emptyMap()
            BlockStringResult result = new BlockStringResult(fullString, new HashMap<>());
            BLOCK_CACHE.put(fullString, result);
            return result;
        }

        // Case 2: Complex Block (e.g., "minecraft:wool[color=red]")
        String id = fullString.substring(0, bracketIndex);

        // Extract inner string: "color=red,type=hard"
        String propsRaw = fullString.substring(bracketIndex + 1, fullString.length() - 1);

        // Call extracted method
        HashMap<String, String> props = parseProperties(propsRaw);

        BlockStringResult result = new BlockStringResult(id, props);
        BLOCK_CACHE.put(fullString, result);
        return result;
    }

    // --- Extracted Helper Method ---
    private static HashMap<String, String> parseProperties(String propsRaw) {
        HashMap<String, String> props = new HashMap<>();

        int len = propsRaw.length();
        int start = 0;

        while (start < len) {
            // Find end of current property (comma or end of string)
            int end = propsRaw.indexOf(',', start);
            if (end == -1) {
                end = len;
            }

            // Find separator (=) inside this segment
            int eqIndex = -1;
            // Optimization: iterate only the current segment, not whole string
            for (int i = start; i < end; i++) {
                if (propsRaw.charAt(i) == '=') {
                    eqIndex = i;
                    break;
                }
            }

            if (eqIndex != -1) {
                // Standard "key=value"
                String key = propsRaw.substring(start, eqIndex);
                String val = propsRaw.substring(eqIndex + 1, end);
                props.put(key, val);
            } else {
                // Edge case "key" (implies =true)
                String key = propsRaw.substring(start, end);
                props.put(key, "true");
            }

            start = end + 1;
        }

        return props;
    }
}