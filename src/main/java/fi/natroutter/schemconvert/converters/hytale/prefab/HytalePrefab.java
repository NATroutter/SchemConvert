package fi.natroutter.schemconvert.converters.hytale.prefab;

import com.google.gson.GsonBuilder;
import fi.natroutter.foxlib.FoxLib;
import fi.natroutter.foxlib.files.FileUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class HytalePrefab {
    public List<HytaleBlock> blocks;


    public void save(String fileName, File output_dir) throws IOException {
        if (fileName == null || fileName.isBlank()) {
            fileName = "invalid_name_"+ UUID.randomUUID();
        }
        fileName = fileName + ".prefab.json";

        File outputFile = new File(output_dir, fileName);
        try (FileWriter writer = new FileWriter(outputFile)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(this, writer);
        }
    }

}
