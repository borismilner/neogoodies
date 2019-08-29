# NeoGoodies

Useful utilities for working with Neo4j.


<span style="text-decoration:underline">Samples for testing plugin calls</span>
call generate.nodes(5, ["Officer", "Gentleman"],"")
call generate.linkedList(10, ["Person"], "{}", "Likes")
call generate.values(10, "FULLNAME",[])
call generate.zipper(10,"SourceNode", "{}", "TargetNode", "{}", "Defines", "{}")
call generate.fromYamlFile("c:/temp/sample.graph.yaml")