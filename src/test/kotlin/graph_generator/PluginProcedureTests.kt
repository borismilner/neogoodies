package graph_generator

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.neo4j.graphdb.Label
import org.neo4j.graphdb.Node
import plugin.*
import tools.GraphGenerator
import utilities.EmbeddedServerHelper
import utilities.TestUtil
import utilities.TestUtil.registerProcedure
import utilities.ValueFaker
import utilities.YamlParser


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
        registerProcedure(embeddedServer, GenerateNodesProcedure::class.java)
        registerProcedure(embeddedServer, GenerateValuesProcedure::class.java)
        registerProcedure(embeddedServer, GenerateLinkedListProcedure::class.java)
        registerProcedure(embeddedServer, GenerateZipperProcedure::class.java)
        registerProcedure(embeddedServer, GenerateFromYamlFileProcedure::class.java)
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
        TestUtil.testCall(
                EmbeddedServerHelper.graphDb, "CALL generate.nodes({howMany},{labels},{propertiesYamlString})",
                mapOf(
                        Pair(first = "howMany", second = howManyToCreate),
                        Pair(first = "labels", second = labelsForEachNode),
                        Pair(first = "propertiesYamlString", second = propertiesString)
                )
        ) { result ->
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
    }

    @Test
    fun testGenerateValuesProcedure() {
        for ((_, generatorName) in propertiesForEachNode) {
            TestUtil.testCall(
                    EmbeddedServerHelper.graphDb, "CALL generate.values({howMany},{generatorName},{parameters})",
                    mapOf(
                            Pair(first = "howMany", second = howManyToCreate),
                            Pair(first = "generatorName", second = generatorName),
                            Pair(first = "parameters", second = arrayListOf(""))
                    )
            ) { result ->
                assertThat(result.keys).contains(valuesKey)
                assertThat(result[valuesKey] as ArrayList<*>).hasSize(howManyToCreate)
            }
        }
    }

    @Test
    fun testGenerateLinkedListProcedure() {

        TestUtil.testCall(
                EmbeddedServerHelper.graphDb, "CALL generate.linkedList({howMany},{labels},{nodePropertiesString},{relationshipType})",
                mapOf(
                        Pair(first = "howMany", second = howManyToCreate),
                        Pair(first = "labels", second = labelsForEachNode),
                        Pair(first = "nodePropertiesString", second = propertiesString),
                        Pair(first = "relationshipType", second = relationshipType)
                )
        ) { result ->
            assertThat(result.keys).contains(nodesKey)
            assertThat(result.keys).contains(relationshipsKey)
            assertThat(result[nodesKey] as ArrayList<*>).hasSize(howManyToCreate)
            assertThat(result[relationshipsKey] as ArrayList<*>).hasSize(howManyToCreate - 1)
        }
    }

    @Test
    fun testGenerateZipperProcedure() {
        TestUtil.testCall(
                EmbeddedServerHelper.graphDb, "CALL generate.zipper({howMany},{sourceLabel},{sourcePropertiesString},{targetLabel},{targetPropertiesString},{relationshipType},{relationshipProperties})",
                mapOf(
                        Pair(first = "howMany", second = howManyToCreate),
                        Pair(first = "sourceLabel", second = labelsForEachNode[0]),
                        Pair(first = "sourcePropertiesString", second = propertiesString),
                        Pair(first = "targetLabel", second = labelsForEachNode[1]),
                        Pair(first = "targetPropertiesString", second = propertiesString),
                        Pair(first = "relationshipType", second = relationshipType),
                        Pair(first = "relationshipProperties", second = propertiesString)
                )
        ) { result ->
            assertThat(result.keys).contains(nodesKey)
            assertThat(result.keys).contains(relationshipsKey)
            assertThat(result[nodesKey] as ArrayList<*>).hasSize(howManyToCreate * 2)
            assertThat(result[relationshipsKey] as ArrayList<*>).hasSize(howManyToCreate)
        }
    }

    @Test
    fun testGenerateFromYamlFileProcedure() {
        TestUtil.testCall(
                EmbeddedServerHelper.graphDb, "CALL generate.fromYamlFile({yamlFilePath})",
                mapOf(
                        Pair(first = "yamlFilePath", second = "graph_samples/sample_graph.yaml")
                )
        ) { result ->
            assertThat(result.keys).contains(valuesKey)
            val resultsList = result[valuesKey] as ArrayList<String>
            assertThat(resultsList).hasSize(1)
            assertThat(resultsList[0]).isEqualTo("Done")
        }
    }

}