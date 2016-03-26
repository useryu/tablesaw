package com.deathrayresearch.outlier.columns;

import com.deathrayresearch.outlier.Table;
import com.deathrayresearch.outlier.io.TypeUtils;
import com.deathrayresearch.outlier.mapper.IntMapUtils;
import com.deathrayresearch.outlier.sorting.IntComparisonUtil;
import com.deathrayresearch.outlier.store.ColumnMetadata;
import com.deathrayresearch.outlier.util.StatUtil;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.ints.IntIterator;
import org.roaringbitmap.RoaringBitmap;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A column that contains signed 4 byte integer values
 */
public class IntColumn extends AbstractColumn implements IntMapUtils {

  public static final int MISSING_VALUE = (int) ColumnType.INTEGER.getMissingValue();
  private static final int DEFAULT_ARRAY_SIZE = 128;

  private IntArrayList data;

  public static IntColumn create(String name) {
    return new IntColumn(name, DEFAULT_ARRAY_SIZE);
  }

  public static IntColumn create(ColumnMetadata metadata) {
    return new IntColumn(metadata);
  }

  public static IntColumn create(String name, int arraySize) {
    return new IntColumn(name, arraySize);
  }

  public static IntColumn create(String name, IntArrayList ints) {
    IntColumn column = new IntColumn(name, ints.size());
    column.data = ints;
    return column;
  }

  public IntColumn(String name, int initialSize) {
    super(name);
    data = new IntArrayList(initialSize);
  }

  public IntColumn(ColumnMetadata metadata) {
    super(metadata);
    data = new IntArrayList(metadata.getSize());
  }

  public IntArrayList data() {
    return data;
  }

  public IntColumn(String name) {
    super(name);
    data = new IntArrayList(DEFAULT_ARRAY_SIZE);
  }

  public int size() {
    return data.size();
  }

  @Override
  public ColumnType type() {
    return ColumnType.INTEGER;
  }

  public long sum() {
    long sum = 0;
    for (int i : data) {
      sum += i;
    }
    return sum;
  }

  public void add(int i) {
    data.add(i);
  }

  public void set(int index, int value) {
    data.set(index, value);
  }

  public RoaringBitmap isLessThan(int f) {
    RoaringBitmap results = new RoaringBitmap();
    int i = 0;
    for (int next : data) {
      if (next < f) {
        results.add(i);
      }
      i++;
    }
    return results;
  }

  public RoaringBitmap isGreaterThan(int f) {
    RoaringBitmap results = new RoaringBitmap();
    int i = 0;
    for (int next : data) {
      if (next > f) {
        results.add(i);
      }
      i++;
    }
    return results;
  }

  public RoaringBitmap isGreaterThanOrEqualTo(int f) {
    RoaringBitmap results = new RoaringBitmap();
    int i = 0;
    for (int next : data) {
      if (next >= f) {
        results.add(i);
      }
      i++;
    }
    return results;
  }

  public RoaringBitmap isLessThanOrEqualTo(int f) {
    RoaringBitmap results = new RoaringBitmap();
    int i = 0;
    for (int next : data) {
      if (next <= f) {
        results.add(i);
      }
      i++;
    }
    return results;
  }

  public RoaringBitmap isEqualTo(int f) {
    RoaringBitmap results = new RoaringBitmap();
    int i = 0;
    for (int next : data) {
      if (next == f) {
        results.add(i);
      }
      i++;
    }
    return results;
  }

  @Override
  public Table summary() {
    return StatUtil.stats(this).asTable(name());
  }

  @Override
  public int countUnique() {
    RoaringBitmap roaringBitmap = new RoaringBitmap();
    data.forEach(roaringBitmap::add);
    return roaringBitmap.getCardinality();
  }

  @Override
  public IntColumn unique() {
    RoaringBitmap roaringBitmap = new RoaringBitmap();
    data.forEach(roaringBitmap::add);
    return IntColumn.create(name() + " Unique values", IntArrayList.wrap(roaringBitmap.toArray()));
  }

  @Override
  public String getString(int row) {
    return String.valueOf(data.getInt(row));
  }

  @Override
  public IntColumn emptyCopy() {
    return new IntColumn(name(), DEFAULT_ARRAY_SIZE);
  }

  @Override
  public void clear() {
    data.clear();
  }

  @Override
  public void sortAscending() {
    Arrays.parallelSort(data.elements());
  }

  @Override
  public void sortDescending() {
    IntArrays.parallelQuickSort(data.elements(), reverseIntComparator);
  }

