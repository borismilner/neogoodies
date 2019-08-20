package plugin;

import graph_generator.GraphGenerator;
import neo_results.ValueListResult;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Arrays;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class GenerateFromYamlFileProcedure extends PluginProcedure {
    @SuppressWarnings("WeakerAccess")
    @Context
    public Log log;

    @Procedure(value = "generate.fromYamlFile", mode = Mode.WRITE)
    public Stream<ValueListResult> generateNodes(@Name("yamlFilePath") String yamlFilePath) {
        log.info(String.format("Generating a graph from a YAML template if file: %s", yamlFilePath));
        GraphGenerator graphGenerator = getGraphGenerator();
        graphGenerator.generateFromYamlFile(yamlFilePath);
        return Stream.of(new ValueListResult(Arrays.asList(new String[]{"Done"})));
    }
}
