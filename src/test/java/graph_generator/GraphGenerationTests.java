package graph_generator;

import org.junit.jupiter.api.*;
import org.neo4j.graphdb.*;
import testing.EmbeddedServerHelper;
import utilities.ValueFaker;
import utilities.YamlParser;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GraphGenerationTests {
    private GraphGenerator graphGenerator;

    private final String fullNameGenerator = "fullName";
    private final String fullNamePropertyName = "full_name";

    private final String identifierPropertyName = "Identifier";
    private final String identifierGenerator = "creditCardNumber";
    private final String identifiesRelationship = "IDENTIFIES";

    private final String randomNumberGenerator = "randomNumber";


    @BeforeAll
    void beforeAll() {
        GraphDatabaseService embeddedServer = EmbeddedServerHelper.getEmbeddedServer();
        YamlParser yamlParser = new YamlParser();
        ValueFaker valueFaker = new ValueFaker();
        graphGenerator = new GraphGenerator(embeddedServer, yamlParser, valueFaker);
    }

    @BeforeEach
    void setUp() {
        EmbeddedServerHelper.clearGraph();
    }

    @AfterEach
    void tearDown() {
        EmbeddedServerHelper.clearGraph();
    }

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
        String[] expectedLabelsForEachNode = new String[]{"Mario", "Luigi"};
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

    @Test
    void testGenerateZipNodes() {
        int howManyNodesToCreate = 10;
        List<Node> people = graphGenerator.generateNodes(labelsFromStrings(new String[]{"Person"}), String.format("{'%s':'%s'}", fullNamePropertyName, fullNameGenerator), howManyNodesToCreate);
        List<Node> identifiers = graphGenerator.generateNodes(labelsFromStrings(new String[]{"Identifier"}), String.format("{'%s':'%s'}", identifierPropertyName, identifierGenerator), howManyNodesToCreate);
        List<Relationship> createdRelationships = graphGenerator.generateRelationshipsZipper(identifiers, people, identifiesRelationship, String.format("{'strength': '%s'}", randomNumberGenerator));
        assertThat(people).hasSize(howManyNodesToCreate);
        assertThat(identifiers).hasSize(howManyNodesToCreate);
        assertThat(createdRelationships).hasSize(howManyNodesToCreate);
        for (Relationship relationship : createdRelationships){
            Node fromNode = relationship.getStartNode();
            Node toNode = relationship.getEndNode();
            int indexInIdentifiers = identifiers.indexOf(fromNode);
            int indexInPeople = people.indexOf(toNode);
            assertThat(indexInIdentifiers).isEqualTo(indexInPeople);
        }
    }
}
