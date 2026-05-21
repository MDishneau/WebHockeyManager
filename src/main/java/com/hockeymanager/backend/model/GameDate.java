package com.hockeymanager.backend.model;

public class GameDate implements Comparable<GameDate> {

    private final int year;
    private final int month;
    private final int day;

    private static final int[] DAYS_IN_MONTH = {
            0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
    };

    public GameDate(int year, int month, int day) {
        this.year  = year;
        this.month = month;
        this.day   = day;
    }

    public GameDate nextDay() {
        int d = day + 1;
        int m = month;
        int y = year;
        if (d > DAYS_IN_MONTH[m]) {
            d = 1;
            m++;
            if (m > 12) { m = 1; y++; }
        }
        return new GameDate(y, m, d);
    }

    public GameDate plusDays(int n) {
        GameDate result = this;
        for (int i = 0; i < n; i++) result = result.nextDay();
        return result;
    }

    public boolean isBefore(GameDate other) {
        return this.compareTo(other) < 0;
    }

    public boolean isAfter(GameDate other) {
        return this.compareTo(other) > 0;
    }

    @Override
    public int compareTo(GameDate other) {
        if (this.year  != other.year)  return Integer.compare(this.year,  other.year);
        if (this.month != other.month) return Integer.compare(this.month, other.month);
        return Integer.compare(this.day, other.day);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof GameDate)) return false;
        GameDate other = (GameDate) o;
        return year == other.year && month == other.month && day == other.day;
    }

    @Override
    public int hashCode() {
        return year * 10000 + month * 100 + day;
    }

    @Override
    public String toString() {
        return String.format("%04d-%02d-%02d", year, month, day);
    }

    public int getYear()  { return year; }
    public int getMonth() { return month; }
    public int getDay()   { return day; }
}