package fi.natroutter.schemconvert.converters;

import fi.natroutter.schemconvert.mappings.Mapping;

import java.io.File;
public interface IConverter {
    void convert(File[] input_files, File output_dir, Mapping mapping);
}
