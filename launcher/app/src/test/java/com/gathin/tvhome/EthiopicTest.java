package com.gathin.tvhome;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.junit.Test;

public class EthiopicTest {

    private static void assertEth(int gy, int gm, int gd, int ey, int em, int ed) {
        assertArrayEquals(gy + "-" + gm + "-" + gd, new int[]{ey, em, ed}, Ethiopic.fromGregorian(gy, gm, gd));
    }

    @Test
    public void newYearFallsOnSeptember11UnlessThePreviousEthiopianYearWasALeapYear() {
        assertEth(2022, 9, 11, 2015, 1, 1);
        assertEth(2023, 9, 11, 2015, 13, 6); // 2015 is a leap year: Pagume has 6 days
        assertEth(2023, 9, 12, 2016, 1, 1);
        assertEth(2024, 9, 11, 2017, 1, 1);
        assertEth(2026, 9, 11, 2019, 1, 1);
        assertEth(2027, 9, 11, 2019, 13, 6);
        assertEth(2027, 9, 12, 2020, 1, 1);
    }

    @Test
    public void knownHolidays() {
        assertEth(2025, 1, 7, 2017, 4, 29); // Genna (Christmas): 29 Tahsas
        assertEth(2025, 1, 19, 2017, 5, 11); // Timkat (Epiphany): 11 Tir
        assertEth(2025, 9, 27, 2018, 1, 17); // Meskel: 17 Meskerem
        assertEth(2026, 3, 2, 2018, 6, 23); // Adwa Victory Day: 23 Yekatit
        assertEth(2026, 10, 25, 2019, 2, 15); // 15 Tikimt 2019
    }

    @Test
    public void everyDayOfAYearAdvancesByOneDay() {
        Calendar c = new GregorianCalendar(2024, Calendar.JANUARY, 1);
        int[] prev = Ethiopic.fromGregorian(2023, 12, 31);
        for (int i = 0; i < 366 * 3; i++) {
            int[] cur = Ethiopic.fromGregorian(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
            boolean sameMonth = cur[0] == prev[0] && cur[1] == prev[1] && cur[2] == prev[2] + 1;
            boolean nextMonth = cur[0] == prev[0] && cur[1] == prev[1] + 1 && cur[2] == 1 && prev[2] == (prev[1] == 13 ? prev[2] : 30);
            boolean nextYear = cur[0] == prev[0] + 1 && cur[1] == 1 && cur[2] == 1 && prev[1] == 13;
            assertEquals("day " + i + " " + c.getTime(), true, sameMonth || nextMonth || nextYear);
            prev = cur;
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    @Test
    public void clockCountsFromSixInTheMorning() {
        assertEquals(12, Ethiopic.clockHour(6));
        assertEquals(1, Ethiopic.clockHour(7));
        assertEquals(6, Ethiopic.clockHour(12));
        assertEquals(12, Ethiopic.clockHour(18));
        assertEquals(5, Ethiopic.clockHour(23));
        assertEquals(6, Ethiopic.clockHour(0));
        assertEquals(11, Ethiopic.clockHour(5));
    }

    @Test
    public void periodsOfTheDay() {
        assertEquals(3, Ethiopic.periodIndex(0));
        assertEquals(3, Ethiopic.periodIndex(5));
        assertEquals(0, Ethiopic.periodIndex(6));
        assertEquals(0, Ethiopic.periodIndex(11));
        assertEquals(1, Ethiopic.periodIndex(12));
        assertEquals(1, Ethiopic.periodIndex(17));
        assertEquals(2, Ethiopic.periodIndex(18));
        assertEquals(2, Ethiopic.periodIndex(23));
    }

    @Test
    public void geezNumerals() {
        assertEquals("", Ethiopic.geez(0));
        assertEquals("፩", Ethiopic.geez(1));
        assertEquals("፱", Ethiopic.geez(9));
        assertEquals("፲", Ethiopic.geez(10));
        assertEquals("፲፭", Ethiopic.geez(15));
        assertEquals("፳፭", Ethiopic.geez(25));
        assertEquals("፺፱", Ethiopic.geez(99));
        assertEquals("፻", Ethiopic.geez(100));
        assertEquals("፻፩", Ethiopic.geez(101));
        assertEquals("፳፻", Ethiopic.geez(2000));
        assertEquals("፳፻፲፱", Ethiopic.geez(2019));
    }

    @Test
    public void dateAndClockLines() {
        Calendar c = new GregorianCalendar(2026, Calendar.OCTOBER, 25, 8, 15); // a Sunday
        assertEquals("እሑድ፣ ጥቅምት ፲፭ ፳፻፲፱", Ethiopic.dateLine(c, true));
        assertEquals("እሑድ፣ ጥቅምት 15, 2019", Ethiopic.dateLine(c, false));
        assertEquals("ከጠዋቱ ፪ ሰዓት ከ፲፭", Ethiopic.clockLine(c, true));
        assertEquals("ከጠዋቱ 2:15", Ethiopic.clockLine(c, false));
        c.set(Calendar.HOUR_OF_DAY, 19);
        c.set(Calendar.MINUTE, 0);
        assertEquals("ከምሽቱ ፩ ሰዓት", Ethiopic.clockLine(c, true));
    }
}
