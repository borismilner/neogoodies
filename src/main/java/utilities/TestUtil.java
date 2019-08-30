package utilities;

import com.google.common.io.Files;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/*Brutally robbed out of APOC source-code and adjusted to the existing one as much as possible*/

public class TestUtil {

    public static void testCallEmpty(GraphDatabaseService db, String call, Map<String, Object> params) {
        testResult(db, call, params, (res) -> assertThat(res.hasNext()).isFalse() /*assertFalse("Expected no results", res.hasNext())*/);
    }

    public static void testCallCount(GraphDatabaseService db, String call, Map<String, Object> params, final int count) {
        testResult(db, call, params, (res) -> {
            int left = count;
            while (left > 0) {
                assertThat(res.hasNext()).isTrue();
//                assertTrue("Expected " + count + " results, but got only " + (count - left), res.hasNext());
                res.next();
                left--;
            }
            assertThat(res.hasNext()).isFalse();
//            assertFalse("Expected " + count + " results, but there are more ", res.hasNext());
        });
    }

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


    public static void ignoreException(Runnable runnable, Class<? extends Throwable>... causes) {
        try {
            runnable.run();
        } catch (Throwable x) {
            if (TestUtil.hasCauses(x, causes)) {
                System.err.println("Ignoring Exception " + x + ": " + x.getMessage() + " due to causes " + Arrays.toString(causes));
            } else {
                throw x;
            }
        }
    }

    public static URL getUrlFileName(String filename) {
        return Thread.currentThread().getContextClassLoader().getResource(filename);
    }

    public static String readFileToString(File file) {
        return readFileToString(file, StandardCharsets.UTF_8);
    }

    public static String readFileToString(File file, Charset charset) {
        try {
            return Files.toString(file, charset);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
