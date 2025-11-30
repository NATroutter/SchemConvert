package fi.natroutter.schemconvert.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataStore {

    @SerializedName("input_path")
    private List<String> inputPath;

    @SerializedName("output_path")
    private String outputPath;

    @SerializedName("directory_mode")
    private boolean directoryMode;

    private String mapping;

    private int dumpMode;

    private int convertMode;

    public static DataStore fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, DataStore.class);
    }

    public String toJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }

}
