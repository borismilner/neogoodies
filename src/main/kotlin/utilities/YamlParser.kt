package utilities

import graph_components.Property
import org.yaml.snakeyaml.Yaml
import java.util.*

class YamlParser {
    val yaml = Yaml()

    fun parseProperties(yamlPropertiesString: String): List<Property> {
        val values = yaml.load<Map<String, Any>>(yamlPropertiesString)
        val properties = arrayListOf<Property>()
        for ((key, value) in values) {
            if (value is String) {
                properties.add(Property(key, value))
            } else {
                properties.add(fromMap(key, value))
            }
        }

        return properties
    }


    private fun fromMap(propertyKey: String, map: Any): Property {

        @Suppress("UNCHECKED_CAST") val prop = map as LinkedHashMap<String, Any>

        val key = prop.keys.iterator().next()

        if (prop[key] is List<*>) {
            return (Property(propertyKey, key, (prop[key] as List<*>).filterIsInstance<Any>()))
        }

        throw IllegalArgumentException("Invalid format")
    }
}
