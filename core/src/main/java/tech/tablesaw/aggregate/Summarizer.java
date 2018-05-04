/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tech.tablesaw.aggregate;

import com.google.common.collect.ArrayListMultimap;
import tech.tablesaw.api.CategoricalColumn;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import tech.tablesaw.table.SelectionTableSliceGroup;
import tech.tablesaw.table.StandardTableSliceGroup;
import tech.tablesaw.table.TableSliceGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Summarizes the data in a table, by applying functions to a subset of its columns.
 *
 * How to use:
 *
 * 1. Create an instance providing a source table, the column or columns to summarize, and a function or functions to apply
 * 2. Applying the functions to the designated columns, possibly creating subgroup summaries using one of the by() methods
 */
public class Summarizer {

    private final Table original;
    private final Table temp;
    private final List<String> summarizedColumns = new ArrayList<>();
    private final AggregateFunction[] reductions;

    /**
     * Returns an object capable of summarizing the given column in the given sourceTable,
     * by applying the given functions
     */
    public Summarizer(Table sourceTable, Column column, AggregateFunction... functions) {
        Table tempTable = Table.create(sourceTable.name());
        tempTable.addColumns(column);
        this.temp = tempTable;
        this.original = sourceTable;
        summarizedColumns.add(column.name());
        this.reductions = functions;
    }

    /**
     * Returns an object capable of summarizing the given column in the given sourceTable,
     * by applying the given functions
     */
    public Summarizer(Table sourceTable, List<String> columnNames, AggregateFunction... functions) {
        Table tempTable = Table.create(sourceTable.name());
        for (String nm : columnNames) {
            tempTable.addColumns(sourceTable.column(nm));
        }
        this.temp = tempTable;
        this.original = sourceTable;
        summarizedColumns.addAll(columnNames);
        this.reductions = functions;
    }

    /**
     * Returns an object capable of summarizing the given columns in the given sourceTable,
     * by applying the given functions
     */
    public Summarizer(Table sourceTable, Column column1, Column column2, AggregateFunction... functions) {
        Table tempTable = Table.create(sourceTable.name());
        tempTable.addColumns(column1);
        tempTable.addColumns(column2);
        this.temp = tempTable;
        this.original = sourceTable;
        summarizedColumns.add(column1.name());
        summarizedColumns.add(column2.name());
        this.reductions = functions;
    }

    /**
     * Returns an object capable of summarizing the given columns in the given sourceTable,
     * by applying the given functions
     */
    public Summarizer(Table sourceTable,
                      Column column1,
                      Column column2,
                      Column column3,
                      Column column4,
                      AggregateFunction... functions) {
        Table tempTable = Table.create(sourceTable.name());
        tempTable.addColumns(column1);
        tempTable.addColumns(column2);
        tempTable.addColumns(column3);
        tempTable.addColumns(column4);
        this.temp = tempTable;
        this.original = sourceTable;
        summarizedColumns.add(column1.name());
        summarizedColumns.add(column2.name());
        summarizedColumns.add(column3.name());
        summarizedColumns.add(column4.name());
        this.reductions = functions;
    }

    /**
     * Returns an object capable of summarizing the given column2 in the given sourceTable,
     * by applying the given functions
     */
    public Summarizer(Table sourceTable, Column column1, Column column2, Column column3, AggregateFunction... functions) {
        Table tempTable = Table.create(sourceTable.name());
        tempTable.addColumns(column1);
        tempTable.addColumns(column2);
        tempTable.addColumns(column3);
        this.temp = tempTable;
        this.original = sourceTable;
        summarizedColumns.add(column1.name());
        summarizedColumns.add(column2.name());
        summarizedColumns.add(column3.name());
        this.reductions = functions;
    }

    public Table by(String... columnNames) {
        for (String columnName : columnNames) {
            if (!temp.columnNames().contains(columnName)) {
                temp.addColumns(original.column(columnName));
            }
        }
        TableSliceGroup group = StandardTableSliceGroup.create(temp, columnNames);
        return summarize(group);
    }

    public Table by(CategoricalColumn... columns) {
        for (Column c : columns) {
            if (!temp.containsColumn(c)) {
                temp.addColumns(c);
            }
        }
        TableSliceGroup group = StandardTableSliceGroup.create(temp, columns);
        return summarize(group);
    }

    public Table by(String groupNameTemplate, int step) {
        TableSliceGroup group = SelectionTableSliceGroup.create(temp, groupNameTemplate, step);
        return summarize(group);
    }

    public Table byYu(String... columnNames) {
        for (String columnName : columnNames) {
            if (!temp.columnNames().contains(columnName)) {
                temp.addColumns(original.column(columnName));
            }
        }
        TableSliceGroup group = StandardTableSliceGroup.create(temp, columnNames);
        return summarizeYu(group);
    }

    public Table byYu(CategoricalColumn... columns) {
        for (Column c : columns) {
            if (!temp.containsColumn(c)) {
                temp.addColumns(c);
            }
        }
        TableSliceGroup group = StandardTableSliceGroup.create(temp, columns);
        return summarizeYu(group);
    }

