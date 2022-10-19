package com.dbms.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/** Class for writing new tuples to a file in byte-code */
public class TupleWriter {

    /** Bytes per page */
    private static final int PAGE_SIZE = 4096;

    /** Number of attribute per tuple in current page */
    private int numAttributes;

    /** Number of tuples on current page */
    private int numTuples;

    /** Write buffer */
    private ByteBuffer buffer;

    /** The index at which to read the next integer in the buffer */
    private int bufferIndex;

    /** Output stream for the query */
    private FileOutputStream fout;

    /** Channel to write the buffer to the output stream */
    private FileChannel fc;

    /**
     * @param path (unaliased) file path name
     * @throws IOException
     */
    public TupleWriter(String path) throws IOException {
        buffer = ByteBuffer.allocate(PAGE_SIZE);
        fout = new FileOutputStream(path);
        fc = fout.getChannel();
        bufferIndex = 8;
    }

    /**
     * Writes tuple data to file path
     * @param t contains the data to write
     * @throws IOException
     */
    public void writeTuple(Tuple t) throws IOException {
        if (bufferIndex + t.size() * 4 > PAGE_SIZE) writePage();
        numAttributes = t.size();
        numTuples++;
        for (int v : t.getValues()) {
            buffer.putInt(bufferIndex, v);
            bufferIndex += 4;
        }
    }

    /**
     * Writes a page into the buffer
     * @throws IOException
     */
    private void writePage() throws IOException {
        buffer.putInt(0, numAttributes);
        buffer.putInt(4, numTuples);
        fc.write(buffer);
        clearBuffer();
        numAttributes = 0;
        numTuples = 0;
        bufferIndex = 8;
    }

    /** Clears the buffer by filling it with zeros and resetting the position to the front. */
    private void clearBuffer() {
        buffer.clear();
        buffer.put(new byte[PAGE_SIZE]); // hack to reset with zeros
        buffer.clear();
    }

    /**
     * Writes the buffer if tuples remaining and closes output writer.
     * @throws IOException
     */
    public void close() throws IOException {
        if (numTuples > 0) writePage();
        fout.close();
        fc.close();
    }
}
