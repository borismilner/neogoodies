package structures

import org.neo4j.graphdb.Node
import org.neo4j.graphdb.Relationship

data class GraphResult(val nodes: List<Node>, val relationships: List<Relationship>)
data class NodeListResult(val nodes: List<Node>)
data class NodeResult(val node: Node)
