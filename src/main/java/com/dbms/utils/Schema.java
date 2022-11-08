package com.dbms.utils;

import java.util.LinkedList;
import java.util.List;

/** Wrapper class for {@code List<ColumnName>}, providing useful methods for building schemas. */
public class Schema {
    /** representation of a schema */
    private List<Attribute> schema;

    /** @param wraps a list of column names in a schema */
    public Schema(List<Attribute> schema) {
        this.schema = schema;
    }

    /** @return list of column names represented by this schema */
    public List<Attribute> get() {
        return schema;
    }

    /** @return number of columns in this schema */
    public int size() {
        return schema.size();
    }

    /** @param tableName (aliased) table name
     * @param columnNames list of column names associated with the table
     * @return schema representing all column names from tableName x columnNames */
    public static Schema from(String tableName, List<String> columnNames) {
        List<Attribute> s = new LinkedList<>();
        for (String col : columnNames) {
            s.add(Attribute.bundle(tableName, col));
        }
        return new Schema(s);
    }

    /** @param s1 left schema
     * @param s2 right schema
     * @return joined schema with all left schema columns then right schema columns */
    public static Schema join(Schema s1, Schema s2) {
        List<Attribute> s = new LinkedList<>();
        s1.get().forEach(cn -> s.add(cn));
        s2.get().forEach(cn -> s.add(cn));
        return new Schema(s);
    }
}
