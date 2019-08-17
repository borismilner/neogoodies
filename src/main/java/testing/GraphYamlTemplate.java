package testing;

import java.util.List;
import java.util.Map;

public class GraphYamlTemplate {
    public String name;
    public String comments;
    public List<Map<String, Map<String, Object>>> nodes;
    public List<Map<String, Object>> relationships;
    public List<Map<String, Map<String, String>>> customProperties;
}
