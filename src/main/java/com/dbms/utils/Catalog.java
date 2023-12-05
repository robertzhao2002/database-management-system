package com.dbms.utils;

import com.dbms.index.Index;
import com.dbms.index.TreeIndexBuilder;
import io.github.pixee.security.BoundedLineReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItem;

/** A static class responsible for storing the information of the input queries file, schema file,
 * and DB files. Also creates and maintains an alias map so that all parts of the DBMS can easily
 * retrieve the full table name for a given alias. */
public class Catalog {

    /** path to input directory */
    private static String input;

    /** path to output directory */
    private static String output;

    /** path to temp directory */
    private static String temp;

    /** Map from (unaliased) table name to list of {@code Attribute} objects */
    private static Map<String, List<Attribute>> schema;

    /** Map of aliases to real table names */
    private static Map<String, String> aliasMap = new HashMap<>();

    /** Maps unaliased table name to the indexes associated with that table */
    public static Map<String, List<Index>> INDEXES;

    /** Stats about all the tables in the database: number of rows, number of attributes, and
     * min/max of attributes */
    public static Stats STATS;

    /** Number of pages to use in external sort */
    public static int EXTPages = 5;

    /** Number of pages to use in BNLJ */
    public static int BNLJPages = 5;

    /** @param segments file path to join
     * @return segments joined with File.seperator */
    private static String join(String... segments) {
        return String.join(File.separator, segments);
    }

    /** @param path path to file
     * @return BufferedReader for the file at path
     * @throws FileNotFoundException */
    private static BufferedReader readerFromPath(String... path) throws FileNotFoundException {
        return new BufferedReader(new FileReader(join(path)));
    }

