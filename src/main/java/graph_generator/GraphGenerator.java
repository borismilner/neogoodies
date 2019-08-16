package graph_generator;

import graph_components.NodesAndEdges;
import graph_components.Property;
import org.neo4j.graphdb.*;
import utilities.ValueFaker;
import utilities.YamlParser;

import java.util.*;

public class GraphGenerator {
    private final GraphDatabaseService database;
    private final YamlParser parser;
    private final ValueFaker valueFaker;
    private final Random random;

    public GraphGenerator(GraphDatabaseService database, YamlParser parser, ValueFaker valueFaker) {
        this.database = database;
        this.parser = parser;
        this.valueFaker = valueFaker;
        this.random = valueFaker.getRandom();
    }

    public List<Object> generateValues(String generatorName, List<Object> parameters, Long howMany) {
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

    public List<Node> generateNodes(Label[] labels, String propertiesString, long howMany) {
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < howMany; ++i) {
            Node node = database.createNode(labels);
            for (Property property : getProperties(propertiesString)) {
                node.setProperty(property.key(), valueFaker.getValue(property));
            }
            nodes.add(node);
        }
        return nodes;
    }

    private void addRelationshipProperties(Relationship relationship, List<Property> properties) {
        for (Property property : properties) {
            relationship.setProperty(property.key(), valueFaker.getValue(property));
        }
    }

    public List<Relationship> generateRelationshipsZipper(List<Node> fromNodes, List<Node> toNodes, String relationshipType, String relationshipProperties) {
        if (fromNodes.size() != toNodes.size()) {
            throw new RuntimeException(String.format("Non compatible node-list sizes, from=%d while to=%d", fromNodes.size(), toNodes.size()));
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

    public List<Relationship> generateRelationshipsFromAllToAll(List<Node> fromNodes, List<Node> toNodes, String relationshipType, String relationshipProperties) {
        if (fromNodes.isEmpty() || toNodes.isEmpty()) {
            throw new RuntimeException("Neither fromNodes nor toNodes can be empty!");
        }

        List<Relationship> relationships = new ArrayList<>();

        List<Property> propertyList = getProperties(relationshipProperties);
        for (Node fromNode : fromNodes) {
            for (Node toNode : toNodes) {
                createRelationship(relationshipType, relationships, propertyList, fromNode, toNode);
            }
        }
        return relationships;
    }

    private void createRelationship(String relationshipType, List<Relationship> relationships, List<Property> propertyList, Node fromNode, Node toNode) {
        Relationship relationship = fromNode.createRelationshipTo(toNode, RelationshipType.withName(relationshipType));
        addRelationshipProperties(relationship, propertyList);
        relationships.add(relationship);
    }

    public NodesAndEdges generateLinkedList(List<Node> nodesToLink, String relationshipType) {
        List<Relationship> relationships = new ArrayList<>();
        int numOfRequiredLinks = nodesToLink.size() - 1;
        for (int i = 0; i < numOfRequiredLinks; i++) {
            Node fromNode = nodesToLink.get(i);
            Node toNode = nodesToLink.get(i + 1);
            relationships.add(fromNode.createRelationshipTo(toNode, RelationshipType.withName(relationshipType)));
        }
        return new NodesAndEdges(nodesToLink, relationships);
    }
}
