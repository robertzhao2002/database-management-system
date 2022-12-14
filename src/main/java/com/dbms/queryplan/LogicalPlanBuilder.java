package com.dbms.queryplan;

import com.dbms.operators.logical.LogicalDuplicateEliminationOperator;
import com.dbms.operators.logical.LogicalJoinOperator;
import com.dbms.operators.logical.LogicalOperator;
import com.dbms.operators.logical.LogicalProjectOperator;
import com.dbms.operators.logical.LogicalScanOperator;
import com.dbms.operators.logical.LogicalSelectOperator;
import com.dbms.operators.logical.LogicalSortOperator;
import com.dbms.utils.Catalog;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.Distinct;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;

/** Builds a query plan from a Statement and stores the root operator. */
public class LogicalPlanBuilder {
    public LogicalOperator root;

    /** @param tableName (aliased) table name for the scan
     * @param exp       select expression, null if not filtered
     * @return scan operator if expression is null, otherwise a select operator */
    private LogicalOperator createScanAndSelect(String tableName, Expression exp) {
        LogicalOperator op = new LogicalScanOperator(tableName);
        if (exp != null) op = new LogicalSelectOperator(op, exp);
        return op;
    }

    /** Initializes a join operator after selection-pushing has been done
     *
     * @param tables list of join tables
     * @param uv     {@code UnionFindVisitor} for selection-pushing
     * @return {@code LogicalJoinOperator} that contains all the conditions of each children as
     *         select operators
     * @throws FileNotFoundException */
    private LogicalJoinOperator createJoinOperator(List<String> tables, UnionFindVisitor uv)
            throws FileNotFoundException {
        Map<String, LogicalOperator> children = new HashMap<>();
        for (String table : tables) {
            children.put(table, createScanAndSelect(table, uv.getExpression(table)));
        }
        return new LogicalJoinOperator(children, uv, tables);
    }

    /** Populates Catalog alias map if tables use aliases.
     *
     * @param from  from table
     * @param joins join tables, null if not a join
     * @return list of (aliased) table names in the order of tables provided */
    private List<String> extractNames(FromItem from, List<Join> joins) {
        LinkedList<FromItem> tables = joins != null
                ? joins.stream().map(j -> j.getRightItem()).collect(Collectors.toCollection(LinkedList::new))
                : new LinkedList<>();
        tables.addFirst(from);
        return Catalog.populateAliasMap(tables);
    }

    /** @param statement Statement for which to build a query plan and create a root operator
     * @throws FileNotFoundException */
    public LogicalPlanBuilder(Statement statement) throws FileNotFoundException {
        // extract relevant items from statement
        Select select = (Select) statement;
        PlainSelect body = (PlainSelect) select.getSelectBody();
        List<SelectItem> selectItems = body.getSelectItems();
        boolean isAllColumns = selectItems.get(0) instanceof AllColumns;
        Expression exp = body.getWhere();
        FromItem mainFromItem = body.getFromItem();
        List<Join> joins = body.getJoins();
        List<String> tableNames = extractNames(mainFromItem, joins);
        List<OrderByElement> orderByElements = body.getOrderByElements();
        Distinct distinct = body.getDistinct();

        LogicalOperator subRoot;
        if (joins != null) {
            UnionFindVisitor uv = new UnionFindVisitor();
            if (exp != null) exp.accept(uv);
            subRoot = createJoinOperator(tableNames, uv);
        } else {
            // if no joins, create a scan/select operator for the from table
            subRoot = createScanAndSelect(tableNames.get(0), exp);
        }

        // add if necessary: projection, sorting, duplicate elimination
        if (!isAllColumns) subRoot = new LogicalProjectOperator(subRoot, selectItems);
        subRoot = orderByElements != null || distinct != null
                ? new LogicalSortOperator(subRoot, orderByElements)
                : subRoot;
        root = distinct != null ? new LogicalDuplicateEliminationOperator(subRoot) : subRoot;
    }

    /** Writes this plan. Assumes a statement was already processed.
     *
     * @param i query number
     * @throws FileNotFoundException */
    public void writePlan(int i) throws FileNotFoundException {
        PrintWriter pw = new PrintWriter(Catalog.pathToOutputLogicalPlan(i));
        root.write(pw, 0);
        pw.close();
    }
}
