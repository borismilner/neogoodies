package utilities

import org.assertj.core.api.Assertions.assertThat
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.internal.kernel.api.exceptions.KernelException
import org.neo4j.kernel.impl.proc.Procedures
import org.neo4j.kernel.internal.GraphDatabaseAPI
import utilities.TestUtil.testResult
import java.net.Socket
import java.util.function.Consumer

object TestUtilities {

    @Throws(KernelException::class)
    fun registerProcedure(db: GraphDatabaseService, vararg procedures: Class<*>) {
        val proceduresService = (db as GraphDatabaseAPI).dependencyResolver.resolveDependency(Procedures::class.java)
        for (procedure in procedures) {
            proceduresService.run {
                registerProcedure(procedure, true)
                registerFunction(procedure, true)
                registerAggregationFunction(procedure, true)
            }
        }
    }

    private fun printFullStackTrace(e: Throwable?) {
        var e = e
        var padding = ""
        while (e != null) {
            if (e.cause == null) {
                System.err.println(padding + e.message)
                for (element in e.stackTrace) {
                    if (element.className.matches("^(org.junit|org.apache.maven|sun.reflect|apoc.util.TestUtil|scala.collection|java.lang.reflect|org.neo4j.cypher.internal|org.neo4j.kernel.impl.proc|sun.net|java.net).*".toRegex())) {
                        continue
                    }
                    System.err.println(padding + element.toString())
                }
            }
            e = e.cause
            padding += "    "
        }
    }

    fun testCall(graphDb: GraphDatabaseService, callQuery: String, parameters: Map<String, Any>?, resultsStreamConsumer: Consumer<Map<String, Any>>) {
        testResult(graphDb, callQuery, parameters) { res ->
            try {
                assertThat(res.hasNext()).isTrue()
                val row = res.next()
                resultsStreamConsumer.accept(row)
                assertThat(res.hasNext()).isFalse()
            } catch (t: Throwable) {
                printFullStackTrace(t)
                throw t
            }
        }
    }

    fun isServerListening(host: String, port: Int): Boolean {
        try {
            Socket(host, port).use { return true }
        } catch (e: Exception) {
            return false
        }

    }
}