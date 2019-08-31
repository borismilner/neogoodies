# <u>Neo-Goodies</u>

> Useful tools for working with Neo4J
---
### <u>Samples for testing plugin calls</u>

```
call generate.nodes(5, ["Officer", "Gentleman"],"")
```
```
call generate.linkedList(10, ["Person"], "{}", "Likes")
```
```
call generate.values(10, "FULLNAME",[])
```
```
call generate.zipper(10,"SourceNode", "{}", "TargetNode", "{}", "Defines", "{}")
```
```
call generate.fromYamlFile("c:/temp/sample.graph.yaml")
```