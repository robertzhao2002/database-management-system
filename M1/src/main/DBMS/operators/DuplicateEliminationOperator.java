package DBMS.operators;

import DBMS.utils.Tuple;

/** An operator that assumes its child returns Tuples in sorted order and filters out any
 * duplicates. */
public class DuplicateEliminationOperator extends Operator {
    Operator child;

    /** previous Tuple returned */
    Tuple prev= null;

    /** @param child child operator; must be a sort operator */
    public DuplicateEliminationOperator(Operator child) {
        this.child= child;
    }

    /** @return next unique Tuple */
    @Override
    public Tuple getNextTuple() {
        Tuple next;
        while ((next= child.getNextTuple()) != null && next.equals(prev)) {}
        prev= next;
        return next;
    }

    /** resets the child operator and sets prev to null */
    @Override
    public void reset() {
        child.reset();
        prev= null;
    }
}
