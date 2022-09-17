package DBMS.operators;

import DBMS.utils.Tuple;
import DBMS.visitors.ExpressionParseVisitor;
import net.sf.jsqlparser.expression.Expression;

public class SelectOperator extends Operator {

    private Operator scanOperator;
    private ExpressionParseVisitor visitor= new ExpressionParseVisitor();

    /** select expression, Tuple is returned if this evaluates to true */
    private Expression exp;

    /** @param scanOperator child operator of SelectOperator
     * @param expression   the WHERE expression which we select for; is not null */
    public SelectOperator(Operator scanOperator, Expression expression) {
        this.scanOperator= scanOperator;
        this.exp= expression;
    }

    /** resets underlying scan operator */
    @Override
    public void reset() {
        scanOperator.reset();
    }

    /** @return the next tuple that passes the select expression */
    @Override
    public Tuple getNextTuple() {
        Tuple nextTuple= scanOperator.getNextTuple();
        if (nextTuple == null) return null;
        visitor.currentTuple= nextTuple;
        exp.accept(visitor);
        return visitor.getBooleanResult() ? nextTuple : getNextTuple();
    }

}
