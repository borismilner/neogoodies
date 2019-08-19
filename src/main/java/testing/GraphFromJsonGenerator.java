package testing;

import configuration.Constants;
import exceptions.InputValidationException;
import logging.LogHelper;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.graphdb.*;
import utilities.GeneralUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class GraphFromJsonGenerator {
    private final GraphDatabaseService db;
    private final String jsonFilePath;
    private final Logger log;
    private final Pattern propertyPattern = Pattern.compile("(.*?)@([a-z]+)\\[(\\d+)]");
    private final Pattern customPropertyPattern = Pattern.compile("(.*?):(.*?@[a-z]+\\[\\d+])");
    private final Pattern relationshipPattern = Pattern.compile("(.*?)-\\[(.+)]->(.*)");
    private static final String FORMAT = "%s_%d";
    private static int serialInteger = 0;

    private final Map<String, Node> mapStringToNode;

    GraphFromJsonGenerator(GraphDatabaseService db, String jsonFilePath) {
        this.db = db;
        this.jsonFilePath = jsonFilePath;
        this.log = LogHelper.getLogger();
        mapStringToNode = new HashMap<>();
    }

    private static int getSerial() {
        serialInteger++;
        return serialInteger;
    }

    private void setMockProperty(Node node, String propertyTemplate) {

        String nodeLabel = node.getLabels().iterator().next().toString();

        Matcher matcher = this.propertyPattern.matcher(propertyTemplate);
        if (!matcher.find()) {
            throw new InputValidationException(String.format("%s must be of the FORMAT: name@type[cardinality]", propertyTemplate));
        }

        String propertyName = matcher.group(1);
        String propertyType = matcher.group(2);
        int propertyCardinality = Integer.parseInt(matcher.group(3));

        Object propertyValue;

        switch (propertyType) {
            case "string":
                if (propertyCardinality == 0) {
                    propertyValue = String.format(FORMAT, nodeLabel, getSerial());
                } else {
                    List<String> stringList = new ArrayList<>(propertyCardinality);
                    for (int i = 0; i < propertyCardinality; i++) {
                        stringList.add(String.format(FORMAT, nodeLabel, (getSerial() + 1) * 10 * (i + 1)));
                    }
                    String[] strings = new String[stringList.size()];
                    strings = stringList.toArray(strings);
                    propertyValue = strings;
                }
                break;
            case "integer":
                if (propertyCardinality == 0) {
                    propertyValue = getSerial();
                } else {
                    List<Integer> intList = new ArrayList<>(propertyCardinality);
                    for (int i = 0; i < propertyCardinality; i++) {
                        intList.add((getSerial() + 1) * 10 * (i + 1));
                    }
                    Integer[] ints = new Integer[intList.size()];
                    ints = intList.toArray(ints);
                    propertyValue = ints;
                }
                break;
            default:
                throw new InputValidationException("Currently only supporting properties of type string or integer");
        }
        node.setProperty(propertyName, propertyValue);
    }

    private void generateGraph(GraphJsonTemplate graphJsonTemplate) {
        log.info(String.format("Populating graph: %s", graphJsonTemplate.graphName));
        if (!graphJsonTemplate.comments.trim().equals("")) {
            log.info(String.format("Comments: %s", graphJsonTemplate.comments));
        }
        log.info(String.format("Populating nodes of %d types.", graphJsonTemplate.nodes.size()));
        try (Transaction transaction = db.beginTx()) {
            for (NodeStructure nodeStructure : graphJsonTemplate.nodes) {
                for (int counter = 0; counter < nodeStructure.howMany; counter++) {
                    Node node = db.createNode(Label.label(nodeStructure.type));
                    node.setProperty(nodeStructure.idProperty, String.valueOf(counter));
                    for (String property : nodeStructure.properties) {
                        setMockProperty(node, property);
                    }
                    mapStringToNode.put(String.format("%s(%d)", nodeStructure.type, counter), node);
                    if (nodeStructure.createIdNode) {
                        Node idNode = db.createNode(Label.label(Constants.IDENTIFIER_STR));
                        idNode.setProperty(Constants.TYPE_STR, Constants.IDENTIFIER_STR);
                        idNode.setProperty(Constants.VALUE_STR, String.format(FORMAT, nodeStructure.type, counter));
                        idNode.createRelationshipTo(node, RelationshipType.withName(Constants.IDENTIFIES_REL_STR));
                        mapStringToNode.put(String.format("^^%s(%d)", nodeStructure.type, counter), idNode);
                    }
                }
            }

            for (String nodeProperty : graphJsonTemplate.customProperties) {
                Matcher matcher = this.customPropertyPattern.matcher(nodeProperty);
                if (!matcher.find()) {
                    throw new InputValidationException(String.format("%s must be of the FORMAT: Node:PropertyName@type[cardinality]", customPropertyPattern));
                }

                String nodeKey = matcher.group(1);
                String theRest = matcher.group(2);

                setMockProperty(mapStringToNode.get(nodeKey), theRest);
            }

            transaction.success();
        }
        log.info(String.format("Populating %d relationships.", graphJsonTemplate.edges.size()));
        try (Transaction transaction = db.beginTx()) {
            for (String relationship : graphJsonTemplate.edges) {
                Matcher matcher = relationshipPattern.matcher(relationship);
                if (!matcher.find()) {
                    throw new InputValidationException(String.format("%s must be of the FORMAT: Node(#)-[RELATIONSHIP]->Node(#)", relationship));
                }

                String sourceNodeKey = matcher.group(1);
                String relationshipType = matcher.group(2);
                String targetNodeKey = matcher.group(3);

                Node sourceNode = mapStringToNode.get(sourceNodeKey);
                Node targetNode = mapStringToNode.get(targetNodeKey);

                if (sourceNode == null || targetNode == null) {
                    throw new InputValidationException(String.format("Either %s or %s was not created during node-creation phase.", sourceNodeKey, targetNodeKey));
                }

                sourceNode.createRelationshipTo(targetNode, RelationshipType.withName(relationshipType));

            }
            transaction.success();
        }
    }

    void generateGraph() {
        File file = new File(jsonFilePath);
        if (!file.exists() || !file.isFile()) {
            throw new InputValidationException(String.format("%s does not exist", jsonFilePath));
        }

        String jsonContent;
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(jsonFilePath));
            jsonContent = new String(bytes, StandardCharsets.UTF_8);
            jsonContent = GeneralUtils.removeComments(jsonContent);
        } catch (IOException e) {
            throw new InputValidationException(String.format("Could not read %s", jsonFilePath));
        }
        ObjectMapper mapper = new ObjectMapper();
        GraphJsonTemplate graphJsonTemplate = null;
        try {
            graphJsonTemplate = mapper.readValue(jsonContent, GraphJsonTemplate.class);
        } catch (IOException e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
        generateGraph(graphJsonTemplate);
    }
}
