# Database Management System

## Authors: [Daniel Jann](www.github.com/stressmaster), [Ben Kogan](www.github.com/thebenkogan), [Jacob Kraizman](c4pt41n-V0Yag3R), [Robert Zhao](www.github.com/robertzhao2002)

### Top-Level

The top level class of our code is [Interpreter.java](./src/main/java/com/dbms/Interpreter.java)

### Selection Pushing

The selection pushing is initiated in the [LogicalPlanBuilder.java](./src/main/java/com/dbms/queryplan/LogicalPlanBuilder.java), which creates a [UnionFindVisitor.java](./src/main/java/com/dbms/queryplan/UnionFindVisitor.java) to parse the WHERE expression and use the [UnionFind.java](./src/main/java/com/dbms/queryplan/UnionFind.java) to keep track of transitive equalities and the bounds.

We follow the expression visiting algorithm introduced in the handout. For each conjunct, we update the union find and keep track of any unusable expressions. We deviated slightly from the handout by also adding equalities between attributes to the unusable expressions data structure. This is because it is hard to extract this information directly from the union find, so by persisting the expression, we can easily add it back when creating the select/join condition. We will also note that we separate unusable join expressions from unusable select expressions. The difference lies in the number of tables referenced in the conjunct, and this distinction helps us easily extract one or the other without having to check the number of referenced tables later.

### Selection Implementation

The selection implementation is decided in the [PhysicalPlanBuilder.java](./src/main/java/com/dbms/queryplan/PhysicalPlanBuilder.java) when we visit a `LogicalSelectOperator`.

We followed the cost formulas in the handout, and we utilize the [Stats.java](./src/main/java/com/dbms/utils/Stats.java) class to perform most of the calculations. We start by getting all the indexes that are for the table being selected. If no indexes are available, then we cannot perform any optimizations and just make a scan and select operator. If there are indexes, then we start by getting the cost to scan the table directly. Then, for each index, we create an [IndexExpressionVisitor.java](./src/main/java/com/dbms/index/IndexExpressionVisitor.java) to parse the select condition and get the bounds for the index attribute. We compute the reduction factor and cost for using the index based on these bounds, and save the index with lowest cost. Finally, if the best index cost is lower than the scan cost, then we use the best index and create an [IndexScanOperator.java](./src/main/java/com/dbms/operators/physical/IndexScanOperator.java), potentially adding a select operator if some conjuncts are not covered by the index. Otherwise, we create a scan and select operator.

### Join Ordering

The join ordering is decided in the [JoinOrderOptimizer.java](./src/main/java/com/dbms/queryplan/JoinOrderOptimizer.java).

We followed the dynamic programming algorithm introduced in the handout. We iterate through increasing sizes of subsets. For each subset size, we generate all the combinations of table names, then compute their entry in the DP table. An entry contains the best join order, its associated cost, the size of the join (including the root), and the V-Values for each attribute in the join. Before creating this entry, we have to find the ordering of tables that minimizes the cost. To do this, we iterate through all possible tables to exclude from the current subset, and we obtain the cost by looking up the one-smaller subset in the DP table and adding it's cost and output size. Once we have the ordering with lowest cost, we compute the new output size and corresponding V-Values. These functions generally have three cases: single table scan, single table select, and a join between two tables. In each case, we follow the formulas in the handout. Instead of parsing the join expressions again to obtain the equalities, we make use of the union find to extract sets of equal attributes.

### Join Implementation

The join implementation is decided in the [PhysicalPlanBuilder.java](./src/main/java/com/dbms/queryplan/PhysicalPlanBuilder.java) via the `selectJoinImplementation` function.

We followed the simple strategy of using SMJ whenever possible. More specifically, if the join expression represents an equijoin, then we create an SMJ operator, otherwise, we create a BNLJ operator. We hardcoded 5 buffer pages for the BNLJ operator.

### Running the Application

To run the top-level application, use the command below.

```
./gradlew run --args="[config-file-path]"
```

### Formatting Code

To have unified formatting, we used a 3rd party formatting Gradle plugin called Spotless by Diffplug. We used the Palantir Java Formatting style. We have the formatter as a pre-commit hook, so it runs every time we commit. 

#### Applying the Formatting to All Java Files

To manually run the formatter, run the command below. NOTE: this will potentially change many files, if they aren't following the formatting guidelines properly.

```
./gradlew spotlessJavaApply
```

#### Verifying that the All Java Files are Formatted Properly

To check if your sourcecode follows the formatting guidelines, run the command below.

```
./gradlew spotlessJavaCheck
```

#### Committing without Formatting

```
git commit -am [message] --no-verify
```

### Unit Tests

#### Run all Unit Tests

```
./gradlew test
```

#### Run a Specific Set of Unit Tests

```
./gradlew test --tests [test-class-name]
```

#### Zip Command

```
make zip
```

### Acknowledgements

Third-party services used to facilitate the project:

* [Guava String Joiner](https://guava.dev/releases/18.0/api/docs/com/google/common/base/Joiner.html) for clean syntax of joining union-find strings with a new-line character
