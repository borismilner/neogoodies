package utilities

import org.assertj.core.api.Assertions.assertThat
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.internal.kernel.api.exceptions.KernelException
import org.neo4j.kernel.impl.proc.Procedures
import org.neo4j.kernel.internal.GraphDatabaseAPI
import utilities.TestUtil.testResult
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

    fun printFullStackTrace(e: Throwable?) {
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

    fun testCall(db: GraphDatabaseService, call: String, params: Map<String, Any>?, consumer: Consumer<Map<String, Any>>) {
        testResult(db, call, params) { res ->
            try {
                assertThat(res.hasNext()).isTrue()
                val row = res.next()
                consumer.accept(row)
                assertThat(res.hasNext()).isFalse()
            } catch (t: Throwable) {
                printFullStackTrace(t)
                throw t
            }
        }
    }

    fun testCall(db: GraphDatabaseService, call: String, consumer: Consumer<Map<String, Any>>) {
        testCall(db, call, null, consumer)
    }
}