package com.dbms.operators.logical;

import static com.dbms.utils.Helpers.writeLevel;

import com.dbms.queryplan.PhysicalPlanBuilder;
import java.io.IOException;
import java.io.PrintWriter;
import net.sf.jsqlparser.expression.Expression;

/** The logical representation of the select operator, which contains the child operator and
 * expression that we use to construct the physical operator */
public class LogicalSelectOperator extends LogicalOperator {

    /** {@code child} is the {@code LogicalOperator} for the child operation of the
     * {@code LogicalSelectOperator} */
    public LogicalOperator child;

    /** {@code exp} is the expression containing columns to select from */
    public Expression exp;

    /** @param logicalScan child operator of SelectOperator
     * @param exp         the WHERE expression which we select for; is not null */
    public LogicalSelectOperator(LogicalOperator logicalScan, Expression exp) {
        child = logicalScan;
        this.exp = exp;
    }

    /** @param physicalPlan visitor which converts logical to physical operator
     * @throws IOException */
    @Override
    public void accept(PhysicalPlanBuilder physicalPlan) {
        try {
            physicalPlan.visit(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void write(PrintWriter pw, int level) {
        String s = String.format("Select[%s]", exp.toString());
        pw.println(writeLevel(s, level));
        child.write(pw, level + 1);
    }
}
