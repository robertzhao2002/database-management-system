package com.dbms.utils;

import java.util.LinkedList;
import java.util.List;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;

/** A static class that provides helpful functions for converting string SQL segments to JsqlParser
 * segments, and other useful functions for expressions. */
public class Helpers {

    /** @param query The string representation of a query
     * @return the query (PlainSelect) which the string represents */
    private static PlainSelect convertQuery(String query) {
        try {
            return (PlainSelect) ((Select) CCJSqlParserUtil.parse(query)).getSelectBody();
        } catch (JSQLParserException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** @param exp The string representation of an expression
     * @return the Expression which the string represents */
    public static Expression strExpToExp(String exp) {
        return convertQuery("select * from t where " + exp).getWhere();
    }

    /** @param orderBys A list of string representations of OrderByElements
     * @return the list of OrderByElements corresponding to the input strings */
    public static List<OrderByElement> strOrderBysToOrderBys(String... orderBys) {
        return convertQuery("select * from t order by " + String.join(", ", orderBys))
                .getOrderByElements();
    }

    /** @param selectItems A list of string representations of SelectItems
     * @return the list of SelectItems corresponding to the input strings */
    public static List<SelectItem> strSelectItemsToSelectItems(String... selectItems) {
        return convertQuery("select " + String.join(", ", selectItems) + " from t")
                .getSelectItems();
    }

    /** @param exp The expression to be added to an AndExpression
     * @return the AndExpression whose rightExpression is exp and leftExpression is empty */
    public static AndExpression wrapExpressionWithAnd(Expression exp) {
        if (exp instanceof AndExpression) return (AndExpression) exp;
        AndExpression andExp = new AndExpression(null, exp);
        return andExp;
    }

    /** @param exps the list of expressions, not null
     * @return left-deep AND Expression comprised of joined expList, null if exps is empty */
    public static Expression wrapListOfExpressions(List<Expression> exps) {
        if (exps.isEmpty()) return null;
        if (exps.size() == 1) {
            return exps.get(0);
        }
        AndExpression and = new AndExpression(exps.get(0), exps.get(1));
        for (int i = 2; i < exps.size(); i++) {
            and = new AndExpression(and, exps.get(i));
        }
        return and;
    }

    /** Wrapper for grabbing the proper name of a table in String form.
     *
     * @param table Table to get the name of.
     * @return alias name as a String if there is one, original name if no alias. */
    public static String getProperTableName(Table table) {
        return table.getAlias() != null ? table.getAlias().getName() : table.getName();
    }

    /** @param selectItems select items in the query
     * @return list of (aliased) table names and column names associated with the items */
    public static List<Attribute> getColumnNamesFromSelectItems(List<SelectItem> selectItems) {
        List<Attribute> names = new LinkedList<>();
        for (SelectItem item : selectItems) {
            Column col = (Column) ((SelectExpressionItem) item).getExpression();
            String tableName = getProperTableName(col.getTable());
            names.add(Attribute.bundle(tableName, col.getColumnName()));
        }
        return names;
    }

    /** Retrieves all the EqualTo conditions from a given expression. Precondition: all
     * sub-expressions are EqualTo expressions.
     *
     * @param exp The Expression associated with the JoinOperator. Precondition: exp is an
     *            AndExpression, not null
     * @return a list of EqualsTo expressions in the EquiJoin */
    public static List<EqualsTo> getEqualityConditions(Expression exp) {
        AndExpression and = wrapExpressionWithAnd(exp);
        List<EqualsTo> result = new LinkedList<>();

        while (and.getRightExpression() != null) {
            Expression nextAnd = and.getLeftExpression();
            result.add((EqualsTo) and.getRightExpression());
            if (nextAnd instanceof EqualsTo) {
                result.add((EqualsTo) and.getLeftExpression());
                return result;
            }
            if (nextAnd == null) return result;
            and = (AndExpression) nextAnd;
        }

        return result;
    }

    /** @param exp The Expression associated with the JoinOperator.
     * @return {@code true} when {@code exp} has only {@code EqualsTo} conditions, {@code false}
     *         otherwise */
    public static boolean isEquiJoin(Expression exp) {
        if (exp == null) return false;
        AndExpression and = wrapExpressionWithAnd(exp);
        while (and.getRightExpression() != null) {
            Expression nextAnd = and.getLeftExpression();
            Expression next = and.getRightExpression();
            if (!(next instanceof EqualsTo)) return false;
            if (nextAnd == null || nextAnd instanceof EqualsTo) return true;
            if (!(nextAnd instanceof AndExpression)) return false;
            and = (AndExpression) nextAnd;
        }
        return true;
    }

    /** @param s string to print at this level
     * @param level level in the query plan tree, 0-based
     * @return level number of dashes (-) concatenated with s */
    public static String writeLevel(String s, int level) {
        String dashes = "";
        for (int i = 0; i < level; i++) dashes += "-";
        return dashes + s;
    }

    /** Just a shorter version of {@code Expression.toString()}
     *
     * @param e given {@code Expression} (can be null)
     * @return either {@code "null"} or the integer value as a string */
    public static String str(Object o) {
        return o == null ? "null" : o.toString();
    }
}
