package graph_generator

import org.junit.jupiter.api.*
import plugin.GenerateNodesProcedure
import tools.GraphGenerator
import utilities.EmbeddedServerHelper
import utilities.TestUtil
import utilities.TestUtil.registerProcedure
import utilities.ValueFaker
import utilities.YamlParser


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PluginProcedureTests {

    private lateinit var graphGenerator: GraphGenerator

    @BeforeAll
    fun beforeAll() {
        val embeddedServer = EmbeddedServerHelper.graphDb
        graphGenerator = GraphGenerator(embeddedServer, YamlParser(), ValueFaker())
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
        registerProcedure(EmbeddedServerHelper.graphDb, GenerateNodesProcedure::class.java)
//        TestUtil.testCall(
//                EmbeddedServerHelper.graphDb, "CALL generate.nodes({howMany},{labels},{propertiesYamlString})", mapOf(
//                Pair(first = "howMany", second = 10),
//                Pair(first = "labels", second = """["Boris", "Milner"]"""),
//                Pair(first = "propertiesYamlString", second = "{}"))
//        ) { row -> println(row.toString()) }

        TestUtil.testCall(
                EmbeddedServerHelper.graphDb, "call generate.nodes(5, [\"Officer\", \"Gentleman\"],\"\")"
        ) { row -> println(row.toString()) }
    }
}