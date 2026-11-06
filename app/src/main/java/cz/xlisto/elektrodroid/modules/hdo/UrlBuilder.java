package cz.xlisto.elektrodroid.modules.hdo;


import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;

import java.util.Calendar;


/**
 * Pomocná třída pro sestavení URL adresy pro zobrazení stavu HDO (Hromadného Dálkového Ovládání).
 *
 * <p>Sestavuje správnou webovou adresu na základě vybrané distribuční oblasti (ČEZ, EG.D, PRE),
 * regionu a zadaného kódu HDO z prvků uživatelského rozhraní.</p>
 */
public class UrlBuilder {

    private final Spinner spDistributionArea;
    private final Spinner spDistrict;
    private final EditText etHdoCode;


    /**
     * Vytvoří novou instanci {@link UrlBuilder}.
     *
     * @param spDistributionArea spinner s výběrem distribuční oblasti (ČEZ, EG.D, PRE)
     * @param spDistrict         spinner s výběrem okresu nebo regionu
     * @param etHdoCode          vstupní pole pro zadání kódu nebo povelu HDO
     */
    public UrlBuilder(Spinner spDistributionArea, Spinner spDistrict, EditText etHdoCode) {
        this.spDistributionArea = spDistributionArea;
        this.spDistrict = spDistrict;
        this.etHdoCode = etHdoCode;
    }


    /**
     * Sestaví finální URL adresu pro zvolenou distribuční oblast a zadané parametry.
     *
     * @return URL adresa ve formátu {@link String} pro načtení stránek HDO
     */
    public String buildUrl() {
        String url = "";
        String distributionArea = spDistributionArea.getSelectedItem().toString();
        String district = spDistrict.getSelectedItem().toString();
        String hdoCode = etHdoCode.getText().toString().toUpperCase();

        switch (distributionArea) {
            case "ČEZ":
                String region = getRegion(district);
                url = "https://www.cezdistribuce.cz/webpublic/distHdo/adam/containers/" + region + "?code=" + hdoCode;
                break;
            case "EG.D":
                url = "https://www.egd.cz/casy-platnosti-nizkeho-tarifu";
                break;
            case "PRE":
                // Vzor příkladu URL pro PRE:
                // https://www.predistribuce.cz/cs/potrebuji-zaridit/zakaznici/stav-hdo/?povel=485&den_od=16&mesic_od=07&rok_od=2016&den_do=29&mesic_do=07&rok_do=2016&printable=1
                Calendar calendar = Calendar.getInstance();
                int dayStart = calendar.get(Calendar.DAY_OF_MONTH);
                int monthStart = calendar.get(Calendar.MONTH) + 1;
                int yearStart = calendar.get(Calendar.YEAR);
                calendar.add(Calendar.DAY_OF_MONTH, 13);
                int dayEnd = calendar.get(Calendar.DAY_OF_MONTH);
                int monthEnd = calendar.get(Calendar.MONTH) + 1;
                int yearEnd = calendar.get(Calendar.YEAR);
                hdoCode = district.substring(0, 3);
                url = "https://www.predistribuce.cz/cs/potrebuji-zaridit/zakaznici/stav-hdo/?povel=" + hdoCode
                        + "&den_od=" + dayStart + "&mesic_od=" + monthStart + "&rok_od=" + yearStart
                        + "&den_do=" + dayEnd + "&mesic_do=" + monthEnd + "&rok_do=" + yearEnd + "&printable=1";
                break;
        }
        return url;
    }


    /**
     * Převede vybraný název okresu na kód regionu používaný webovou službou ČEZ Distribuce.
     *
     * @param district název okresu z uživatelského rozhraní (např. "Střed", "Sever")
     * @return identifikátor regionu v URL adrese
     */
    @NonNull
    private static String getRegion(String district) {
        String region = "";
        if (district.equals("Střed")) {
            region = "stred";
        }
        if (district.equals("Sever")) {
            region = "sever";
        }
        if (district.equals("Západ")) {
            region = "zapad";
        }
        if (district.equals("Východ")) {
            region = "vychod";
        }
        if (district.equals("Morava")) {
            region = "morava";
        }
        return region;
    }
}
