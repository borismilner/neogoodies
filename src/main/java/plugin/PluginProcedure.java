package plugin;

import graph_generator.GraphGenerator;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.procedure.Context;
import utilities.ValueFaker;
import utilities.YamlParser;

public class PluginProcedure {
    @Context
    public GraphDatabaseService database;

    GraphGenerator getGraphGenerator() {
        YamlParser yamlParser = new YamlParser();
        ValueFaker valueFaker = new ValueFaker();
        return new GraphGenerator(database, yamlParser, valueFaker);
    }
}
