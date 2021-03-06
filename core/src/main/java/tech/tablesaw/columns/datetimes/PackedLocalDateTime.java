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

package tech.tablesaw.columns.datetimes;

import com.google.common.base.Strings;
import com.google.common.primitives.Ints;
import tech.tablesaw.api.DateTimeColumn;
import tech.tablesaw.columns.dates.PackedLocalDate;
import tech.tablesaw.columns.times.PackedLocalTime;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.IsoChronology;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.Date;
import java.util.Locale;

import static tech.tablesaw.api.DateTimeColumn.*;

/*
 * TODO(lwhite): Extend missing-value handling on predicates to DateColumn and TimeColumn
 *
 * TODO(lwhite): Handle missing values on non-boolean (predicate) methods
 */

/**
 * A short localdatetime packed into a single long value. The long is comprised of an int for the date and an int
 * for the time
 * <p>
 * The bytes are packed into the date int as:
 * First two bytes: short (year)
 * next byte (month of year)
 * last byte (day of month)
 * <p>
 * The bytes are packed into the time int as
 * First byte: hourOfDay
 * next byte: minuteOfHour
 * last two bytes (short): millisecond of minute
 * <p>
 * Storing the millisecond of minute in an short requires that we treat the short as if it were unsigned. Unfortunately,
 * Neither Java nor Guava provide unsigned short support so we use char, which is a 16-bit unsigned int to
 * store values of up to 60,000 milliseconds (60 secs * 1000)
 */
public class PackedLocalDateTime {

    private PackedLocalDateTime() {}

    public static byte getDayOfMonth(long date) {
        return (byte) date(date);  // last byte
    }

    public static short getYear(long dateTime) {
        return PackedLocalDate.getYear(date(dateTime));
    }

    public static LocalDateTime asLocalDateTime(long dateTime) {
        if (dateTime == MISSING_VALUE) {
            return null;
        }
        int date = date(dateTime);
        int time = time(dateTime);

        return LocalDateTime.of(PackedLocalDate.asLocalDate(date), PackedLocalTime.asLocalTime(time));
    }

    public static byte getMonthValue(long dateTime) {
        int date = date(dateTime);
        return (byte) (date >> 8);
    }

    public static long pack(LocalDate date, LocalTime time) {
        if (date == null || time == null) {
            return DateTimeColumn.MISSING_VALUE;
        }
        int d = PackedLocalDate.pack(date);
        int t = PackedLocalTime.pack(time);
        return (((long) d) << 32) | (t & 0xffffffffL);
    }

    public static long pack(LocalDateTime dateTime) {
        if (dateTime == null) {
            return DateTimeColumn.MISSING_VALUE;
        }
        LocalDate date = dateTime.toLocalDate();
        LocalTime time = dateTime.toLocalTime();
        return (pack(date, time));
    }

