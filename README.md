# <u>Neo-Goodies</u>

> Useful tools for working with Neo4J
---
### <u>Samples for testing plugin calls</u>

```
CALL generate.nodes(5, ["Officer", "Gentleman"], "{'name': 'FIRSTNAME', 'phone': 'PHONE_NUMBER'}")
```
![alt text](images/generate_nodes_plugin_call.png "generate.nodes function")
```
CALL generate.linkedList(10, ["Person"], "{}", "Likes")
```
```
CALL generate.values(10, "FULLNAME",[])
```
```
CALL generate.zipper(10,"SourceNode", "{}", "TargetNode", "{}", "Defines", "{}")
```
```
CALL generate.fromYamlFile("c:/temp/sample.graph.yaml")
```