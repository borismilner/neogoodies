package utilities;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/*Brutally robbed out of APOC source-code and adjusted to the existing one as much as possible*/

public class TestUtil {

    public static void testFail(GraphDatabaseService db, String call, Class<? extends Exception> t) {
        try {
            testResult(db, call, null, (r) -> {
                while (r.hasNext()) {
                    r.next();
                }
                r.close();
            });
            fail("Didn't fail with " + t.getSimpleName());
        } catch (Exception e) {
            Throwable inner = e;
            boolean found = false;
            do {
                found |= t.isInstance(inner);
                inner = inner.getCause();
            }
            while (inner != null && inner.getCause() != inner);
            assertThat(found).isTrue();
//            assertTrue("Didn't fail with " + t.getSimpleName() + " but " + e.getClass().getSimpleName() + " " + e.getMessage(), found);
        }
    }

    public static void testResult(GraphDatabaseService db, String call, Consumer<Result> resultConsumer) {
        testResult(db, call, null, resultConsumer);
    }

    public static void testResult(GraphDatabaseService db, String call, Map<String, Object> params, Consumer<Result> resultConsumer) {
        try (Transaction tx = db.beginTx()) {
            Map<String, Object> p = (params == null) ? Collections.emptyMap() : params;
            resultConsumer.accept(db.execute(call, p));
            tx.success();
        }
    }

    public static boolean hasCauses(Throwable t, Class<? extends Throwable>... types) {
        if (anyInstance(t, types)) {
            return true;
        }
        while (t != null && t.getCause() != t) {
            if (anyInstance(t, types)) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    private static boolean anyInstance(Throwable t, Class<? extends Throwable>[] types) {
        for (Class<? extends Throwable> type : types) {
            if (type.isInstance(t)) {
                return true;
            }
        }
        return false;
    }
}
