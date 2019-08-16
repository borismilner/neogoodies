package testing;

import logging.LogHelper;
import org.apache.logging.log4j.Logger;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.harness.ServerControls;
import org.neo4j.harness.TestServerBuilders;

import java.net.URI;

public class EmbeddedServerHelper {

    private static ServerControls embeddedDatabaseServer;
    private static final Logger log = LogHelper.getLogger();
    private static GraphDatabaseService graphDb;

    private static GraphDatabaseService getEmbeddedServer() {
        if (graphDb != null) {
            return graphDb;
        }
        embeddedDatabaseServer = TestServerBuilders
                .newInProcessBuilder()
                .withConfig("dbms.connector.bolt.enabled", "true")
                .withConfig("dbms.connector.bolt.listen_address", ":7687") // TODO: Get port from configurations or as a parameter
                .withConfig("dbms.logs.query.enabled", "true")
                .withConfig("dbms.track_query_cpu_time", "true")
                .newServer();

        log.info(String.format("Bolt server is available at: %s", embeddedDatabaseServer.boltURI()));
        graphDb = embeddedDatabaseServer.graph();
        return graphDb;
    }

    public static URI getBoltUri() {
        return embeddedDatabaseServer.boltURI();
    }

    public static void generateGraphFromJsonTemplate(String jsonFilePath) {
        GraphFromJsonGenerator graphFromJsonGenerator = new GraphFromJsonGenerator(getEmbeddedServer(), jsonFilePath);
        graphFromJsonGenerator.generateGraph();
    }

    public static void clearGraph() {

        try (
                Driver driver = GraphDatabase.driver(getBoltUri());
                Session session = driver.session()
        ) {
            log.debug("Clearing the graph using detach delete...");
            session.run("MATCH (n) detach delete n");
        }

    }

    private EmbeddedServerHelper() {
    }
}
