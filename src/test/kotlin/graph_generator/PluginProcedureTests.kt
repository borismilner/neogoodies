package graph_generator

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.neo4j.graphdb.Label
import org.neo4j.graphdb.Node
import plugin.GenerateNodesProcedure
import plugin.GenerateValuesProcedure
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

    private lateinit var graphGenerator: GraphGenerator

    @BeforeAll
    fun beforeAll() {
        val embeddedServer = EmbeddedServerHelper.graphDb
        graphGenerator = GraphGenerator(embeddedServer, YamlParser(), ValueFaker())
        registerProcedure(embeddedServer, GenerateNodesProcedure::class.java)
        registerProcedure(embeddedServer, GenerateValuesProcedure::class.java)
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
        val stringBuilder = StringBuilder("{")
        for ((propertyName, generatorName) in propertiesForEachNode) {
            stringBuilder.append("'$propertyName':'$generatorName',")
        }
        stringBuilder.deleteCharAt(stringBuilder.length - 1).append("}")
        TestUtil.testCall(
                EmbeddedServerHelper.graphDb, "CALL generate.nodes({howMany},{labels},{propertiesYamlString})",
                mapOf(
                        Pair(first = "howMany", second = howManyToCreate),
                        Pair(first = "labels", second = labelsForEachNode),
                        Pair(first = "propertiesYamlString", second = stringBuilder.toString())
                )
        ) { result ->
            val resultNodes = result["nodes"] as ArrayList<Node>
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
                assertThat(result.keys).contains("values")
                assertThat(result["values"] as ArrayList<*>).hasSize(howManyToCreate)
            }
        }
    }
}