package graph_generator;

import org.neo4j.graphdb.GraphDatabaseService;

public class GraphGenerator {
    private final GraphDatabaseService database;

    public GraphGenerator(GraphDatabaseService database) {
        this.database = database;
    }
}
