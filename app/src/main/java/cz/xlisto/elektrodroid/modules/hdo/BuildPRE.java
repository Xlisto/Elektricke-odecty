package cz.xlisto.elektrodroid.modules.hdo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import cz.xlisto.elektrodroid.models.HdoModel;


/**
 * Sestaví a agreguje z JSON dat PRE seznam HdoModelů podle všedních dní, víkendu a svátků.
 * Xlisto 11.01.2024 14:35
 */
public class BuildPRE {
    private static final String TAG = "BuildPRE";
    private static final String KOD_POVELU = "kodPovelu";
    private static final String PLATNOST = "platnost";
    private static final String CAS_ZAP = "casZap";
    private static final String CAS_VYP = "casVyp";
    private static final String DEN_TEXT = "denText";
    private static final String IS_HOLIDAY = "isHoliday";
    private static final String EMPTY = "";


    private static class TimeSlotKey {

        final String rele;
        final String timeOn;
        final String timeOff;


        TimeSlotKey(String rele, String timeOn, String timeOff) {
            this.rele = rele;
            this.timeOn = timeOn;
            this.timeOff = timeOff;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TimeSlotKey that = (TimeSlotKey) o;
            return Objects.equals(rele, that.rele) && Objects.equals(timeOn, that.timeOn) && Objects.equals(timeOff, that.timeOff);
        }


        @Override
        public int hashCode() {
            return Objects.hash(rele, timeOn, timeOff);
        }

    }


    private static class SlotDays {

        boolean mon, tue, wed, thu, fri, sat, sun;

    }