    public static long pack(Date date) {
        if (date == null) {
            return DateTimeColumn.MISSING_VALUE;
        }
        return pack(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
    }

    public static long pack(short yr, byte m, byte d, byte hr, byte min, byte s, byte n) {
        int date = PackedLocalDate.pack(yr, m, d);

        int time = Ints.fromBytes(hr, min, s, n);

        return (((long) date) << 32) | (time & 0xffffffffL);
    }

    public static int date(long packedDateTIme) {
        return (int) (packedDateTIme >> 32);
    }

    public static int time(long packedDateTIme) {
        return (int) packedDateTIme;
    }

    public static String toString(long dateTime) {
        if (dateTime == Long.MIN_VALUE) {
            return "";
        }
        int date = date(dateTime);
        int time = time(dateTime);

        return
                "" + PackedLocalDate.getYear(date)
                        + "-"
                        + Strings.padStart(Byte.toString(PackedLocalDate.getMonthValue(date)), 2, '0')
                        + "-"
                        + Strings.padStart(Byte.toString(PackedLocalDate.getDayOfMonth(date)), 2, '0')
                        + "T"
                        + Strings.padStart(Byte.toString(PackedLocalTime.getHour(time)), 2, '0')
                        + ":"
                        + Strings.padStart(Byte.toString(PackedLocalTime.getMinute(time)), 2, '0')
                        + ":"
                        + Strings.padStart(Byte.toString(PackedLocalTime.getSecond(time)), 2, '0')
                        + "."
                        + Strings.padStart(String.valueOf(PackedLocalTime.getMilliseconds(time)), 3, '0');
    }

    public static int getDayOfYear(long packedDateTime) {
        return getMonth(packedDateTime).firstDayOfYear(isLeapYear(packedDateTime)) + getDayOfMonth(packedDateTime) - 1;
    }

    public static int getWeekOfYear(long packedDateTime) {
        LocalDateTime date = asLocalDateTime(packedDateTime);
        TemporalField woy = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear();
        return date.get(woy);
    }

    public static boolean isLeapYear(long packedDateTime) {
        if (packedDateTime == MISSING_VALUE) return false;
        return IsoChronology.INSTANCE.isLeapYear(getYear(packedDateTime));
    }

    public static Month getMonth(long packedDateTime) {
        return Month.of(getMonthValue(packedDateTime));
    }

    public static int lengthOfMonth(long packedDateTime) {
        switch (getMonthValue(packedDateTime)) {
            case 2:
                return (isLeapYear(packedDateTime) ? 29 : 28);
            case 4:
            case 6:
            case 9:
            case 11:
                return 30;
            default:
                return 31;
        }
    }

    public static DayOfWeek getDayOfWeek(long packedDateTime) {
        int date = PackedLocalDateTime.date(packedDateTime);
        return PackedLocalDate.getDayOfWeek(date);
    }

    /**
     * Returns the quarter of the year of the given date as an int from 1 to 4, or -1, if the argument is the
     * MISSING_VALUE for DateTimeColumn
     */
    public static int getQuarter(long packedDate) {
        if (packedDate == DateTimeColumn.MISSING_VALUE) {
            return -1;
        }
        return PackedLocalDate.getQuarter(date(packedDate));
    }

    public static boolean isInQ1(long packedDateTime) {
        if (packedDateTime == MISSING_VALUE) return false;
        Month month = getMonth(packedDateTime);
        return month == Month.JANUARY ||
                month == Month.FEBRUARY ||
                month == Month.MARCH;
    }

    public static boolean isInQ2(long packedDateTime) {
        if (packedDateTime == MISSING_VALUE) return false;
        Month month = getMonth(packedDateTime);
        return month == Month.APRIL ||
                month == Month.MAY ||
                month == Month.JUNE;
    }

    public static boolean isInQ3(long packedDateTime) {
        if (packedDateTime == MISSING_VALUE) return false;
        Month month = getMonth(packedDateTime);
        return month == Month.JULY ||
                month == Month.AUGUST ||
                month == Month.SEPTEMBER;
    }

    public static boolean isInQ4(long packedDateTime) {
        if (packedDateTime == MISSING_VALUE) return false;
        Month month = getMonth(packedDateTime);
        return month == Month.OCTOBER ||
                month == Month.NOVEMBER ||
                month == Month.DECEMBER;
    }

    public static boolean isAfter(long packedDateTime, long value) {
        return (packedDateTime != MISSING_VALUE) && packedDateTime > value;
    }

    public static boolean isBefore(long packedDateTime, long value) {
        return (packedDateTime != MISSING_VALUE) && packedDateTime < value;
    }

    public static boolean isSunday(long packedDateTime) {
        if (packedDateTime == MISSING_VALUE) return false;
        DayOfWeek dayOfWeek = getDayOfWeek(packedDateTime);
        return dayOfWeek == DayOfWeek.SUNDAY;
    }

    public static boolean isMonday(long packedDateTime) {
        if (packedDateTime == MISSING_VALUE) return false;
        DayOfWeek dayOfWeek = getDayOfWeek(packedDateTime);
        return dayOfWeek == DayOfWeek.MONDAY;
    }

    public static boolean isTuesday(long packedDateTime) {
        if (packedDateTime == MISSING_VALUE) return false;
        DayOfWeek dayOfWeek = getDayOfWeek(packedDateTime);
        return dayOfWeek == DayOfWeek.TUESDAY;
    }

    public static boolean isWednesday(long packedDateTime) {
        if (packedDateTime == MISSING_VALUE) return false;
        DayOfWeek dayOfWeek = getDayOfWeek(packedDateTime);
        return dayOfWeek == DayOfWeek.WEDNESDAY;
    }

    public static boolean isThursday(long packedDateTime) {
        if (packedDateTime == MISSING_VALUE) return false;
        DayOfWeek dayOfWeek = getDayOfWeek(packedDateTime);
        return dayOfWeek == DayOfWeek.THURSDAY;
    }

    public static boolean isFriday(long packedDateTime) {
        if (packedDateTime == MISSING_VALUE) return false;
        DayOfWeek dayOfWeek = getDayOfWeek(packedDateTime);
        return dayOfWeek == DayOfWeek.FRIDAY;
    }

    public static boolean isSaturday(long packedDateTime) {
        if (packedDateTime == MISSING_VALUE) return false;
        DayOfWeek dayOfWeek = getDayOfWeek(packedDateTime);
        return dayOfWeek == DayOfWeek.SATURDAY;
    }

    public static boolean isFirstDayOfMonth(long packedDateTime) {
        return (packedDateTime != MISSING_VALUE) && getDayOfMonth(packedDateTime) == 1;
    }

    public static boolean isInJanuary(long packedDateTime) {
        return (packedDateTime != MISSING_VALUE) && getMonth(packedDateTime) == Month.JANUARY;
    }

    public static boolean isInFebruary(long packedDateTime) {
        return (packedDateTime != MISSING_VALUE) && getMonth(packedDateTime) == Month.FEBRUARY;
    }

    public static boolean isInMarch(long packedDateTime) {
        return (packedDateTime != MISSING_VALUE) && getMonth(packedDateTime) == Month.MARCH;
    }

    public static boolean isInApril(long packedDateTime) {
        return (packedDateTime != MISSING_VALUE) && getMonth(packedDateTime) == Month.APRIL;
    }

    public static boolean isInMay(long packedDateTime) {
        return (packedDateTime != MISSING_VALUE) && getMonth(packedDateTime) == Month.MAY;
    }

    public static boolean isInJune(long packedDateTime) {
        return (packedDateTime != MISSING_VALUE) && getMonth(packedDateTime) == Month.JUNE;
    }

    public static boolean isInJuly(long packedDateTime) {
        return (packedDateTime != MISSING_VALUE) && getMonth(packedDateTime) == Month.JULY;
    }

    public static boolean isInAugust(long packedDateTime) {
        return (packedDateTime != MISSING_VALUE) && getMonth(packedDateTime) == Month.AUGUST;
    }

    public static boolean isInSeptember(long packedDateTime) {
        return (packedDateTime != MISSING_VALUE) && getMonth(packedDateTime) == Month.SEPTEMBER;
    }

    public static boolean isInOctober(long packedDateTime) {
        return (packedDateTime != MISSING_VALUE) && getMonth(packedDateTime) == Month.OCTOBER;
    }

    public static boolean isInNovember(long packedDateTime) {
        return (packedDateTime != MISSING_VALUE) && getMonth(packedDateTime) == Month.NOVEMBER;
    }

    public static boolean isInDecember(long packedDateTime) {
        return (packedDateTime != MISSING_VALUE) && getMonth(packedDateTime) == Month.DECEMBER;
    }

    public static boolean isLastDayOfMonth(long packedDateTime) {
        return (packedDateTime != MISSING_VALUE) && getDayOfMonth(packedDateTime) == lengthOfMonth(packedDateTime);
    }

    public static boolean isInYear(long packedDateTime, int year) {
        return (packedDateTime != MISSING_VALUE) && getYear(packedDateTime) == year;
    }

    public static boolean isMidnight(long packedDateTime) {
        return (packedDateTime != MISSING_VALUE) && PackedLocalTime.isMidnight(time(packedDateTime));
    }

    public static boolean isNoon(long packedDateTime) {
        return (packedDateTime != MISSING_VALUE) && PackedLocalTime.isNoon(time(packedDateTime));
    }

    /**
     * Returns true if the time is in the AM or "before noon".
     * Note: we follow the convention that 12:00 NOON is PM and 12 MIDNIGHT is AM
     */
    public static boolean AM(long packedDateTime) {
        return (packedDateTime != MISSING_VALUE) && PackedLocalTime.AM(time(packedDateTime));
    }

    /**
     * Returns true if the time is in the PM or "after noon".
     * Note: we follow the convention that 12:00 NOON is PM and 12 MIDNIGHT is AM
     */
    public static boolean PM(long packedDateTime) {
        return (packedDateTime != MISSING_VALUE) && PackedLocalTime.PM(time(packedDateTime));
    }

    public static int getMinuteOfDay(long packedLocalDateTime) {
        return getHour(packedLocalDateTime) * 60 + getMinute(packedLocalDateTime);
    }

    public static byte getSecond(int packedLocalDateTime) {
        return (byte) (getMillisecondOfMinute(packedLocalDateTime) / 1000);
    }

    public static byte getHour(long packedLocalDateTime) {
        return PackedLocalTime.getHour(time(packedLocalDateTime));
    }

    public static byte getMinute(long packedLocalDateTime) {
        return PackedLocalTime.getMinute(time(packedLocalDateTime));
    }

    public static byte getSecond(long packedLocalDateTime) {
        return PackedLocalTime.getSecond(time(packedLocalDateTime));
    }

    public static double getSecondOfDay(long packedLocalDateTime) {
        return PackedLocalTime.getSecondOfDay(time(packedLocalDateTime));
    }

    public static short getMillisecondOfMinute(long packedLocalDateTime) {
        return (short) PackedLocalTime.getMillisecondOfMinute(time(packedLocalDateTime));
    }

    public static long getMillisecondOfDay(long packedLocalDateTime) {
        LocalDateTime localDateTime = PackedLocalDateTime.asLocalDateTime(packedLocalDateTime);
        long total = (long) localDateTime.get(ChronoField.MILLI_OF_SECOND);
        total += localDateTime.getSecond() * 1000;
        total += localDateTime.getMinute() * 60 * 1000;
        total += localDateTime.getHour() * 60 * 60 * 1000;
        return total;
    }

    public static long create(int date, int time) {
        return (((long) date) << 32) | (time & 0xffffffffL);
    }

    public static long toEpochMilli(long packedLocalDateTime, ZoneOffset offset) {
        LocalDateTime dateTime = asLocalDateTime(packedLocalDateTime);
        Instant instant = dateTime.toInstant(offset);
        return instant.toEpochMilli();
    }

    public static long ofEpochMilli(long millisecondsSinceEpoch, ZoneId zoneId) {
        Instant instant = Instant.ofEpochMilli(millisecondsSinceEpoch);
        return pack(LocalDateTime.ofInstant(instant, zoneId));
    }

    public int lengthOfYear(long packedDateTime) {
        return (isLeapYear(packedDateTime) ? 366 : 365);
    }

    // TODO: Need to add hoursUntil(), minutesUntil()

    public static int daysUntil(long packedDateTimeEnd, long packedDateTimeStart) {
        return (int) (PackedLocalDate.toEpochDay(date(packedDateTimeEnd))
                - PackedLocalDate.toEpochDay(date(packedDateTimeStart)));
    }

    public static int weeksUntil(long packedDateTimeEnd, long packedDateStart) {
        return daysUntil(packedDateTimeEnd, packedDateStart)/7;
    }

    public static int monthsUntil(long packedDateTimeEnd, long packedDateStart) {

        int start = getMonthInternal(packedDateStart) * 32 + getDayOfMonth(packedDateStart);
        int end = getMonthInternal(packedDateTimeEnd) * 32 + getDayOfMonth(packedDateTimeEnd);
        return (end - start) / 32;
    }

    public static int yearsUntil(long packedDateEnd, long packedDateStart) {
        return monthsUntil(packedDateEnd, packedDateStart)/12;
    }

    private static int getMonthInternal(long packedDateTime) {
        return (getYear(packedDateTime) * 12 + getMonthValue(packedDateTime) - 1);
    }

    public static boolean isEqualTo(long packedDateTime, long value) {
        return DateTimePredicates.isEqualTo.test(packedDateTime, value);
    }

    public static boolean isOnOrAfter(long valueToTest, long valueToTestAgainst) {
        return isAfter(valueToTest, valueToTestAgainst)
                || isEqualTo(valueToTest, valueToTestAgainst);
    }

    public static boolean isOnOrBefore(long valueToTest, long valueToTestAgainst) {
        return isBefore(valueToTest, valueToTestAgainst)
                || isEqualTo(valueToTest, valueToTestAgainst);
    }
}
