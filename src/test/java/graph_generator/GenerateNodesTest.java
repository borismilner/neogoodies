package graph_generator;

import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import testing.EmbeddedServerHelper;
import utilities.ValueFaker;
import utilities.YamlParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class GenerateNodesTest {

    private Label[] labelsFromStrings(List<String> labelNames) {
        List<Label> nodeLabels = new ArrayList<>();
        for (String labelName : labelNames) {
            Label newLabel = Label.label(labelName);
            nodeLabels.add(newLabel);
        }
        Label[] labels = new Label[nodeLabels.size()];
        for (int i = 0; i < nodeLabels.size(); i++) {
            labels[i] = nodeLabels.get(i);
        }
        return labels;
    }

    @Test
    public void testGenerateNodes() {
        GraphDatabaseService embeddedServer = EmbeddedServerHelper.getEmbeddedServer();
        YamlParser yamlParser = new YamlParser();
        ValueFaker valueFaker = new ValueFaker();
        GraphGenerator graphGenerator = new GraphGenerator(embeddedServer, yamlParser, valueFaker);
        List<Node> nodes = graphGenerator.generateNodes(labelsFromStrings(Arrays.asList("Boris", "Aaaa")), "{name: fullName}", 10);
        assertThat(nodes).hasSize(10);
    }
}
