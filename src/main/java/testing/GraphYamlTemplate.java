package testing;

import structures.EdgeDetails;
import structures.NodeDetails;
import structures.NodePropertiesDetails;

import java.util.List;

public class GraphYamlTemplate {
    public String name;
    public String comments;
    public List<NodeDetails> nodes;
    public List<EdgeDetails> relationships;
    public List<NodePropertiesDetails> customProperties;
}
