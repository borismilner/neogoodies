@file:Suppress("unused")

package plugin

import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Label
import org.neo4j.logging.Log
import org.neo4j.procedure.Context
import org.neo4j.procedure.Mode
import org.neo4j.procedure.Name
import org.neo4j.procedure.Procedure
import structures.GraphResult
import structures.NodeListResult
import structures.ValueListResult
import tools.GraphGenerator
import utilities.ValueFaker
import utilities.YamlParser
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
        val labels = (labelsStringArray as Iterable<*>).filterIsInstance<String>()
        log!!.info("Generating ${howMany.toInt()} nodes with labels ${labels.joinToString()} and properties $propertiesYamlString")

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
    fun generateValues(@Name("howMany") howMany: Long,
                       @Name("generatorName") generatorName: String,
                       @Name("parameters") parameters: Any): Stream<ValueListResult> {
        log!!.info("Generating ${howMany.toInt()} values using generator $generatorName with properties $parameters")

        val graphGenerator = graphGenerator
        return Stream.of(ValueListResult(graphGenerator.generateValues(
                generatorName,
                (parameters as List<*>).filterIsInstance<Any>(),
                howMany)))
    }
}

class GenerateLinkedListProcedure : PluginProcedure() {
    @JvmField
    @Context
    var log: Log? = null

    @Procedure(value = "generate.linkedList", mode = Mode.WRITE)
    fun generateLinkedList(@Name("howMany") howMany: Long,
                           @Name("labels") labelsStringArray: Any,
                           @Name("nodePropertiesString") nodePropertiesString: String,
                           @Name("relationshipType") relationshipType: String): Stream<GraphResult> {
        val labels = (labelsStringArray as Iterable<*>).filterIsInstance<String>()
        log!!.info("Generating a linked list of $howMany nodes with labels ${labels.joinToString()} connected by $relationshipType")
        val graphGenerator = graphGenerator
        val nodesForLinkedList = graphGenerator.generateNodes(GraphGenerator.labelsFromStrings(labels), nodePropertiesString, howMany)
        val graphResult = graphGenerator.generateLinkedList(nodesForLinkedList, relationshipType)
        return Stream.of(graphResult)
    }
}

class GenerateZipperProcedure : PluginProcedure() {
    @JvmField
    @Context
    var log: Log? = null

    @Procedure(value = "generate.zipper", mode = Mode.WRITE)
    fun generateZipper(@Name("howMany") howMany: Long,
                       @Name("sourceLabel") sourceLabelName: String,
                       @Name("sourcePropertiesString") sourcePropertiesString: String,
                       @Name("targetLabel") targetLabelName: String,
                       @Name("targetPropertiesString") targetPropertiesString: String,
                       @Name("relationshipType") relationshipType: String,
                       @Name("relationshipProperties") relationshipProperties: String): Stream<GraphResult> {

        log!!.info("Generating zipper structure of ${howMany.toInt()} nodes in each side, source=$sourceLabelName, target=$targetLabelName, relationship=$relationshipType, rel_properties:$relationshipProperties")

        val graphGenerator = graphGenerator
        val sourceLabel = Label.label(sourceLabelName)
        val targetLabel = Label.label(targetLabelName)
        val sourceNodes = graphGenerator.generateNodes(arrayOf(sourceLabel), sourcePropertiesString, howMany.toInt().toLong())
        val targetNodes = graphGenerator.generateNodes(arrayOf(targetLabel), targetPropertiesString, howMany.toInt().toLong())
        val relationships = graphGenerator.generateRelationshipsZipper(sourceNodes, targetNodes, relationshipType, relationshipProperties)
        val graphResult = GraphResult(sourceNodes + targetNodes, relationships)
        return Stream.of(graphResult)
    }
}

class GenerateFromYamlFileProcedure : PluginProcedure() {
    @JvmField
    @Context
    var log: Log? = null

    @Procedure(value = "generate.fromYamlFile", mode = Mode.WRITE)
    fun generateFromYamlFile(@Name("yamlFilePath") yamlFilePath: String): Stream<ValueListResult> {
        log!!.info("Generating a graph from a YAML template if file: $yamlFilePath")
        val graphGenerator = graphGenerator
        graphGenerator.generateFromYamlFile(yamlFilePath)
        return Stream.of(ValueListResult(listOf("Done")))
    }
}
