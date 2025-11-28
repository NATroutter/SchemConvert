package fi.natroutter.schemconvert.mappings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class Mapping {

    @Getter
    private List<Entry> entries;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Entry {
        private String mc_material;
        private String hy_material;
        private List<Condition> conditions;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Condition {
        private String condition;

        @SerializedName("new")
        private String newProperties;
    }

    public static Mapping fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, Mapping.class);
    }

    public String toJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }

}
