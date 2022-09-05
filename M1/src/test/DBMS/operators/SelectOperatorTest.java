package DBMS.operators;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import DBMS.utils.Catalog;
import DBMS.utils.Helpers;
import net.sf.jsqlparser.expression.Expression;

class SelectOperatorTest {
    @BeforeAll
    public static void setup() throws IOException {
        Catalog.init("samples/input");
    }

    SelectOperator getOperator() throws FileNotFoundException {
        Expression exp= Helpers
            .expressionFromQuery("select * from Boats where Boats.D > 102 AND Boats.E != 1");
        ScanOperator scanOp= new ScanOperator("Boats");
        return new SelectOperator(scanOp, exp);
    }

    @Test
    void testGetNextTuple() throws IOException {
        SelectOperator selectOp= getOperator();

        assertEquals(Arrays.toString(new int[] { 104, 104, 2 }),
            selectOp.getNextTuple().toString());
        assertEquals(Arrays.toString(new int[] { 107, 2, 8 }),
            selectOp.getNextTuple().toString());
        assertNull(selectOp.getNextTuple());
    }

    @Test
    void testReset() throws IOException {
        SelectOperator selectOp= getOperator();

        assertEquals(Arrays.toString(new int[] { 104, 104, 2 }),
            selectOp.getNextTuple().toString());

        selectOp.reset();

        assertEquals(Arrays.toString(new int[] { 104, 104, 2 }),
            selectOp.getNextTuple().toString());
    }
}