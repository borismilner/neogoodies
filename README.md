# <u>Neo-Goodies</u>

> Useful tools for working with Neo4J  
> Currently the library consists of function allowing:  
>* Creation of different graph structures
>* Working with the embedded Neo4J server (mainly for unit-testing)
---
```
CALL generate.nodes(5, ["Officer", "Gentleman"], "{'name': 'FIRSTNAME', 'phone': 'PHONE_NUMBER'}")
```
![alt text](images/generate_nodes_plugin_call.png "generate.nodes function")
```
CALL generate.linkedList(5, ["Person"], "{'name': 'FIRSTNAME', 'phone': 'PHONE_NUMBER'}", "LIKES")
```
![alt text](images/generate_linkedlist_plugin_call.png "generate.linkedList function")
```
CALL generate.values(10, "COUNTRY",[])
```
![alt text](images/generate_values_plugin_call.png "generate.values function")
```
CALL generate.zipper(3,"SourceNode", "{'name': 'FIRSTNAME', 'phone': 'PHONE_NUMBER'}", "TargetNode", "{'name': 'FIRSTNAME', 'country': 'COUNTRY'}", "CONTACTS", "{'email':'EMAIL_ADDRESS'}")
```
![alt text](images/generate_zip_plugin_call.png "generate.zipper function")
```
CALL generate.fromYamlFile("c:/temp/sample_graph.yaml")
```
Check out graph-template sample [here](graph_samples/sample_graph.yaml)  

![alt text](images/generate_yaml_plugin_call.png "generate.fromYamlFile function")