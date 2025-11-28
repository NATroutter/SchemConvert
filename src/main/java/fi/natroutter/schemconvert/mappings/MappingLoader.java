package fi.natroutter.schemconvert.mappings;

import fi.natroutter.foxlib.files.DirectoryManager;
import fi.natroutter.foxlib.logger.FoxLogger;
import fi.natroutter.schemconvert.SchemConvert;
import lombok.Getter;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MappingLoader {

    @Getter
    private boolean initialized = false;

    private FoxLogger logger = SchemConvert.getLogger();
    private DirectoryManager manager;

    @Getter
    private ConcurrentHashMap<String, Mapping> mappings = new ConcurrentHashMap<>();

    public String[] keys() {
        return mappings.keySet().toArray(new String[0]);
    }

    public String[] names() {
        return mappings.keySet().stream().map(k -> k.replace(".json", "")).toArray(String[]::new);
    }

    public Mapping getMappingByIndex(int index) {
        return mappings.get(keys()[index]);
    }

    public MappingLoader() {
        File mappingsDir = Path.of(System.getProperty("user.dir"), "mappings").toFile();

        //Load custom mapping configuration!
        manager = new DirectoryManager.Builder(mappingsDir)
                 .setLogger(logger)
                 .setReadFilesInDirectory(true)
                 .setAllowedExtensions(List.of("json"))
                 .setExportingFiles(List.of("mappings/default.json"))
                 .onFileRead(file -> {
                     if (file.success() && !file.content().isEmpty()) {
                         mappings.put(file.name(), Mapping.fromJson(file.content()));
                     }
                 })
                 .build();

    }

    public void reload() {
        mappings.clear();
        manager.reload();
    }

}
