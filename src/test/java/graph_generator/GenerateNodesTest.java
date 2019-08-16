package graph_generator;

import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import testing.EmbeddedServerHelper;
import utilities.ValueFaker;
import utilities.YamlParser;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GenerateNodesTest {

    private Label[] labelsFromStrings(String[] labelNames) {
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
    void testGenerateNodes() {
        int howManyNodesToCreate = 10;
        String fullNameGenerator = "fullName";
        String fullNamePropertyName = "full_name";
        String[] expectedLabelsForEachNode = new String[]{"Crazy", "Person"};
        GraphDatabaseService embeddedServer = EmbeddedServerHelper.getEmbeddedServer();
        YamlParser yamlParser = new YamlParser();
        ValueFaker valueFaker = new ValueFaker();
        GraphGenerator graphGenerator = new GraphGenerator(embeddedServer, yamlParser, valueFaker);
        List<Node> nodes = graphGenerator.generateNodes(labelsFromStrings(expectedLabelsForEachNode), String.format("{'%s':'%s'}", fullNamePropertyName, fullNameGenerator), howManyNodesToCreate);
        assertThat(nodes).hasSize(howManyNodesToCreate);
        Transaction transaction = graphGenerator.beginTransaction();
        nodes.forEach(node -> {
            for (String expectedLabelName : expectedLabelsForEachNode) {
                assertThat(node.hasLabel(Label.label(expectedLabelName))).isTrue();
            }
            assertThat(node.hasProperty(fullNamePropertyName)).isTrue();
        });
        transaction.success();
    }
}
