package graph_generator

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.neo4j.graphdb.Label
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.Relationship
import tools.GraphGenerator
import utilities.EmbeddedServerHelper
import utilities.FakeGenerator
import utilities.ValueFaker
import utilities.YamlParser
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GraphGeneratorTests {
    private var graphGenerator: GraphGenerator? = null

    private val fullNamePropertyName = "full_name"
    private val identifierPropertyName = "Identifier"

    private val identifiesRelationship = "IDENTIFIES"
    private val friendOfRelationship = "FRIEND_OF"

    private val personLabelString = "Person"


    @BeforeAll
    fun beforeAll() {
        val embeddedServer = EmbeddedServerHelper.graphDb
        val yamlParser = YamlParser()
        val valueFaker = ValueFaker()
        graphGenerator = GraphGenerator(embeddedServer, yamlParser, valueFaker)
    }

    @BeforeEach
    fun setUp() {
        EmbeddedServerHelper.clearGraph()
    }

    @AfterEach
    fun tearDown() {
        EmbeddedServerHelper.clearGraph()
    }

    @Test
    fun testGenerateNodes() {
        val howManyNodesToCreate = 10
        val expectedLabelsForEachNode = arrayOf("Mario", "Luigi")
        val nodes = graphGenerator!!.generateNodes(
                GraphGenerator.labelsFromStrings(expectedLabelsForEachNode),
                "{'$fullNamePropertyName':'${FakeGenerator.FULLNAME}'}",
                howManyNodesToCreate.toLong()
        )
        assertThat<Node>(nodes).hasSize(howManyNodesToCreate)
        val transaction = graphGenerator!!.database.beginTx()
        nodes.forEach { node ->
            for (expectedLabelName in expectedLabelsForEachNode) {
                assertThat(node.hasLabel(Label.label(expectedLabelName))).isTrue()
            }
            assertThat(node.hasProperty(fullNamePropertyName)).isTrue()
        }
        transaction.success()
    }

    @Test
    fun testGenerateZippedNodes() {
        val howManyNodesToCreate = 10
        val people = graphGenerator!!.generateNodes(GraphGenerator.labelsFromStrings(arrayOf(personLabelString)), String.format("{'%s':'%s'}", fullNamePropertyName, FakeGenerator.FULLNAME), howManyNodesToCreate.toLong())
        val identifiers = graphGenerator!!.generateNodes(GraphGenerator.labelsFromStrings(arrayOf(identifierPropertyName)), String.format("{'%s':'%s'}", identifierPropertyName, FakeGenerator.CREDIT_CARD_NUMBER), howManyNodesToCreate.toLong())
        val createdRelationships = graphGenerator!!.generateRelationshipsZipper(identifiers, people, identifiesRelationship, String.format("{'strength': '%s'}", FakeGenerator.RANDOM_NUMBER))
        assertThat<Node>(people).hasSize(howManyNodesToCreate)
        assertThat<Node>(identifiers).hasSize(howManyNodesToCreate)
        assertThat<Relationship>(createdRelationships).hasSize(howManyNodesToCreate)
        val transaction = graphGenerator!!.database.beginTx()
        createdRelationships.forEach { relationship ->
            val fromNode = relationship.startNode
            val toNode = relationship.endNode
            val indexInIdentifiers = identifiers.indexOf(fromNode)
            val indexInPeople = people.indexOf(toNode)
            assertThat(indexInIdentifiers).isEqualTo(indexInPeople)
        }
        transaction.success()
    }

    @Test
    fun testGenerateRelationshipsFromAllToAll() {
        val howManyNodesToCreate = 10
        val people = graphGenerator!!.generateNodes(GraphGenerator.labelsFromStrings(arrayOf("Person")), String.format("{'%s':'%s'}", fullNamePropertyName, FakeGenerator.FULLNAME), howManyNodesToCreate.toLong())
        val identifiers = graphGenerator!!.generateNodes(GraphGenerator.labelsFromStrings(arrayOf(identifierPropertyName)), String.format("{'%s':'%s'}", identifierPropertyName, FakeGenerator.CREDIT_CARD_NUMBER), howManyNodesToCreate.toLong())
        val createdRelationships = graphGenerator!!.generateRelationshipsFromAllToAll(identifiers, people, identifiesRelationship, String.format("{'strength': '%s'}", FakeGenerator.RANDOM_NUMBER))
        assertThat<Node>(people).hasSize(howManyNodesToCreate)
        assertThat<Node>(identifiers).hasSize(howManyNodesToCreate)
        assertThat<Relationship>(createdRelationships).hasSize(howManyNodesToCreate * howManyNodesToCreate)
    }

    @Test
    fun testGenerateLinkedList() {
        val howManyNodesToCreate = 10
        val people = graphGenerator!!.generateNodes(GraphGenerator.labelsFromStrings(arrayOf("Person")), String.format("{'%s':'%s'}", fullNamePropertyName, FakeGenerator.FULLNAME), howManyNodesToCreate.toLong())
        val (nodes, relationships) = graphGenerator!!.generateLinkedList(people, friendOfRelationship)
        assertThat<Node>(nodes).hasSize(howManyNodesToCreate)
        assertThat<Relationship>(relationships).hasSize(howManyNodesToCreate - 1)
    }

    @Test
    fun testGenerateValues() {
        val howManyProperties: Long = 10
        val parametersForGenerator = ArrayList<Any>()
        parametersForGenerator.add("1")
        parametersForGenerator.add("10")
        val numberBetween = graphGenerator!!.generateValues(FakeGenerator.NUMBER_BETWEEN.name, parametersForGenerator, howManyProperties)
        assertThat(numberBetween).hasSize(howManyProperties.toInt())
        for (property in numberBetween) {
            val value = property as Int
            assertThat(value).isBetween(Integer.valueOf(parametersForGenerator[0] as String), Integer.valueOf(parametersForGenerator[1] as String))
        }
    }

    @Test
    fun testGenerateFromYamlFile() {
        graphGenerator!!.generateFromYamlFile("graph_samples/sample_graph.yaml")
    }
}
