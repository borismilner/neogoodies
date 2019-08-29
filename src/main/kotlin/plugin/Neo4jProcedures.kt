@file:Suppress("unused")

package plugin

import graph_generator.GraphGenerator
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.logging.Log
import org.neo4j.procedure.Context
import org.neo4j.procedure.Mode
import org.neo4j.procedure.Name
import org.neo4j.procedure.Procedure
import structures.GraphResult
import structures.NodeListResult
import structures.ValueListResult
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

class GenerateValuesProcedure : PluginProcedure() {
    @JvmField
    @Context
    var log: Log? = null

    @Procedure(value = "generate.values", mode = Mode.READ)
    fun generateNodes(@Name("howMany") howMany: Long,
                      @Name("generatorName") generatorName: String,
                      @Name("parameters") parameters: Any): Stream<ValueListResult> {
        log!!.info(String.format("Generating %d values using generator %s with properties %s",
                howMany.toInt(),
                generatorName,
                parameters)
        )
        val graphGenerator = graphGenerator
        return Stream.of(ValueListResult(graphGenerator.generateValues(
                generatorName,
                parameters as List<Any>, // TODO: Check and assign empty list when an empty string
                howMany)))
    }
}

class GenerateLinkedListProcedure : PluginProcedure() {
    @JvmField
    @Context
    var log: Log? = null

    @Procedure(value = "generate.linkedList", mode = Mode.WRITE)
    fun generateNodes(@Name("howMany") howMany: Long,
                      @Name("labels") labelsStringArray: Any,
                      @Name("nodePropertiesString") nodePropertiesString: String,
                      @Name("relationshipType") relationshipType: String): Stream<GraphResult> {
        val labels = (labelsStringArray as ArrayList<String>).toTypedArray()
        log!!.info("Generating a linked list of $howMany nodes with labels ${labels.contentToString()} connected by $relationshipType")
        val graphGenerator = graphGenerator
        val nodesForLinkedList = graphGenerator.generateNodes(GraphGenerator.labelsFromStrings(labels), nodePropertiesString, howMany)
        val graphResult = graphGenerator.generateLinkedList(nodesForLinkedList, relationshipType)
        return Stream.of(graphResult)
    }
}
