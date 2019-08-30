package utilities

import logging.LogHelper
import org.neo4j.driver.v1.GraphDatabase
import org.neo4j.driver.v1.StatementResult
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.harness.ServerControls
import org.neo4j.harness.TestServerBuilders
import java.net.URI

object EmbeddedServerHelper {
    private val log = LogHelper.logger
    private val embeddedDatabaseServer: ServerControls
    val graphDb: GraphDatabaseService
    private val boltUri: URI

    init {

        log.debug("Initializing embedded-server with pre-defined settings")
        embeddedDatabaseServer = TestServerBuilders
                .newInProcessBuilder()
                .withConfig("dbms.connector.bolt.enabled", "true")
                .withConfig("dbms.connector.bolt.listen_address", ":8888")
                .withConfig("dbms.logs.query.enabled", "true")
                .withConfig("dbms.track_query_cpu_time", "true")
                .newServer()

        graphDb = embeddedDatabaseServer.graph()
        boltUri = embeddedDatabaseServer.boltURI()
        log.info("Bolt server is available at: $boltUri")
    }

    fun execute(query: String): StatementResult {
        GraphDatabase.driver(boltUri).use { driver ->
            driver.session().use { session ->
                log.debug("Executing: $query")
                return session.run(query)
            }
        }
    }

    fun clearGraph() {

        GraphDatabase.driver(boltUri).use { driver ->
            driver.session().use { session ->
                log.debug("Clearing the graph using detach delete...")
                val run = session.run("MATCH (n) detach delete n")
                run
            }
        }

    }
}
