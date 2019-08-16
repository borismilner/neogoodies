package neo_results;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.List;

public class GraphResult {
    private final List<Node> nodes;

    private final List<Relationship> relationships;

    public GraphResult(List<Node> nodes, List<Relationship> relationships) {
        this.nodes = nodes;
        this.relationships = relationships;
    }
}
