package testing;

import java.util.List;

class NodeStructure {
    String type;
    int howMany;
    String idProperty;
    List<String> properties;
    boolean createIdNode;
}

class GraphJsonTemplate {
    String graphName;
    List<NodeStructure> nodes;
    List<String> edges;
    String comments;
    List<String> customProperties;
}