    public Table byYu(String groupNameTemplate, int step) {
        TableSliceGroup group = SelectionTableSliceGroup.create(temp, groupNameTemplate, step);
        return summarizeYu(group);
    }

    /**
     * Returns the result of applying to the functions to all the values in the appropriate column
     */
    public Table apply() {
        List<Table> results = new ArrayList<>();
        ArrayListMultimap<String, AggregateFunction> reductionMultimap = getAggregateFunctionMultimap();

        for (String name : reductionMultimap.keys()) {
            List<AggregateFunction> reductions = reductionMultimap.get(name);
            Table table = TableSliceGroup.summaryTableName(temp);
            for (AggregateFunction function : reductions) {
                Column column = temp.column(name);
                double result = function.summarize(column);
                Column newColumn = DoubleColumn.create(TableSliceGroup.aggregateColumnName(name, function.functionName()));
                ((DoubleColumn) newColumn).append(result);
                table.addColumns(newColumn);
            }
            results.add(table);
        }
        return (combineTables(results));
    }

    /**
     * Returns the result of applying to the functions to all the values in the appropriate column
     */
    public Table applyYu() {
        List<Table> results = new ArrayList<>();
        ArrayListMultimap<String, AggregateFunction> reductionMultimap = getAggregateFunctionMultimapYu();

        for (String name : reductionMultimap.keys()) {
            List<AggregateFunction> reductions = reductionMultimap.get(name);
            Table table = TableSliceGroup.summaryTableName(temp);
            for (AggregateFunction function : reductions) {
                Column column = temp.column(name);
                double result = function.summarize(column);
                Column newColumn = DoubleColumn.create(TableSliceGroup.aggregateColumnName(name, function.functionName()));
                ((DoubleColumn) newColumn).append(result);
                table.addColumns(newColumn);
            }
            results.add(table);
        }
        return (combineTables(results));
    }

    /**
     * Associates the columns to be summarized with the functions that match their type. All valid combinations are used
     * @param group A table slice group
     * @return      A table containing a row of summarized data for each group in the table slice group
     */
    private Table summarizeYu(TableSliceGroup group) {
        List<Table> results = new ArrayList<>();

        ArrayListMultimap<String, AggregateFunction> reductionMultimap = getAggregateFunctionMultimapYu();

        for (String name : reductionMultimap.keys()) {
            List<AggregateFunction> reductions = reductionMultimap.get(name);
            results.add(group.aggregate(name, reductions.toArray(new AggregateFunction[0])));
        }
        return combineTables(results);
    }
    
    /**
     * Associates the columns to be summarized with the functions that match their type. All valid combinations are used
     * @param group A table slice group
     * @return      A table containing a row of summarized data for each group in the table slice group
     */
    private Table summarize(TableSliceGroup group) {
        List<Table> results = new ArrayList<>();

        ArrayListMultimap<String, AggregateFunction> reductionMultimap = getAggregateFunctionMultimap();

        for (String name : reductionMultimap.keys()) {
            List<AggregateFunction> reductions = reductionMultimap.get(name);
            results.add(group.aggregate(name, reductions.toArray(new AggregateFunction[0])));
        }
        return combineTables(results);
    }

    private ArrayListMultimap<String, AggregateFunction> getAggregateFunctionMultimapYu() {
        ArrayListMultimap<String, AggregateFunction> reductionMultimap = ArrayListMultimap.create();
        int index=0;
        if(summarizedColumns.size()!=reductions.length) {
        	throw new IllegalArgumentException(reductions.length + " length of aggregate function not same as columns size"+this.summarizedColumns.size());
        }
        for (String name: summarizedColumns) {
            Column column = temp.column(name);
            ColumnType type = column.type();
            AggregateFunction reduction = reductions[index];            
            if (reduction.isCompatibleWith(type)) {
            	reductionMultimap.put(name, reduction);
            }else {
            	throw new IllegalArgumentException(reduction + " is not compatible with column type:"+type);
            }
            index = index+1;
        }
        return reductionMultimap;
    }

    private ArrayListMultimap<String, AggregateFunction> getAggregateFunctionMultimap() {
        ArrayListMultimap<String, AggregateFunction> reductionMultimap = ArrayListMultimap.create();
        for (String name: summarizedColumns) {
            Column column = temp.column(name);
            ColumnType type = column.type();
            for (AggregateFunction reduction : reductions) {
                if (reduction.isCompatibleWith(type)) {
                    reductionMultimap.put(name, reduction);
                }
            }
        }
        return reductionMultimap;
    }

    private Table combineTables(List<Table> tables) {
        if (tables.size() == 1) {
            return tables.get(0);
        }

        Table result = null;
        for (Table table : tables) {
            if (result == null) {
                result = table;
            } else {
                for (Column column : table.columns()) {
                    if (!result.columnNames().contains(column.name())) {
                        result.addColumns(column);
                    }
                }
            }
        }
        return result;
    }
}
