package graph_generator;

import exceptions.InputValidationException;
import graph_components.Property;
import logging.LogHelper;
import neo_results.GraphResult;
import org.apache.logging.log4j.Logger;
import org.neo4j.graphdb.*;
import org.yaml.snakeyaml.Yaml;
import testing.GraphYamlTemplate;
import utilities.ValueFaker;
import utilities.YamlParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;

public class GraphGenerator {
    private final GraphDatabaseService database;
    private final YamlParser parser;
    private final ValueFaker valueFaker;

    Transaction beginTransaction() {
        return database.beginTx();
    }

    GraphGenerator(GraphDatabaseService database, YamlParser parser, ValueFaker valueFaker) {
        this.database = database;
        this.parser = parser;
        this.valueFaker = valueFaker;
    }

    List<Object> generateValues(String generatorName, List<Object> parameters, Long howMany) {
        Property property = new Property(generatorName, generatorName, parameters);
        List<Object> values = new ArrayList<>();
        for (int i = 0; i < howMany; i++) {
            values.add(valueFaker.getValue(property));
        }
        return values;
    }

    private List<Property> getProperties(String propertiesString) {
        if (propertiesString.equals("''") || propertiesString.equals("'{}'") || propertiesString.equals("") || propertiesString.equals("{}")) {
            return new ArrayList<>();
        }
        return parser.parseProperties(propertiesString);
    }

    List<Node> generateNodes(Label[] labels, String propertiesString, long howMany) {
        List<Node> nodes = new ArrayList<>();
        try (Transaction transaction = database.beginTx()) {
            for (int i = 0; i < howMany; ++i) {
                Node node = database.createNode(labels);
                for (Property property : getProperties(propertiesString)) {
                    node.setProperty(property.key(), valueFaker.getValue(property));
                }
                nodes.add(node);
            }
            transaction.success();
        }
        return nodes;
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
        List<Property> propertyList = getProperties(relationshipProperties);
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

        List<Property> propertyList = getProperties(relationshipProperties);
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
        for (int i = 0; i < numOfRequiredLinks; i++) {
            Node fromNode = nodesToLink.get(i);
            Node toNode = nodesToLink.get(i + 1);
            relationships.add(fromNode.createRelationshipTo(toNode, RelationshipType.withName(relationshipType)));
        }
        return new GraphResult(nodesToLink, relationships);
    }

    void generateFromYamlFile(String filePath) {
        Logger log = LogHelper.getLogger();
        GraphYamlTemplate required;
        Map<String, List<Entity>> mapComponents = new HashMap<>();
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

    }
}
