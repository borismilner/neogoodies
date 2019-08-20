package plugin;

import graph_generator.GraphGenerator;
import neo_results.ValueListResult;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.List;
import java.util.stream.Stream;

public class GenerateValuesProcedure extends PluginProcedure {
    @SuppressWarnings("WeakerAccess")
    @Context
    public Log log;

    @Procedure(value = "generate.values", mode = Mode.READ)
    public Stream<ValueListResult> generateNodes(@Name("howMany") Long howMany,
                                                 @Name("generatorName") String generatorName,
                                                 @Name("parameters") Object parameters) {
        log.info(String.format("Generating %d values using generator %s with properties %s",
                               howMany.intValue(),
                               generatorName,
                               parameters)
                );
        GraphGenerator graphGenerator = getGraphGenerator();
        return Stream.of(new ValueListResult(graphGenerator.generateValues(
                generatorName,
                (List<Object>) parameters,
                howMany)));
    }
}
