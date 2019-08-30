package graph_generator

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.neo4j.graphdb.Label
import org.neo4j.graphdb.Node
import plugin.*
import tools.GraphGenerator
import utilities.EmbeddedServerHelper
import utilities.TestUtilities.registerProcedure
import utilities.TestUtilities.testCall
import utilities.ValueFaker
import utilities.YamlParser
import java.util.function.Consumer


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PluginProcedureTests {

    private val howManyToCreate = 10
    private val labelsForEachNode = arrayListOf("Officer", "Gentleman")
    private val propertiesForEachNode = mapOf(
            Pair("first_name", "FIRSTNAME"),
            Pair("last_name", "LASTNAME"),
            Pair("country", "COUNTRY"),
            Pair("zip_code", "ZIP_CODE"),
            Pair("credit_card_type", "CREDIT_CARD_TYPE"),
            Pair("credit_card_number", "CREDIT_CARD_NUMBER"),
            Pair("phone_number", "PHONE_NUMBER")
    )

    private val relationshipType = "LIKES"

    private val nodesKey = "nodes"
    private val valuesKey = "values"
    private val relationshipsKey = "relationships"

    private val propertiesString: String

    init {
        val stringBuilder = StringBuilder("{")
        for ((propertyName, generatorName) in propertiesForEachNode) {
            stringBuilder.append("'$propertyName':'$generatorName',")
        }
        stringBuilder.deleteCharAt(stringBuilder.length - 1).append("}")
        propertiesString = stringBuilder.toString()
    }

    private lateinit var graphGenerator: GraphGenerator

    @BeforeAll
    fun beforeAll() {
        val embeddedServer = EmbeddedServerHelper.graphDb
        graphGenerator = GraphGenerator(embeddedServer, YamlParser(), ValueFaker())
        registerProcedure(
                embeddedServer,
                GenerateNodesProcedure::class.java,
                GenerateValuesProcedure::class.java,
                GenerateLinkedListProcedure::class.java,
                GenerateZipperProcedure::class.java,
                GenerateFromYamlFileProcedure::class.java)
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
    fun testGenerateNodesProcedure() {
        testCall(
                graphDb = EmbeddedServerHelper.graphDb,
                callQuery = "CALL generate.nodes({howMany},{labels},{propertiesYamlString})",
                parameters = mapOf(
                        Pair(first = "howMany", second = howManyToCreate),
                        Pair(first = "labels", second = labelsForEachNode),
                        Pair(first = "propertiesYamlString", second = propertiesString)
                ),
                resultsStreamConsumer = Consumer { result ->
                    val resultNodes = result[nodesKey] as ArrayList<Node>
                    assertThat(resultNodes.size).isEqualTo(howManyToCreate)
                    for (node: Node in resultNodes) {
                        for (label: String in labelsForEachNode) {
                            assertThat(node.hasLabel(Label.label(label)))
                            for ((propertyName, _) in propertiesForEachNode) {
                                assertThat(node.hasProperty(propertyName))
                            }
                        }
                    }
                }
        )
    }

    @Test
    fun testGenerateValuesProcedure() {
        for ((_, generatorName) in propertiesForEachNode) {
            testCall(
                    graphDb = EmbeddedServerHelper.graphDb,
                    callQuery = "CALL generate.values({howMany},{generatorName},{parameters})",
                    parameters = mapOf(
                            Pair(first = "howMany", second = howManyToCreate),
                            Pair(first = "generatorName", second = generatorName),
                            Pair(first = "parameters", second = arrayListOf(""))
                    ),
                    resultsStreamConsumer = Consumer { result ->
                        assertThat(result.keys).contains(valuesKey)
                        assertThat(result[valuesKey] as ArrayList<*>).hasSize(howManyToCreate)

                    }
            )
        }
    }

    @Test
    fun testGenerateLinkedListProcedure() {

        testCall(
                graphDb = EmbeddedServerHelper.graphDb,
                callQuery = "CALL generate.linkedList({howMany},{labels},{nodePropertiesString},{relationshipType})",
                parameters = mapOf(
                        Pair(first = "howMany", second = howManyToCreate),
                        Pair(first = "labels", second = labelsForEachNode),
                        Pair(first = "nodePropertiesString", second = propertiesString),
                        Pair(first = "relationshipType", second = relationshipType)
                ),
                resultsStreamConsumer = Consumer { result ->
                    assertThat(result.keys).contains(nodesKey)
                    assertThat(result.keys).contains(relationshipsKey)
                    assertThat(result[nodesKey] as ArrayList<*>).hasSize(howManyToCreate)
                    assertThat(result[relationshipsKey] as ArrayList<*>).hasSize(howManyToCreate - 1)
                }
        )
    }

    @Test
    fun testGenerateZipperProcedure() {
        testCall(
                graphDb = EmbeddedServerHelper.graphDb,
                callQuery = "CALL generate.zipper({howMany},{sourceLabel},{sourcePropertiesString},{targetLabel},{targetPropertiesString},{relationshipType},{relationshipProperties})",
                parameters = mapOf(
                        Pair(first = "howMany", second = howManyToCreate),
                        Pair(first = "sourceLabel", second = labelsForEachNode[0]),
                        Pair(first = "sourcePropertiesString", second = propertiesString),
                        Pair(first = "targetLabel", second = labelsForEachNode[1]),
                        Pair(first = "targetPropertiesString", second = propertiesString),
                        Pair(first = "relationshipType", second = relationshipType),
                        Pair(first = "relationshipProperties", second = propertiesString)
                ),
                resultsStreamConsumer = Consumer { result ->
                    assertThat(result.keys).contains(nodesKey)
                    assertThat(result.keys).contains(relationshipsKey)
                    assertThat(result[nodesKey] as ArrayList<*>).hasSize(howManyToCreate * 2)
                    assertThat(result[relationshipsKey] as ArrayList<*>).hasSize(howManyToCreate)
                }
        )
    }

    @Test
    fun testGenerateFromYamlFileProcedure() {
        testCall(
                graphDb = EmbeddedServerHelper.graphDb,
                callQuery = "CALL generate.fromYamlFile({yamlFilePath})",
                parameters = mapOf(
                        Pair(first = "yamlFilePath", second = "graph_samples/sample_graph.yaml")
                ),
                resultsStreamConsumer = Consumer { result ->
                    assertThat(result.keys).contains(valuesKey)
                    val resultsList = result[valuesKey] as ArrayList<String>
                    assertThat(resultsList).hasSize(1)
                    assertThat(resultsList[0]).isEqualTo("Done")
                }
        )

        // Make sure that the intended structure was indeed created

        val expectedNumberOfNodes = 60
        val expectedRelationshipTypes = arrayOf("FRIEND_OF", "OWNES", "IDENTIFIES", "LOOKS_LIKE")

        var results = EmbeddedServerHelper.execute("""match (n) return count(n) as numOfNodes""")

        val numberOfNodes = results.single()["numOfNodes"].asInt()
        assertThat(numberOfNodes).isEqualTo(expectedNumberOfNodes)

        results = EmbeddedServerHelper.execute("""match (n)-[r]-() return distinct type(r)""")
        assertThat(setOf(results) == setOf(expectedRelationshipTypes))

    }

}