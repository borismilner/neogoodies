package plugin;

import graph_generator.GraphGenerator;
import org.neo4j.graphdb.Node;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import structures.GraphResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class GenerateLinkedListProcedure extends PluginProcedure {
    @SuppressWarnings("WeakerAccess")
    @Context
    public Log log;

    @Procedure(value = "generate.linkedList", mode = Mode.WRITE)
    public Stream<GraphResult> generateNodes(@Name("howMany") Long howMany,
                                             @Name("labels") Object labelsStringArray,
                                             @Name("nodePropertiesString") String nodePropertiesString,
                                             @Name("relationshipType") String relationshipType) {
        String[] labels = (String[]) ((ArrayList) labelsStringArray).toArray(new String[0]);
        log.info(String.format("Generating a linked list of %d nodes with labels %s connected by %s",
                               howMany.intValue(),
                               Arrays.toString(labels),
                               relationshipType));
        GraphGenerator graphGenerator = getGraphGenerator();
        List<Node> nodesForLinkedList = graphGenerator.generateNodes(GraphGenerator.labelsFromStrings(labels), nodePropertiesString, howMany);
        GraphResult graphResult = graphGenerator.generateLinkedList(nodesForLinkedList, relationshipType);
        return Stream.of(graphResult);
    }
}