    /** Initializes the catalog with the info from the file in {@code path}
     *
     * @param path file containing the configuration info
     * @throws IOException */
    public static void init(String path) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(path));
        Catalog.input = BoundedLineReader.readLine(br, 5_000_000);
        Catalog.output = BoundedLineReader.readLine(br, 5_000_000);
        Catalog.temp = BoundedLineReader.readLine(br, 5_000_000);
        br.close();
        schema = getSchema(Catalog.input);
        INDEXES = getIndexInfo(readerFromPath(Catalog.input, "db", "index_info.txt"));
        STATS = new Stats(new BufferedWriter(new FileWriter(join(input, "db", "stats.txt"))), schema);
    }

    public static void setSchema(Map<String, List<Attribute>> s) {
        schema = s;
    }

    /** Initializes {@code Catalog.INDEXES}
     *
     * @param br reader for reading {@code index_info.txt} file
     * @throws IOException */
    private static Map<String, List<Index>> getIndexInfo(BufferedReader br) throws IOException {
        Map<String, List<Index>> indexes = new HashMap<>();
        String line;
        while ((line = BoundedLineReader.readLine(br, 5_000_000)) != null) {
            String info[] = line.split(" ");
            String table = info[0];
            String column = info[1];
            boolean cluster = Integer.parseInt(info[2]) == 1;
            int order = Integer.parseInt(info[3]);
            Attribute c = Attribute.bundle(table, column);
            if (!indexes.containsKey(table)) indexes.put(table, new LinkedList<>());
            indexes.get(table).add(new Index(c, order, cluster));
        }
        br.close();
        return indexes;
    }

    /** Serialize all indexes in INDEXES */
    public static void serializeIndexes() {
        INDEXES.values().forEach(indexes -> indexes.forEach(index -> TreeIndexBuilder.serialize(index)));
    }

    /** Creates a map to set {@code Catalog.schema} to
     *
     * @param input file path of schema file
     * @return schema map containing schema info
     * @throws IOException */
    private static Map<String, List<Attribute>> getSchema(String input) throws IOException {
        Map<String, List<Attribute>> schemaMap = new HashMap<>();
        BufferedReader schemaBr = readerFromPath(input, "db", "schema.txt");
        String line;
        while ((line = BoundedLineReader.readLine(schemaBr, 5_000_000)) != null) {
            StringTokenizer table = new StringTokenizer(line, " ");
            String tableName = table.nextToken();
            List<Attribute> attributes = new LinkedList<>();
            while (table.hasMoreTokens()) {
                attributes.add(Attribute.bundle(tableName, table.nextToken()));
            }
            schemaMap.put(tableName, attributes);
        }
        schemaBr.close();
        return schemaMap;
    }

    /** @param name (unaliased) name of the table to lookup
     * @return BufferedReader for the table
     * @throws FileNotFoundException */
    public BufferedReader getTable(String name) throws FileNotFoundException {
        return readerFromPath(input, "db", "data", name);
    }

    /** @param tableName (unaliased) table name
     * @return path to table file in input directory */
    public static String pathToTable(String tableName) {
        return join(input, "db", "data", tableName);
    }

    /** @param path path to temp file within temp directory
     * @return updated path with temp directory prepended */
    public static String pathToTempFile(String path) {
        return join(temp, path);
    }

    /** @param i query number
     * @return path to output file */
    public static String pathToOutputFile(int i) {
        return join(output, "query" + i);
    }

    /** @param i query number
     * @return path to output file */
    public static String pathToOutputLogicalPlan(int i) {
        return join(output, "query" + i + "_logicalplan");
    }

    /** @param i query number
     * @return path to output file */
    public static String pathToOutputPhysicalPlan(int i) {
        return join(output, "query" + i + "_physicalplan");
    }

    /** @param tableName unaliased table name
     * @param attributeName column name
     * @return {@code String} of file path to indexes */
    public static String pathToIndexFile(Attribute c) {
        return join(input, "db", "indexes", c.TABLE + "." + c.COLUMN);
    }

    /** @param name (unaliased) name of the table to extract columns
     * @return list of {@code Attribute} */
    public static List<Attribute> getAttributes(String name) {
        return schema.get(name);
    }

    /** @param name (aliased) name of the table to extract columns
     * @return list of {@code Attribute} with (aliased) table names */
    public static List<Attribute> getAliasedAttributes(String name) {
        List<Attribute> attributes = schema.get(getRealTableName(name));
        return attributes.stream().map(a -> a.alias(name)).collect(Collectors.toCollection(LinkedList::new));
    }

    /** @param cn (unaliased) table name and associated column name
     * @return 0-based index of the column in the schema */
    public static int getColumnIndex(Attribute cn) {
        return schema.get(cn.TABLE).indexOf(cn);
    }

    /** If fromItems use aliases, this populates the aliasMap and returns the aliased names.
     * Otherwise, this returns the real table names.
     *
     * @param fromItems tables that may or may not use aliases
     * @return list of (aliased) table names in fromItems */
    public static List<String> populateAliasMap(List<FromItem> fromItems) {
        boolean usingAliases = fromItems.get(0).getAlias() != null;
        LinkedList<String> tableNames = new LinkedList<>();
        for (FromItem fromItem : fromItems) {
            Table table = (Table) fromItem;
            String tableName = usingAliases ? table.getAlias().getName() : table.getName();
            if (usingAliases) aliasMap.put(tableName, table.getName());
            tableNames.add(tableName);
        }
        return tableNames;
    }

    /** @param name table name (aliased)
     * @return the actual table name corresponding to the input */
    public static String getRealTableName(String name) {
        return aliasMap.containsKey(name) ? aliasMap.get(name) : name;
    }

    /** @return BufferedReader for the query file corresponding to the Catalog input. Each line will
     *         be the query as a string.
     * @throws FileNotFoundException */
    public static BufferedReader getQueriesFile() throws FileNotFoundException {
        return new BufferedReader(new FileReader(String.join(File.separator, input, "queries.sql")));
    }

    /** Creates a sub-directory in the given {@code temp} folder.
     *
     * @param id is a UUID to name the new directory
     * @throws IOException */
    public static void createTempSubDir(String id) throws IOException {
        new File(join(temp, id)).mkdir();
    }

    /** Deletes everything in the temp directory when application is finished running
     *
     * @throws IOException */
    public static void cleanTempDir() throws IOException {
        Files.walkFileTree(Paths.get(temp), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                try {
                    Files.delete(file);
                } catch (Exception e) {
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                try {
                    Files.delete(dir);
                } catch (Exception e) {
                }
                return FileVisitResult.CONTINUE;
            }
        });
        new File(temp).mkdir();
    }

    /** @param indexName (unaliased) table name of index
     * @return the indexes associated with the table, null if none */
    public static List<Index> getIndexes(String tableName) {
        return INDEXES.get(tableName);
    }
}
