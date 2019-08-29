package structures

import java.util.*

data class EdgeDetails(var properties: String? = null,
                       var connectionMethod: String? = null,
                       var relationshipType: String? = null,
                       var nodes: ArrayList<ArrayList<String>>? = null,
                       var source: String? = null,
                       var target: String? = null)


data class NodeDetails(
        var mainLabel: String? = null,
        var howMany: Int = 0,
        var properties: Map<String, String>? = null,
        var additionalLabels: List<String>? = null
)

data class NodePropertiesDetails(
        var node: String? = null,
        var properties: HashMap<String, String>? = null
)
