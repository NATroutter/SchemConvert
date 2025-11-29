package fi.natroutter.schemconvert.converters;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;

@Data
@AllArgsConstructor
public class UniBlock {
    private int x;
    private int y;
    private int z;
    private String material;
    private HashMap<String, String> properties;
}
