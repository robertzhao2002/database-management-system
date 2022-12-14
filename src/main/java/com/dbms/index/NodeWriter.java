package com.dbms.index;

import com.dbms.utils.Attribute;
import com.dbms.utils.Catalog;
import com.dbms.utils.IO;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

/** Class for serializing leaf/index nodes and writing the header node. */
public class NodeWriter extends IO {

    /** Constructs a {@code NodeWriter} instance according to the given table and column names
     *
     * @param tableName     unaliased name of the table
     * @param attributeName name of the column
     * @throws FileNotFoundException */
    NodeWriter(Attribute c) throws FileNotFoundException {
        buffer = ByteBuffer.allocate(PAGE_SIZE);
        fout = new FileOutputStream(Catalog.pathToIndexFile(c));
        fc = fout.getChannel();
        bufferIndex = 8;
    }

    /** Writes the header node on the first page
     *
     * @param rootAddress the page number of the root node in the index tree
     * @param numLeaves   the number of leaves in the index tree
     * @param order       the order given by {@code index_info.txt}
     * @throws IOException */
    public void writeHeaderNode(int rootAddress, int numLeaves, int order) throws IOException {
        setChannelToPage(0);
        writeInt(rootAddress);
        writeInt(numLeaves);
        writeInt(order);
        writePage();
    }

    /** Writes leaf nodes to the buffer
     *
     * @param pageNumber the page to write the node on
     * @param entries    list of {@code DataEntry} containing the key and {@code RID}
     * @throws IOException */
    public void writeLeafNode(int pageNumber, List<DataEntry> entries) throws IOException {
        setChannelToPage(pageNumber);
        writeInt(0);
        writeInt(entries.size());
        for (DataEntry entry : entries) {
            writeInt(entry.key);
            writeInt(entry.rids.size());
            for (RID rid : entry.rids) {
                writeInt(rid.pageId);
                writeInt(rid.tupleId);
            }
        }
        writePage();
    }

    /** Write index nodes to the buffer
     *
     * @param pageNumber location in buffer to write the node
     * @param keys       smallest keys of the children excluding the smallest
     * @param addresses  location of children nodes
     * @throws IOException */
    public void writeIndexNode(int pageNumber, List<Integer> keys, List<Integer> addresses) throws IOException {
        setChannelToPage(pageNumber);
        writeInt(1);
        writeInt(keys.size());
        for (int k : keys) writeInt(k);
        for (int address : addresses) writeInt(address);
        writePage();
    }

    /** closes the buffer */
    public void close() throws IOException {
        fout.close();
        fc.close();
    }

    /** Writes num at the current bufferIndex and increments bufferIndex by 4
     *
     * @param num integer to write */
    private void writeInt(int num) {
        buffer.putInt(bufferIndex, num);
        bufferIndex += 4;
    }

    /** Writes a page into the buffer
     *
     * @throws IOException */
    private void writePage() throws IOException {
        fc.write(buffer);
        clearBuffer();
    }

    /** Sets channel to a given page
     *
     * @param pageNumber page numebr to set channel to
     * @throws IOException */
    private void setChannelToPage(int pageNumber) throws IOException {
        fc.position(PAGE_SIZE * pageNumber);
        clearBuffer();
        bufferIndex = 0;
    }
}
