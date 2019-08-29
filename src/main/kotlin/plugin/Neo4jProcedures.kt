package plugin

import graph_generator.GraphGenerator
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.logging.Log
import org.neo4j.procedure.Context
import org.neo4j.procedure.Mode
import org.neo4j.procedure.Name
import org.neo4j.procedure.Procedure
import structures.NodeListResult
import utilities.ValueFaker
import utilities.YamlParser
import java.util.*
import java.util.stream.Stream

open class PluginProcedure {
    @Context
    lateinit var database: GraphDatabaseService

    companion object {
        val yamlParser: YamlParser = YamlParser()
        val valueFaker: ValueFaker = ValueFaker()
    }

    val graphGenerator: GraphGenerator
        get() {
            return GraphGenerator(database, yamlParser, valueFaker)
        }
}

class GenerateNodesProcedure : PluginProcedure() {
    @JvmField
    @Context
    var log: Log? = null

    @Procedure(value = "generate.nodes", mode = Mode.WRITE)
    fun generateNodes(@Name("howMany") howMany: Long,
                      @Name("labels") labelsStringArray: Any,
                      @Name("propertiesYamlString") propertiesYamlString: String): Stream<NodeListResult> {
        val labels = (labelsStringArray as ArrayList<String>).toTypedArray()
        log!!.info(String.format("Generating %d nodes with labels %s and properties %s",
                howMany.toInt(),
                labels.contentToString(),
                propertiesYamlString))

        return Stream.of<NodeListResult>(
                NodeListResult(
                        graphGenerator.generateNodes(
                                GraphGenerator.labelsFromStrings(labels),
                                propertiesYamlString,
                                howMany
                        )
                )
        )
    }
}