  IntComparator reverseIntComparator =  new IntComparator() {

    @Override
    public int compare(Integer o2, Integer o1) {
      return (o1 < o2 ? -1 : (o1.equals(o2) ? 0 : 1));
    }

    @Override
    public int compare(int o2, int o1) {
      return (o1 < o2 ? -1 : (o1 == o2 ? 0 : 1));
    }
  };


  private IntColumn copy() {
    return IntColumn.create(name(), data);
  }

  @Override
  public boolean isEmpty() {
    return data.isEmpty();
  }

  @Override
  public void addCell(String object) {
    try {
      add(convert(object));
    } catch (NumberFormatException nfe) {
      throw new NumberFormatException(name() + ": " + nfe.getMessage());
    } catch (NullPointerException e) {
      throw new RuntimeException(name() + ": "
          + String.valueOf(object) + ": "
          + e.getMessage());
    }
  }

  /**
   * Returns a float that is parsed from the given String
   * <p>
   * We remove any commas before parsing
   */
  public static int convert(String stringValue) {
    if (Strings.isNullOrEmpty(stringValue) || TypeUtils.MISSING_INDICATORS.contains(stringValue)) {
      return (int) ColumnType.INTEGER.getMissingValue();
    }
    Matcher matcher = COMMA_PATTERN.matcher(stringValue);
    return Integer.parseInt(matcher.replaceAll(""));
  }

  private static final Pattern COMMA_PATTERN = Pattern.compile(",");

  public int get(int index) {
    return data.getInt(index);
  }

  @Override
  public it.unimi.dsi.fastutil.ints.IntComparator rowComparator() {
    return comparator;
  }

  final it.unimi.dsi.fastutil.ints.IntComparator comparator = new it.unimi.dsi.fastutil.ints.IntComparator() {

    @Override
    public int compare(Integer i1, Integer i2) {
      return compare((int) i1, (int) i2);
    }

    public int compare(int i1, int i2) {
      int prim1 = get(i1);
      int prim2 = get(i2);
      return IntComparisonUtil.getInstance().compare(prim1, prim2);
    }
  };

  public int max() {
    return StatUtil.max(this);
  }

  public int min() {
    return StatUtil.min(this);
  }

  public int firstElement() {
    if (size() > 0) {
      return get(0);
    }
    return MISSING_VALUE;
  }

  public RoaringBitmap isPositive() {
    RoaringBitmap results = new RoaringBitmap();
    int i = 0;
    for (int next : data) {
      if (next > 0) {
        results.add(i);
      }
      i++;
    }
    return results;
  }

  public RoaringBitmap isNegative() {
    RoaringBitmap results = new RoaringBitmap();
    int i = 0;
    for (int next : data) {
      if (next < 0) {
        results.add(i);
      }
      i++;
    }
    return results;
  }

  public RoaringBitmap isNonNegative() {
    RoaringBitmap results = new RoaringBitmap();
    int i = 0;
    for (int next : data) {
      if (next >= 0) {
        results.add(i);
      }
      i++;
    }
    return results;
  }

  public RoaringBitmap isZero() {
    RoaringBitmap results = new RoaringBitmap();
    int i = 0;
    for (int next : data) {
      if (next == 0) {
        results.add(i);
      }
      i++;
    }
    return results;
  }

  public RoaringBitmap isEven() {
    RoaringBitmap results = new RoaringBitmap();
    int i = 0;
    for (int next : data) {
      if ((next & 1) == 0) {
        results.add(i);
      }
      i++;
    }
    return results;
  }

  public RoaringBitmap isOdd() {
    RoaringBitmap results = new RoaringBitmap();
    int i = 0;
    for (int x : data) {
      if ((x & 1) != 0) {
        results.add(i);
      }
      i++;
    }
    return results;
  }

  public FloatArrayList toFloatArray() {
    FloatArrayList output = new FloatArrayList(data.size());
    for (int aData : data) {
      output.add(aData);
    }
    return output;
  }

  public String print() {
    StringBuilder builder = new StringBuilder();
    builder.append(title());
    for (int i : data){
      builder.append(String.valueOf(i));
      builder.append('\n');
    }
    return builder.toString();
  }

  @Override
  public String toString() {
    return "Int column: " + name();
  }

  @Override
  public void appendColumnData(Column column) {
    Preconditions.checkArgument(column.type() == this.type());
    IntColumn intColumn = (IntColumn) column;
    for (int i = 0; i < intColumn.size(); i++) {
      add(intColumn.get(i));
    }
  }

  @Override
  public IntIterator iterator() {
    return data.iterator();
  }
}