    /**
     * Sestaví a agreguje seznam HDO pro PRE odděleně na všední dny (Po - Pá), víkend (So - Ne) a svátky.
     *
     * @param jsonData objekt json s daty HDO
     * @return seznam HDO kontejnerů
     */
    public static ArrayList<HdoSiteFragment.HdoListContainer> build(String jsonData) {
        ArrayList<HdoSiteFragment.HdoListContainer> hdoListContainers = new ArrayList<>();
        ArrayList<HdoModel> hdoList = new ArrayList<>();
        try {
            JSONArray jsonRoot = new JSONArray(jsonData);
            if (jsonRoot.length() == 0) {
                return hdoListContainers;
            }

            String firstDate = null;
            String lastDate = null;

            // Oddělené zásoby pro všední dny (Po-Pá), víkend (So-Ne) a svátky
            Map<TimeSlotKey, SlotDays> weekdayMap = new LinkedHashMap<>();
            Map<TimeSlotKey, SlotDays> weekendMap = new LinkedHashMap<>();
            Map<TimeSlotKey, Boolean> holidayMap = new LinkedHashMap<>();

            for (int i = 0; i < jsonRoot.length(); i++) {
                JSONObject jsonObject = jsonRoot.getJSONObject(i);
                String rele = jsonObject.getString(KOD_POVELU);
                String date = jsonObject.getString(PLATNOST);

                if (firstDate == null) {
                    firstDate = date;
                }
                lastDate = date;

                int dayOfWeek = parseDayOfWeek(date);
                boolean isHoliday = jsonObject.optBoolean(IS_HOLIDAY, false) ||
                        (jsonObject.has(DEN_TEXT) && jsonObject.getString(DEN_TEXT).toLowerCase().contains("svátek"));

                for (int j = 0; j < 10; j++) {
                    String timeOn = jsonObject.optString(CAS_ZAP + j, "");
                    String timeOff = jsonObject.optString(CAS_VYP + j, "");
                    if (timeOn.isEmpty() || timeOff.isEmpty() || timeOn.equals(EMPTY) || timeOff.equals(EMPTY)) {
                        break;
                    }

                    TimeSlotKey key = new TimeSlotKey(rele, timeOn, timeOff);

                    if (isHoliday) {
                        holidayMap.put(key, true);
                    } else if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                        SlotDays days = weekendMap.get(key);
                        if (days == null) {
                            days = new SlotDays();
                            weekendMap.put(key, days);
                        }
                        if (dayOfWeek == Calendar.SATURDAY) days.sat = true;
                        if (dayOfWeek == Calendar.SUNDAY) days.sun = true;
                    } else {
                        // Všední dny (Pondělí až Pátek)
                        SlotDays days = weekdayMap.get(key);
                        if (days == null) {
                            days = new SlotDays();
                            weekdayMap.put(key, days);
                        }
                        switch (dayOfWeek) {
                            case Calendar.MONDAY -> days.mon = true;
                            case Calendar.TUESDAY -> days.tue = true;
                            case Calendar.WEDNESDAY -> days.wed = true;
                            case Calendar.THURSDAY -> days.thu = true;
                            case Calendar.FRIDAY -> days.fri = true;
                        }
                    }
                }
            }

            String dateFrom = firstDate != null ? firstDate : "";
            String dateUntil = lastDate != null ? lastDate : "";
            String validityDate = dateFrom.equals(dateUntil) ? dateFrom : dateFrom + " - " + dateUntil;

            // 1. Přidání agregovaných modelů pro všední dny (Po - Pá)
            for (Map.Entry<TimeSlotKey, SlotDays> entry : weekdayMap.entrySet()) {
                TimeSlotKey key = entry.getKey();
                SlotDays days = entry.getValue();

                HdoModel hdo = new HdoModel(
                        key.rele,
                        dateFrom,
                        dateUntil,
                        key.timeOn,
                        key.timeOff,
                        days.mon ? 1 : 0,
                        days.tue ? 1 : 0,
                        days.wed ? 1 : 0,
                        days.thu ? 1 : 0,
                        days.fri ? 1 : 0,
                        0,
                        0,
                        DistributionArea.PRE.toString()
                );
                hdoList.add(hdo);
            }

            // 2. Přidání agregovaných modelů pro víkend (So - Ne)
            for (Map.Entry<TimeSlotKey, SlotDays> entry : weekendMap.entrySet()) {
                TimeSlotKey key = entry.getKey();
                SlotDays days = entry.getValue();

                HdoModel hdo = new HdoModel(
                        key.rele,
                        dateFrom,
                        dateUntil,
                        key.timeOn,
                        key.timeOff,
                        0,
                        0,
                        0,
                        0,
                        0,
                        days.sat ? 1 : 0,
                        days.sun ? 1 : 0,
                        DistributionArea.PRE.toString()
                );
                hdoList.add(hdo);
            }

            // 3. Přidání modelů pro svátek (SVÁTEK)
            for (Map.Entry<TimeSlotKey, Boolean> entry : holidayMap.entrySet()) {
                TimeSlotKey key = entry.getKey();

                HdoModel hdo = new HdoModel(
                        key.rele,
                        dateFrom,
                        dateUntil,
                        key.timeOn,
                        key.timeOff,
                        0, 0, 0, 0, 0, 0, 0,
                        DistributionArea.PRE.toString()
                );
                hdo.setSv(1);
                hdoList.add(hdo);
            }

            HdoSiteFragment.HdoListContainer hdoListContainer = new HdoSiteFragment.HdoListContainer(hdoList, validityDate, HdoSiteFragment.HdoListContainer.Distribution.PRE);
            hdoListContainers.add(hdoListContainer);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return hdoListContainers;
    }


    private static int parseDayOfWeek(String dateStr) {
        try {
            String[] parts = dateStr.split("\\.");
            int day = Integer.parseInt(parts[0].trim());
            int month = Integer.parseInt(parts[1].trim());
            int year = Integer.parseInt(parts[2].trim());
            Calendar c = Calendar.getInstance();
            c.set(year, month - 1, day);
            return c.get(Calendar.DAY_OF_WEEK);
        } catch (Exception e) {
            return -1;
        }
    }
}
