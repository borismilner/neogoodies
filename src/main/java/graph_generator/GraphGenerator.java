package graph_generator;

import exceptions.InputValidationException;
import graph_components.Property;
import logging.LogHelper;
import neo_results.GraphResult;
import org.apache.logging.log4j.Logger;
import org.neo4j.graphdb.*;
import org.yaml.snakeyaml.Yaml;
import testing.GraphYamlTemplate;
import testing.NodeDetails;
import utilities.ValueFaker;
import utilities.YamlParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class GraphGenerator {
    private final GraphDatabaseService database;
    private final YamlParser parser;
    private final ValueFaker valueFaker;
    private Map<String, List<Node>> mapComponents = new HashMap<>();

    private Pattern nodePattern = Pattern.compile("(.*?)<(\\d+)>"); // e.g. Person<3>

    GraphGenerator(GraphDatabaseService database, YamlParser parser, ValueFaker valueFaker) {
        this.database = database;
        this.parser = parser;
        this.valueFaker = valueFaker;
    }

    Transaction beginTransaction() {
        return database.beginTx();
    }

    List<Object> generateValues(String generatorName, List<Object> parameters, Long howMany) {
        Property property = new Property(generatorName, generatorName, parameters);
        List<Object> values = new ArrayList<>();
        for (int i = 0; i < howMany; i++) {
            values.add(valueFaker.getValue(property));
        }
        return values;
    }

    private List<Property> propertiesFromYamlString(String propertiesString) {
        if (propertiesString.equals("''") || propertiesString.equals("'{}'") || propertiesString.equals("") || propertiesString.equals("{}")) {
            return new ArrayList<>();
        }
        return parser.parseProperties(propertiesString);
    }

    static Label[] labelsFromStrings(String[] labelNames) {
        List<Label> nodeLabels = new ArrayList<>();
        for (String labelName : labelNames) {
            Label newLabel = Label.label(labelName);
            nodeLabels.add(newLabel);
        }
        Label[] labels = new Label[nodeLabels.size()];
        for (int i = 0; i < nodeLabels.size(); i++) {
            labels[i] = nodeLabels.get(i);
        }
        return labels;
    }

    List<Node> generateNodes(Label[] labels, String propertiesString, long howMany) {
        List<Node> nodesGenerated = new ArrayList<>();
        try (Transaction transaction = database.beginTx()) {
            for (int i = 0; i < howMany; ++i) {
                Node node = database.createNode(labels);
                for (Property property : propertiesFromYamlString(propertiesString)) {
                    node.setProperty(property.key(), valueFaker.getValue(property));
                }
                nodesGenerated.add(node);
            }
            transaction.success();
        }
        return nodesGenerated;
    }

    private void addRelationshipProperties(Relationship relationship, List<Property> properties) {
        for (Property property : properties) {
            relationship.setProperty(property.key(), valueFaker.getValue(property));
        }
    }

    List<Relationship> generateRelationshipsZipper(List<Node> fromNodes, List<Node> toNodes, String relationshipType, String relationshipProperties) {
        if (fromNodes.size() != toNodes.size()) {
            throw new InputValidationException(String.format("Non compatible node-list sizes, from=%d while to=%d", fromNodes.size(), toNodes.size()));
        }

        List<Relationship> relationships = new ArrayList<>();

        Iterator<Node> fromNodesIterator = fromNodes.iterator();
        Iterator<Node> toNodesIterator = toNodes.iterator();
        List<Property> propertyList = propertiesFromYamlString(relationshipProperties);
        while (fromNodesIterator.hasNext()) {
            Node fromNode = fromNodesIterator.next();
            Node toNode = toNodesIterator.next();
            createRelationship(relationshipType, relationships, propertyList, fromNode, toNode);
        }
        return relationships;
    }

    List<Relationship> generateRelationshipsFromAllToAll(List<Node> fromNodes, List<Node> toNodes, String relationshipType, String relationshipProperties) {
        if (fromNodes.isEmpty() || toNodes.isEmpty()) {
            throw new InputValidationException("Neither fromNodes nor toNodes can be empty!");
        }

        List<Relationship> relationships = new ArrayList<>();

        List<Property> propertyList = propertiesFromYamlString(relationshipProperties);
        try (Transaction transaction = database.beginTx()) {
            for (Node fromNode : fromNodes) {
                for (Node toNode : toNodes) {
                    createRelationship(relationshipType, relationships, propertyList, fromNode, toNode);
                }
            }
            transaction.success();
        }
        return relationships;
    }

    private void createRelationship(String relationshipType, List<Relationship> relationships, List<Property> propertyList, Node fromNode, Node toNode) {
        try (Transaction transaction = database.beginTx()) {
            Relationship relationship = fromNode.createRelationshipTo(toNode, RelationshipType.withName(relationshipType));
            addRelationshipProperties(relationship, propertyList);
            relationships.add(relationship);
            transaction.success();
        }

    }

    GraphResult generateLinkedList(List<Node> nodesToLink, String relationshipType) {
        List<Relationship> relationships = new ArrayList<>();
        int numOfRequiredLinks = nodesToLink.size() - 1;
        try (Transaction transaction = database.beginTx()) {
            for (int i = 0; i < numOfRequiredLinks; i++) {
                Node fromNode = nodesToLink.get(i);
                Node toNode = nodesToLink.get(i + 1);
                relationships.add(fromNode.createRelationshipTo(toNode, RelationshipType.withName(relationshipType)));
            }
            transaction.success();
        }
        return new GraphResult(nodesToLink, relationships);
    }

    private Node parseSpecificNode(String specificNode) {
        Matcher matcher = nodePattern.matcher(specificNode);
        boolean foundMatch = matcher.find();
        if (!foundMatch) {
            throw new InputValidationException(String.format("Could not parse: %s", specificNode));
        }
        String key = matcher.group(1);
        int index = Integer.parseInt(matcher.group(2));
        return mapComponents.get(key).get(index);
    }

    void generateFromYamlFile(String filePath) {
        Logger log = LogHelper.getLogger();
        GraphYamlTemplate required;
        mapComponents = new HashMap<>();
        try {
            InputStream ios = new FileInputStream(new File(filePath));
            Yaml yaml = parser.getYaml();
            required = yaml.loadAs(ios, GraphYamlTemplate.class);
        } catch (FileNotFoundException e) {
            throw new InputValidationException(String.format("Failed reading %s", filePath));
        }

        log.info(String.format("Generating graph: %s", required.name));
        if (!required.comments.trim().equals("")) {
            log.info(String.format("Comments: %s", required.comments));
        }

        for (Map<String, Map<String, Object>> nodesToCreate : required.nodes) {
            nodesToCreate.forEach((storingKey, baseForNodeDetails) -> {
                log.info(String.format("Generating node with primary label of: %s", storingKey));
                NodeDetails nodeDetails = new NodeDetails(baseForNodeDetails);
                nodeDetails.additionalLabels.add(storingKey);
                List<Node> nodes = generateNodes(
                        labelsFromStrings(nodeDetails.additionalLabels.toArray(new String[0])),
                        nodeDetails.properties,
                        nodeDetails.howMany
                );

                mapComponents.put(storingKey, nodes);
            });
        }

        for (Map<String, Object> rel : required.relationships) {
            for (Map.Entry<String, Object> mapMethodToDetails : rel.entrySet()) {
                String connectionMethod = mapMethodToDetails.getKey();

                switch (connectionMethod) {
                    case "ZipNodes":
                        Map<String, String> value = (Map<String, String>) mapMethodToDetails.getValue();
                        List<Node> sourceNodes = mapComponents.get(value.get("source"));
                        List<Node> targetNodes = mapComponents.get(value.get("target"));
                        String relationshipName = value.get("relationship");
                        String properties = value.get("properties");
                        generateRelationshipsZipper(sourceNodes, targetNodes, relationshipName, properties);
                        break;
                    case "Link":
                        Map<String, Object> mapRelationNameToNodesAndProperties = (Map<String, Object>) mapMethodToDetails.getValue();
                        for (Map.Entry<String, Object> entry : mapRelationNameToNodesAndProperties.entrySet()) {
                            relationshipName = entry.getKey();
                            Map<String, Object> nodesAndProperties = (Map<String, Object>) entry.getValue();
                            List<List<String>> chains = (List<List<String>>) nodesAndProperties.get("nodes");
                            Object relationProperties = nodesAndProperties.get("properties");
                            for (List<String> chain : chains) {

                                List<Node> nodesToLink = new ArrayList<>();
                                for (String specificNode : chain) {
                                    nodesToLink.add(parseSpecificNode(specificNode));
                                }


                                GraphResult graphResult = generateLinkedList(nodesToLink, relationshipName);
                                for (Relationship r : graphResult.relationships) {
                                    addRelationshipProperties(r, propertiesFromYamlString((String) relationProperties));
                                }
                            }
                        }
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + connectionMethod);
                }
            }
        }

        try (Transaction transaction = database.beginTx()) {
            for (Map<String, String> x : required.customProperties) {
                for (Map.Entry<String, String> entry : x.entrySet()) {

                    String specificNode = entry.getKey();
                    String propertiesString = entry.getValue();

                    Node node = parseSpecificNode(specificNode);

                    for (Property property : propertiesFromYamlString(propertiesString)) {
                        node.setProperty(property.key(), valueFaker.getValue(property));
                    }
                }

            }
            transaction.success();
        }
    }
}

