package graph_generator

import org.junit.jupiter.api.*
import tools.GraphGenerator
import utilities.EmbeddedServerHelper
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
    }
}