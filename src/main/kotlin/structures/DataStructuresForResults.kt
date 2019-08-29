package structures

import org.neo4j.graphdb.Node
import org.neo4j.graphdb.Relationship

data class GraphResult(@JvmField val nodes: List<Node>, @JvmField val relationships: List<Relationship>)
data class NodeListResult(@JvmField val nodes: List<Node>)
data class NodeResult(@JvmField val node: Node)
data class RelationshipListResult(@JvmField val relationships: List<Relationship>)
data class ValueListResult(@JvmField val values: List<Any>)
