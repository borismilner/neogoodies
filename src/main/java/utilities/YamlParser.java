package utilities;

import graph_components.Property;
import org.yaml.snakeyaml.Yaml;

import java.util.*;

public class YamlParser {
    private final Yaml yaml = new Yaml();

    public List<Property> parseProperties(String yamlPropertiesString) {
        Map<String, Object> values = yaml.load(yamlPropertiesString);
        List<Property> propertiesFromString = new ArrayList<>();

        values.forEach((key, value) -> {
            if (value instanceof String) {
                propertiesFromString.add(new Property(key, String.valueOf(value)));
            } else if (value instanceof LinkedHashMap) {
                propertiesFromString.add(fromMap(key, value));
            }
        });


        return propertiesFromString;
    }

    private static Property fromMap(String propertyKey, Object map) {

        LinkedHashMap<String, Object> prop = (LinkedHashMap<String, Object>) map;

        String key = prop.keySet().iterator().next();

        if (prop.get(key) instanceof List) {
            return new Property(propertyKey, key, (List<Object>) prop.get(key));
        }

        throw new IllegalArgumentException("Invalid format");
    }

}
