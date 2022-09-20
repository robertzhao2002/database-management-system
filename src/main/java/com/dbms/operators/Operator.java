package com.dbms.operators;

import com.dbms.utils.Tuple;
import java.io.FileWriter;
import java.io.IOException;

public abstract class Operator {

    public abstract Tuple getNextTuple();

    public abstract void reset();

    /**
     * @param writer the FileWriter to use to dump all Tuples of this operator to an output file
     * @throws IOException
     */
    public void dump(FileWriter writer) throws IOException {
        Tuple next;
        while ((next = getNextTuple()) != null) {
            writer.write(next.toString() + "\r\n");
        }
        writer.close();
    }
}
