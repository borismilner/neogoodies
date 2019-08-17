package testing;

import java.util.List;
import java.util.Map;

public class GraphYamlTemplate {
    public String name;
    public String comments;
    public List<Map<String, Map<String, Object>>> nodes;
    public List<Map<String, Object>> relationships;
    public List<Map<String, Map<String, String>>> customProperties;

//    public GraphYamlTemplate(String name, String comments, List<Map<String, NodeDetails>> nodes, RelationsDetails relationships, List<Map<String, Map<String, String>>> customProperties) {
//        this.name = name;
//        this.comments = comments;
//        this.nodes = nodes;
//        this.relationships = relationships;
//        this.customProperties = customProperties;
//    }
}
