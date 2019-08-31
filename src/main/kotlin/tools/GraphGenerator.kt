package tools

import exceptions.InputValidationException
import graph_components.Property
import logging.LogHelper
import org.neo4j.graphdb.*
import structures.GraphResult
import structures.GraphYamlTemplate
import utilities.ValueFaker
import utilities.YamlParser
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.*
import kotlin.collections.ArrayList

class GraphGenerator(val database: GraphDatabaseService,
                     private val parser: YamlParser,
                     private val valueFaker: ValueFaker) {


    private var mapComponents: MutableMap<String, List<Node>> = HashMap()

    private val requiredNodePattern = """(.*?)<(\d+)>""".toRegex()  // e.g. Person<3>
//    private val nodePattern = Pattern.compile("(.*?)<(\\d+)>")

    companion object {
        fun labelsFromStrings(labelNames: Array<String>): Array<Label> {
            val nodeLabels = ArrayList<Label>()
            for (labelName in labelNames) {
                val newLabel = Label.label(labelName)
                nodeLabels.add(newLabel)
            }
            return Array(size = nodeLabels.size, init = { i -> nodeLabels[i] })
        }
    }

    fun generateValues(generatorName: String, parameters: List<Any>, howMany: Long): List<Any> {
        val property = Property(key = generatorName, generatorName = generatorName, parameters = parameters)
        val values = arrayListOf<Any>()
        for (i in 0 until howMany) {
            values.add(valueFaker.getValue(property))
        }
        return values
    }

    private fun propertiesFromYamlString(propertiesString: String): List<Property> {
        return if (
                propertiesString == "''" ||
                propertiesString == "'{}'" ||
                propertiesString == "" ||
                propertiesString == "{}"
        ) {
            ArrayList()
        } else parser.parseProperties(propertiesString)
    }

    private fun mapToYamlString(propertiesMap: Map<String, String>?): String {
        return parser.yaml.dump(propertiesMap)
    }

    fun generateNodes(labels: Array<Label>, propertiesString: String, howMany: Long): List<Node> {
        val nodesGenerated = ArrayList<Node>()
        database.beginTx().use { transaction ->
            for (i in 0 until howMany) {
                val node = database.createNode(*labels)
                for (property in propertiesFromYamlString(propertiesString)) {
                    node.setProperty(property.key, valueFaker.getValue(property))
                }
                nodesGenerated.add(node)
            }
            transaction.success()
        }
        return nodesGenerated
    }

    private fun addRelationshipProperties(relationship: Relationship, properties: List<Property>) {
        for (property in properties) {
            relationship.setProperty(property.key, valueFaker.getValue(property))
        }
    }

    fun generateRelationshipsZipper(fromNodes: List<Node>,
                                    toNodes: List<Node>,
                                    relationshipType: String,
                                    relationshipProperties: String): List<Relationship> {
        if (fromNodes.size != toNodes.size) {
            throw InputValidationException("Non compatible node-list sizes, from=${fromNodes.size} while to=${toNodes.size}")
        }

        val relationships = ArrayList<Relationship>()

        for ((fromNode, toNode) in fromNodes.zip(toNodes)) {
            val propertyList = propertiesFromYamlString(relationshipProperties)
            createRelationship(relationshipType, relationships, propertyList, fromNode, toNode)
        }

        return relationships
    }

    fun generateRelationshipsFromAllToAll(fromNodes: List<Node>,
                                          toNodes: List<Node>,
                                          relationshipType: String,
                                          relationshipProperties: String): List<Relationship> {
        if (fromNodes.isEmpty() || toNodes.isEmpty()) {
            throw InputValidationException("Neither fromNodes nor toNodes can be empty!")
        }

        val relationships = ArrayList<Relationship>()

        database.beginTx().use { transaction ->
            for (fromNode in fromNodes) {
                for (toNode in toNodes) {
                    val propertyList = propertiesFromYamlString(relationshipProperties)
                    createRelationship(relationshipType, relationships, propertyList, fromNode, toNode)
                }
            }
            transaction.success()
        }
        return relationships
    }

    private fun createRelationship(relationshipType: String,
                                   relationships: MutableList<Relationship>,
                                   propertyList: List<Property>,
                                   fromNode: Node,
                                   toNode: Node) {
        database.beginTx().use { transaction ->
            val relationship = fromNode.createRelationshipTo(toNode, RelationshipType.withName(relationshipType))
            addRelationshipProperties(relationship, propertyList)
            relationships.add(relationship)
            transaction.success()
        }

    }

    fun generateLinkedList(nodesToLink: List<Node>, relationshipType: String): GraphResult {
        val relationships = ArrayList<Relationship>()
        val numOfRequiredLinks = nodesToLink.size - 1
        database.beginTx().use { transaction ->
            for (i in 0 until numOfRequiredLinks) {
                val fromNode = nodesToLink[i]
                val toNode = nodesToLink[i + 1]
                relationships.add(fromNode.createRelationshipTo(toNode, RelationshipType.withName(relationshipType)))
            }
            transaction.success()
        }
        return GraphResult(nodesToLink, relationships)
    }

    private fun parseSpecificNode(specificNode: String): Node {
        val matchResult = requiredNodePattern.find(specificNode) ?: throw InputValidationException("Could not parse: $specificNode")
        val key = matchResult.groupValues[1]
        val index = matchResult.groupValues[2].toInt()
        return mapComponents[key]!![index]
    }

    fun generateFromYamlFile(filePath: String) {
        val log = LogHelper.logger
        val required: GraphYamlTemplate
        mapComponents = HashMap()
        try {
            val ios = FileInputStream(File(filePath))
            val yaml = parser.yaml
            required = yaml.loadAs(ios, GraphYamlTemplate::class.java)
        } catch (e: FileNotFoundException) {
            throw InputValidationException("File not found: $filePath")
        }

        log.info("Generating graph: ${required.name}")
        if (required.comments!!.trim { it <= ' ' } != "") {
            log.info("Comments: ${required.comments}")
        }

        for ((mainLabel,
                howMany,
                properties,
                additionalLabels) in required.nodes!!) {

            log.info("Generating node with primary label of: $mainLabel")
            additionalLabels!!.add(mainLabel!!)
            val nodes = generateNodes(
                    labelsFromStrings(additionalLabels.toTypedArray()),
                    mapToYamlString(properties),
                    howMany.toLong()
            )
            mapComponents[mainLabel] = nodes
        }

        for (edgeDetails in required.relationships!!) {

            when (val connectionMethod = edgeDetails.connectionMethod) {

                "ZipNodes" -> {
                    val sourceNodes = mapComponents[edgeDetails.source] as List<Node>
                    val targetNodes = mapComponents[edgeDetails.target] as List<Node>
                    val relationshipName = edgeDetails.relationshipType as String
                    val properties = edgeDetails.properties as String
                    generateRelationshipsZipper(sourceNodes, targetNodes, relationshipName, properties)
                }

                "Link" -> {

                    val relationshipName = edgeDetails.relationshipType as String
                    val chains = edgeDetails.nodes as ArrayList<ArrayList<String>>
                    val relationProperties = edgeDetails.properties as String
                    for (chain in chains) {

                        val nodesToLink = ArrayList<Node>()
                        for (specificNode in chain) {
                            nodesToLink.add(parseSpecificNode(specificNode))
                        }

                        val (_, relationships) = generateLinkedList(nodesToLink, relationshipName)
                        for (relationship in relationships) {
                            addRelationshipProperties(relationship, propertiesFromYamlString(relationProperties))
                        }
                    }
                }
                else -> throw IllegalStateException("Unexpected value: ${connectionMethod as String}")
            }
        }

        database.beginTx().use { transaction ->
            for ((specificNode, properties) in required.customProperties!!) {


                val propertiesString = mapToYamlString(properties)
                val node = parseSpecificNode(specificNode!!)

                for (property in propertiesFromYamlString(propertiesString)) {
                    node.setProperty(property.key, valueFaker.getValue(property))
                }

            }
            transaction.success()
        }
    }
}
