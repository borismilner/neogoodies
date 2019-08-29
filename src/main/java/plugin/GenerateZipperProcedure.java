package plugin;

import graph_generator.GraphGenerator;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import structures.GraphResult;

import java.util.List;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class GenerateZipperProcedure extends PluginProcedure {
    @SuppressWarnings("WeakerAccess")
    @Context
    public Log log;

    @Procedure(value = "generate.zipper", mode = Mode.WRITE)
    public Stream<GraphResult> generateNodes(@Name("howMany") Long howMany,
                                             @Name("sourceLabel") String sourceLabelName,
                                             @Name("sourcePropertiesString") String sourcePropertiesString,
                                             @Name("targetLabel") String targetLabelName,
                                             @Name("targetPropertiesString") String targetPropertiesString,
                                             @Name("relationshipType") String relationshipType,
                                             @Name("relationshipProperties") String relationshipProperties) {

        log.info(String.format("Generating zipper structure of %d nodes in each side, source=%s, target=%s, relationship=%s, rel_properties:%s",
                               howMany.intValue(),
                               sourceLabelName,
                               targetLabelName,
                               relationshipType,
                               relationshipProperties));

        GraphGenerator graphGenerator = getGraphGenerator();
        Label sourceLabel = Label.label(sourceLabelName);
        Label targetLabel = Label.label(targetLabelName);
        List<Node> sourceNodes = graphGenerator.generateNodes(new Label[]{sourceLabel}, sourcePropertiesString, howMany.intValue());
        List<Node> targetNodes = graphGenerator.generateNodes(new Label[]{targetLabel}, targetPropertiesString, howMany.intValue());
        List<Relationship> relationships = graphGenerator.generateRelationshipsZipper(sourceNodes, targetNodes, relationshipType, relationshipProperties);
        sourceNodes.addAll(targetNodes);
        GraphResult graphResult = new GraphResult(sourceNodes, relationships);
        return Stream.of(graphResult);
    }
}
