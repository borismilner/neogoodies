package plugin;

import graph_generator.GraphGenerator;
import neo_results.NodeListResult;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

public class GenerateNodesProcedure extends PluginProcedure {
    @Context
    public Log log;

    @Procedure(value = "generate.nodes", mode = Mode.WRITE)
    public Stream<NodeListResult> generateNodes(@Name("howMany") Long howMany,
                                                @Name("labels") Object labelsStringArray,
                                                @Name("propertiesYamlString") String propertiesYamlString) {
        String[] labels = (String[]) ((ArrayList) labelsStringArray).toArray(new String[0]);
        log.info(String.format("Generating %d nodes with labels %s and properties %s",
                howMany.intValue(),
                Arrays.toString(labels),
                propertiesYamlString));
        GraphGenerator graphGenerator = getGraphGenerator();
        return Stream.of(new NodeListResult(graphGenerator.generateNodes(
                GraphGenerator.labelsFromStrings(labels),
                propertiesYamlString,
                howMany)));
    }
}
