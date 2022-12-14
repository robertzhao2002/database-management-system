package com.dbms.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dbms.Interpreter;
import com.dbms.utils.Catalog;
import com.dbms.utils.TupleReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;
import net.sf.jsqlparser.parser.ParseException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Runs end-to-end tests. */
class EndToEndTest {

    /** {@code queries} is the list of queries for parameterized testing */
    private static List<Arguments> queries;

    @BeforeAll
    public static void setup() throws IOException {
        QueryTestSetBuilder joins =
                new QueryTestSetBuilder("input/joins/config.txt", "output/joins", "expected/joins", "Joins");

        QueryTestSetBuilder indexed = new QueryTestSetBuilder(
                "input/indexed/config.txt", "output/indexed", "expected/indexed", "index queries");

        QueryTestSetBuilder big = new QueryTestSetBuilder("input/big/config.txt", "output/big", "expected/big", "Big");

        queries = joins.queries();
        queries.addAll(indexed.queries());
        queries.addAll(big.queries());
    }

    /** Initializes catalog, runs the input queries file, and asserts interpreter output with
     * expected output.
     *
     * @throws IOException
     * @throws ParseException */
    @ParameterizedTest(name = "{2} query {3}")
    @MethodSource("argumentProvider")
    void test(String actual, String expected, String name, int number) throws IOException, ParseException {
        assertEquals(expected, actual);
    }

    private static Stream<Arguments> argumentProvider() throws IOException {
        return Stream.of(queries.toArray(new Arguments[queries.size()]));
    }
}

/** Class for generating parameterized tests from the sample data files */
class QueryTestSetBuilder {
    private List<Arguments> arguments = new LinkedList<>();

    /** Sorts the output of the contents in {@code path}
     *
     * @param path is the file path containing the output of the query
     * @return a sorted relation as a {@code String}
     * @throws IOException */
    private String sortOutput(String path) throws IOException {
        List<String> tuples = new ArrayList<>();
        TupleReader tr = new TupleReader(path);
        List<Integer> tuple;
        while ((tuple = tr.nextTuple()) != null) {
            String toAdd = tuple.toString();
            int index = Collections.binarySearch(tuples, toAdd);
            tuples.add(index < 0 ? -index - 1 : index, toAdd);
        }
        return tuples.toString();
    }

    QueryTestSetBuilder(
            final String configPath, final String outputPath, final String expectedOutputPath, final String name)
            throws IOException {
        Catalog.init(configPath);

        Interpreter.run();

        File[] expectedFiles = new File(expectedOutputPath).listFiles();
        File[] outputFiles = Arrays.stream(new File(outputPath).listFiles())
                .filter(f -> f.getName().indexOf("_") == -1)
                .toArray(File[]::new);

        for (int i = 0; i < outputFiles.length; i++) {
            String outputFilePath = outputFiles[i].getPath();
            String expectedFilePath = expectedFiles[i].getPath();
            String outputText = new String(Files.readAllBytes(Paths.get(outputFilePath)));
            String expectedText = new String(Files.readAllBytes(Paths.get(expectedFilePath)));
            arguments.add(
                    outputText.equals(expectedText)
                            ? Arguments.of(outputText, expectedText, name, i + 1)
                            : Arguments.of(
                                    sortOutput(outputFilePath),
                                    sortOutput(expectedFilePath),
                                    name + " (Sorted)",
                                    i + 1));
        }
    }

    public List<Arguments> queries() {
        return arguments;
    }
}
