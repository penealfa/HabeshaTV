package com.gathin.tvhome;

import java.util.Calendar;

/**
 * Ethiopian (Ge'ez) calendar, the Ethiopian 12-hour clock and Ge'ez numerals, in plain arithmetic like
 * {@link Shamsi} and {@link Lunar}: no ICU, so it behaves the same on every TV firmware.
 */
final class Ethiopic {

    private static final String[] MONTHS = {
            "መስከረም", "ጥቅምት", "ኅዳር", "ታኅሣሥ", "ጥር", "የካቲት", "መጋቢት",
            "ሚያዝያ", "ግንቦት", "ሰኔ", "ሐምሌ", "ነሐሴ", "ጳጉሜን"};

    /** Indexed by {@link Calendar#DAY_OF_WEEK} minus one, so Sunday first. */
    private static final String[] WEEKDAYS = {"እሑድ", "ሰኞ", "ማክሰኞ", "ረቡዕ", "ሐሙስ", "ዓርብ", "ቅዳሜ"};

    /** Dawn 6:00 to 11:59, 12:00 to 17:59, 18:00 to 23:59, then the night hours. */
    private static final String[] PERIODS = {"ከጠዋቱ", "ከቀኑ", "ከምሽቱ", "ከሌሊቱ"};

    private static final String HOUR = "ሰዓት";
    private static final String PAST = "ከ";

    private static final String[] ONES = {"", "፩", "፪", "፫", "፬", "፭", "፮", "፯", "፰", "፱"};
    private static final String[] TENS = {"", "፲", "፳", "፴", "፵", "፶", "፷", "፸", "፹", "፺"};

    /**
     * Julian day number of 1 Meskerem of year 0, so that years 0, 1, 2 have 365 days and year 3 has 366:
     * Ethiopian leap years are the ones where year mod 4 is 3. (1 Meskerem of year 1 is JDN 1724221.)
     */
    private static final int EPOCH_JDN = 1723856;

    private Ethiopic() {
    }

    /** e.g. "ሐሙስ፣ ጥቅምት ፲፭ ፳፻፲፱" with Ge'ez numerals, or "ሐሙስ፣ ጥቅምት 15, 2019" without. */
    static String dateLine(Calendar c, boolean geezNumerals) {
        int[] e = fromGregorian(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
        String weekday = WEEKDAYS[c.get(Calendar.DAY_OF_WEEK) - 1];
        if (geezNumerals) return weekday + "፣ " + MONTHS[e[1] - 1] + " " + geez(e[2]) + " " + geez(e[0]);
        return weekday + "፣ " + MONTHS[e[1] - 1] + " " + e[2] + ", " + e[0];
    }

    /** e.g. "ከጠዋቱ ፪ ሰዓት ከ፲፭" for 08:15, or "ከጠዋቱ 2:15" without Ge'ez numerals. */
    static String clockLine(Calendar c, boolean geezNumerals) {
        int hour24 = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        int hour = clockHour(hour24);
        String period = PERIODS[periodIndex(hour24)];
        if (geezNumerals) return period + " " + geez(hour) + " " + HOUR + (minute > 0 ? " " + PAST + geez(minute) : "");
        return period + " " + hour + ":" + (minute < 10 ? "0" : "") + minute;
    }

    /**
     * The big clock in Ethiopian hours: 10:24 reads 4:24 and 9:00 reads 3:00, because the Ethiopian day starts at
     * 6:00. Ge'ez numerals give "፬:፳፬"; the hour alone when the minute is zero, like the Ge'ez clock words.
     */
    static String clockDigits(Calendar c, boolean geezNumerals) {
        int hour = clockHour(c.get(Calendar.HOUR_OF_DAY));
        int minute = c.get(Calendar.MINUTE);
        if (geezNumerals) return minute == 0 ? geez(hour) : geez(hour) + ":" + geez(minute);
        return hour + ":" + (minute < 10 ? "0" : "") + minute;
    }

    /** Gregorian to Ethiopian. Returns {year, month 1 to 13, day}. Month 13 is Pagume (5 or 6 days). */
    static int[] fromGregorian(int year, int month, int day) {
        int a = (14 - month) / 12;
        int y = year + 4800 - a;
        int m = month + 12 * a - 3;
        int jdn = day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045;
        int r = jdn - EPOCH_JDN;
        int n = Math.floorMod(r, 1461);
        int dayOfYear = (n % 365) + 365 * (n / 1460);
        int ethYear = 4 * Math.floorDiv(r, 1461) + n / 365 - n / 1460;
        return new int[]{ethYear, dayOfYear / 30 + 1, dayOfYear % 30 + 1};
    }

    /** The Ethiopian day starts at 6:00, so 7:00 is "1 o'clock" and 18:00 is "12 o'clock". Returns 1 to 12. */
    static int clockHour(int hour24) {
        int h = Math.floorMod(hour24 - 6, 12);
        return h == 0 ? 12 : h;
    }

    /** 0 morning (6 to 11), 1 day (12 to 17), 2 evening (18 to 23), 3 night (0 to 5). */
    static int periodIndex(int hour24) {
        if (hour24 >= 6 && hour24 < 12) return 0;
        if (hour24 >= 12 && hour24 < 18) return 1;
        if (hour24 >= 18) return 2;
        return 3;
    }

    /** Ge'ez numerals for 1 to 9999: tens and ones below 100, and ፻ for hundreds. Zero and below give "". */
    static String geez(int n) {
        if (n <= 0) return "";
        if (n < 100) return TENS[n / 10] + ONES[n % 10];
        int hundreds = n / 100;
        int rest = n % 100;
        return (hundreds == 1 ? "" : geez(hundreds)) + "፻" + TENS[rest / 10] + ONES[rest % 10];
    }
}
