package cz.xlisto.elektrodroid.pricelist;

import junit.framework.TestCase;

import org.junit.Test;

import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.models.PriceListRegulBuilder;
import cz.xlisto.elektrodroid.ownview.ViewHelper;

public class PriceListRegulBuilderTest extends TestCase {
    private PriceListRegulBuilder priceListRegulBuilder;

    @Test
    public void testMaxVT_NT_2022_2023() {
        priceListRegulBuilder = new PriceListRegulBuilder(new PriceListModel(), 2022);
        assertEquals(null, priceListRegulBuilder.getMaxVT());
        assertEquals(null, priceListRegulBuilder.getMaxNT());
        assertEquals("Zrušení poplatku POZE od října 2022: 0,00", priceListRegulBuilder.getMaxPOZE());

        priceListRegulBuilder = new PriceListRegulBuilder(new PriceListModel(), 2023);
        assertEquals("Cenový strop: 5000,00", priceListRegulBuilder.getMaxVT());
        assertEquals("Cenový strop: 5000,00", priceListRegulBuilder.getMaxNT());
        assertEquals("Zrušení poplatku POZE: 0,00", priceListRegulBuilder.getMaxPOZE());

    }

    //Test přesahu regulovaného a neregulovaného ceníku
    @Test
    public void testPriceRegulResult() {

        int yearStart = 2022;
        int yearEnd = 2022;
        int monthStart = 1;
        int monthEnd = 2;
        for (int i = 1; i < 24; i++) {
            String start = "4." + monthStart + "." + yearStart;
            String end = "4." + monthEnd + "." + yearEnd;
            monthStart++;
            monthEnd++;
            if (monthStart > 12) {
                monthStart = 1;
                yearStart++;
            }
            if (monthEnd > 12) {
                monthEnd = 1;
                yearEnd++;
            }


            long startRegulPrice = ViewHelper.parseCalendarFromString("1.10.2022").getTimeInMillis();
            long endRegulPrice = ViewHelper.parseCalendarFromString("31.12.2022").getTimeInMillis();
            long dateStartMonthlyReading = ViewHelper.parseCalendarFromString(start).getTimeInMillis();
            long dateEndMonthlyReading = ViewHelper.parseCalendarFromString(end).getTimeInMillis() - (24 * 60 * 60 * 1000);

            //Začátek regulace musí být větší  než začátek odečtu a zároveň začátek regulace menší nebo roven než konec odečtu
            boolean result;
            if (
                    ((startRegulPrice > dateStartMonthlyReading) && (startRegulPrice <= dateEndMonthlyReading))
                            || ((endRegulPrice >= dateStartMonthlyReading) && (endRegulPrice < dateEndMonthlyReading))
            ) {
                result = true;
            } else {
                result = false;
            }

            System.out.println(start+" "+end+": "+result);
            //assertEquals(result, false);
        }




    }

    //Test přesahu regulovaného a neregulovaného ceníku
    @Test
    public void testPriceRegulResult2() {
        String start = "1.10.2022";
        String end = "1.1.2023";
        long startRegulPrice = ViewHelper.parseCalendarFromString("1.10.2022").getTimeInMillis();
        long endRegulPrice = ViewHelper.parseCalendarFromString("31.12.2022").getTimeInMillis();
        long dateStartMonthlyReading = ViewHelper.parseCalendarFromString(start).getTimeInMillis();
        long dateEndMonthlyReading = ViewHelper.parseCalendarFromString(end).getTimeInMillis() - (24 * 60 * 60 * 1000);

        //Začátek regulace musí být větší  než začátek odečtu a zároveň začátek regulace menší nebo roven než konec odečtu
        boolean result;
        if (
                ((startRegulPrice > dateStartMonthlyReading) && (startRegulPrice <= dateEndMonthlyReading))
                        || ((endRegulPrice >= dateStartMonthlyReading) && (endRegulPrice < dateEndMonthlyReading))
        ) {
            result = true;
        } else {
            result = false;
        }

        System.out.println(start+" "+end+": "+result);
    }

}