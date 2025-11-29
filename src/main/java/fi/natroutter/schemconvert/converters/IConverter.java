package fi.natroutter.schemconvert.converters;

import fi.natroutter.schemconvert.converters.hytale.prefab.HytalePrefab;
import fi.natroutter.schemconvert.mappings.Mapping;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface IConverter {
    ConversionResult convertSingle(File input_files, File output_dir, Mapping mapping, Consumer<Float> progress);

    default List<ConversionResult> convertMultiple(List<File> input_files, File output_dir, Mapping mapping, Consumer<Float> progress){
        List<ConversionResult> results = new ArrayList<>();
        for (File file : input_files) {
            results.add(convertSingle(file, output_dir, mapping, progress));
        }
        return results;
    }
}
