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

package tech.tablesaw.columns.strings;

import tech.tablesaw.api.StringColumn;
import tech.tablesaw.columns.ColumnReference;
import tech.tablesaw.columns.strings.filters.ColumnContainsString;
import tech.tablesaw.columns.strings.filters.ColumnEndsWith;
import tech.tablesaw.columns.strings.filters.ColumnEqualTo;
import tech.tablesaw.columns.strings.filters.ColumnEqualToIgnoringCase;
import tech.tablesaw.columns.strings.filters.ColumnNotEqualTo;
import tech.tablesaw.columns.strings.filters.ColumnStartsWith;
import tech.tablesaw.columns.strings.filters.ContainsString;
import tech.tablesaw.columns.strings.filters.EndsWith;
import tech.tablesaw.columns.strings.filters.EqualTo;
import tech.tablesaw.columns.strings.filters.EqualToIgnoringCase;
import tech.tablesaw.columns.strings.filters.HasLengthEqualTo;
import tech.tablesaw.columns.strings.filters.IsAlpha;
import tech.tablesaw.columns.strings.filters.IsAlphaNumeric;
import tech.tablesaw.columns.strings.filters.IsEmptyString;
import tech.tablesaw.columns.strings.filters.IsIn;
import tech.tablesaw.columns.strings.filters.IsLongerThan;
import tech.tablesaw.columns.strings.filters.IsLowerCase;
import tech.tablesaw.columns.strings.filters.IsNotIn;
import tech.tablesaw.columns.strings.filters.IsNumeric;
import tech.tablesaw.columns.strings.filters.IsShorterThan;
import tech.tablesaw.columns.strings.filters.IsUpperCase;
import tech.tablesaw.columns.strings.filters.MatchesRegex;
import tech.tablesaw.columns.strings.filters.NotEqualTo;
import tech.tablesaw.columns.strings.filters.StartsWith;
import tech.tablesaw.filtering.Filter;

import java.io.Serializable;
import java.util.List;

/**
 * A reference to a column that can be used in evaluating query predicates. It is a key part of having a fluent API
 * for querying tables.
 * <p>
 * Basically, it lets you write a query like this:
 * <p>
 * table.select(column("foo").isEqualTo("Bar"));
 * <p>
 * In that example, column() is a static method that returns a ColumnReference for a column named "foo".
 * The method isEqualTo(), is implemented on ColumnReference in a way that it can be applied to potentially, multiple
 * column types, although in this case, it only makes sense for CategoryColumns since the argument is a string.
 * <p>
 * When select() isExecuted, it supplies the table to the ColumnReference. The ColumnReference uses the table
 * and columnName to get access to the right column, and then fulfils its role by ensuring that the filtering
 * "isEqualTo("Bar") is applied to all the cells in the column.
 */
public class StringColumnReference extends ColumnReference implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public StringColumnReference(String column) {
        super(column);
    }

    public Filter isEqualTo(String value) {
        return new EqualTo(this, value);
    }

    public Filter isNotEqualTo(String value) {
        return new NotEqualTo(this, value);
    }

    public Filter isIn(String... strings) {
        return new IsIn(this, strings);
    }

    public Filter isNotIn(String... strings) {
        return new IsNotIn(this, strings);
    }

    public Filter isIn(List<String> strings) {
        return new IsIn(this, strings);
    }

    public Filter isNotIn(List<String> strings) {
        return new IsNotIn(this, strings);
    }

    public Filter isUpperCase() {
        return new IsUpperCase(this);
    }

    public Filter isLowerCase() {
        return new IsLowerCase(this);
    }

    public Filter isAlpha() {
        return new IsAlpha(this);
    }

    public Filter isAlphaNumeric() {
        return new IsAlphaNumeric(this);
    }

    public Filter isNumeric() {
        return new IsNumeric(this);
    }

    public Filter isEmptyString() {
        return new IsEmptyString(this);
    }

    public Filter isLongerThan(int length) {
        return new IsLongerThan(this, length);
    }

    public Filter isShorterThan(int length) {
        return new IsShorterThan(this, length);
    }

    public Filter lengthEquals(int length) {
        return new HasLengthEqualTo(this, length);
    }

    public Filter equalsIgnoreCase(String string) {
        return new EqualToIgnoringCase(this, string);
    }

    public Filter startsWith(String string) {
        return new StartsWith(this, string);
    }

    public Filter endsWith(String string) {
        return new EndsWith(this, string);
    }

    public Filter containsString(String string) {
        return new ContainsString(this, string);
    }

    public Filter containsString(StringColumn sc) {
        return new ColumnContainsString(this, sc);
    }

    public Filter matchesRegex(String string) {
        return new MatchesRegex(this, string);
    }

    public Filter startsWith(StringColumn sc) {
        return new ColumnStartsWith(this, sc);
    }

    public Filter startsWith(StringColumnReference ref) {
        return new ColumnStartsWith(this, ref);
    }

    public Filter endsWith(StringColumn sc) {
        return new ColumnEndsWith(this, sc);
    }

    public Filter endsWith(StringColumnReference ref) {
        return new ColumnEndsWith(this, ref);
    }

    public Filter isEqualTo(StringColumn value) {
        return new ColumnEqualTo(this, value);
    }

    public Filter isEqualTo(StringColumnReference value) {
        return new ColumnEqualTo(this, value);
    }

    public Filter isNotEqualTo(StringColumn value) {
        return new ColumnNotEqualTo(this, value);
    }

    public Filter isNotEqualTo(StringColumnReference value) {
        return new ColumnNotEqualTo(this, value);
    }

    public Filter equalsIgnoreCase(StringColumn stringColumnToCompareAgainst) {
        return new ColumnEqualToIgnoringCase(this, stringColumnToCompareAgainst);
    }

    public Filter equalsIgnoreCase(StringColumnReference stringColumnToCompareAgainst) {
        return new ColumnEqualToIgnoringCase(this, stringColumnToCompareAgainst);
    }

}
