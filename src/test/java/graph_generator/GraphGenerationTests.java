package graph_generator;

import org.junit.jupiter.api.*;
import org.neo4j.graphdb.*;
import structures.GraphResult;
import testing.EmbeddedServerHelper;
import utilities.FakeGenerator;
import utilities.ValueFaker;
import utilities.YamlParser;

import java.util.ArrayList;
import java.util.List;

import static graph_generator.GraphGenerator.labelsFromStrings;
import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GraphGenerationTests {
    private GraphGenerator graphGenerator;

    private static final String FULL_NAME_PROPERTY_NAME = "full_name";
    private static final String IDENTIFIER_PROPERTY_NAME = "Identifier";

    private static final String IDENTIFIES_RELATIONSHIP = "IDENTIFIES";
    private static final String FRIEND_OF_RELATIONSHIP = "FRIEND_OF";

    private static final String PERSON_LABEL_STRING = "Person";


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

    @Test
    void testGenerateNodes() {
        int howManyNodesToCreate = 10;
        String[] expectedLabelsForEachNode = new String[]{"Mario", "Luigi"};
        List<Node> nodes = graphGenerator.generateNodes(labelsFromStrings(expectedLabelsForEachNode), String.format("{'%s':'%s'}", FULL_NAME_PROPERTY_NAME, FakeGenerator.FULLNAME), howManyNodesToCreate);
        assertThat(nodes).hasSize(howManyNodesToCreate);
        Transaction transaction = graphGenerator.beginTransaction();
        nodes.forEach(node -> {
            for (String expectedLabelName : expectedLabelsForEachNode) {
                assertThat(node.hasLabel(Label.label(expectedLabelName))).isTrue();
            }
            assertThat(node.hasProperty(FULL_NAME_PROPERTY_NAME)).isTrue();
        });
        transaction.success();
    }

    @Test
    void testGenerateZippedNodes() {
        int howManyNodesToCreate = 10;
        List<Node> people = graphGenerator.generateNodes(labelsFromStrings(new String[]{PERSON_LABEL_STRING}), String.format("{'%s':'%s'}", FULL_NAME_PROPERTY_NAME, FakeGenerator.FULLNAME), howManyNodesToCreate);
        List<Node> identifiers = graphGenerator.generateNodes(labelsFromStrings(new String[]{IDENTIFIER_PROPERTY_NAME}), String.format("{'%s':'%s'}", IDENTIFIER_PROPERTY_NAME, FakeGenerator.CREDIT_CARD_NUMBER), howManyNodesToCreate);
        List<Relationship> createdRelationships = graphGenerator.generateRelationshipsZipper(identifiers, people, IDENTIFIES_RELATIONSHIP, String.format("{'strength': '%s'}", FakeGenerator.RANDOM_NUMBER));
        assertThat(people).hasSize(howManyNodesToCreate);
        assertThat(identifiers).hasSize(howManyNodesToCreate);
        assertThat(createdRelationships).hasSize(howManyNodesToCreate);
        Transaction transaction = graphGenerator.beginTransaction();
        createdRelationships.forEach(relationship -> {
            Node fromNode = relationship.getStartNode();
            Node toNode = relationship.getEndNode();
            int indexInIdentifiers = identifiers.indexOf(fromNode);
            int indexInPeople = people.indexOf(toNode);
            assertThat(indexInIdentifiers).isEqualTo(indexInPeople);
        });
        transaction.success();
    }

    @Test
    void testGenerateRelationshipsFromAllToAll() {
        int howManyNodesToCreate = 10;
        List<Node> people = graphGenerator.generateNodes(labelsFromStrings(new String[]{"Person"}), String.format("{'%s':'%s'}", FULL_NAME_PROPERTY_NAME, FakeGenerator.FULLNAME), howManyNodesToCreate);
        List<Node> identifiers = graphGenerator.generateNodes(labelsFromStrings(new String[]{IDENTIFIER_PROPERTY_NAME}), String.format("{'%s':'%s'}", IDENTIFIER_PROPERTY_NAME, FakeGenerator.CREDIT_CARD_NUMBER), howManyNodesToCreate);
        List<Relationship> createdRelationships = graphGenerator.generateRelationshipsFromAllToAll(identifiers, people, IDENTIFIES_RELATIONSHIP, String.format("{'strength': '%s'}", FakeGenerator.RANDOM_NUMBER));
        assertThat(people).hasSize(howManyNodesToCreate);
        assertThat(identifiers).hasSize(howManyNodesToCreate);
        assertThat(createdRelationships).hasSize(howManyNodesToCreate * howManyNodesToCreate);
    }

    @Test
    void testGenerateLinkedList() {
        int howManyNodesToCreate = 10;
        List<Node> people = graphGenerator.generateNodes(labelsFromStrings(new String[]{"Person"}), String.format("{'%s':'%s'}", FULL_NAME_PROPERTY_NAME, FakeGenerator.FULLNAME), howManyNodesToCreate);
        GraphResult graphResult = graphGenerator.generateLinkedList(people, FRIEND_OF_RELATIONSHIP);
        assertThat(graphResult.getNodes()).hasSize(howManyNodesToCreate);
        assertThat(graphResult.getRelationships()).hasSize(howManyNodesToCreate - 1);
    }

    @Test
    void testGenerateValues() {
        long howManyProperties = 10;
        List<Object> parametersForGenerator = new ArrayList<>();
        parametersForGenerator.add("1");
        parametersForGenerator.add("10");
        List<Object> numberBetween = graphGenerator.generateValues(FakeGenerator.NUMBER_BETWEEN.name(), parametersForGenerator, howManyProperties);
        assertThat(numberBetween).hasSize((int) howManyProperties);
        for (Object property : numberBetween) {
            int value = (int) property;
            assertThat(value).isBetween(Integer.valueOf((String) parametersForGenerator.get(0)), Integer.valueOf((String) parametersForGenerator.get(1)));
        }
    }

    @Test
    void testGenerateFromYamlFile() {
        graphGenerator.generateFromYamlFile("graph_samples/sample_graph.yaml");
    }
}
