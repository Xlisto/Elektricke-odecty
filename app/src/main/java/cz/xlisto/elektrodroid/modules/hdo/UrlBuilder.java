package cz.xlisto.elektrodroid.modules.hdo;

import android.widget.EditText;
import android.widget.Spinner;

import java.util.Calendar;

/**
 * Sestaví URL pro zobrazení stránky s HDO. Volbu distribuční sítě provádí podle nastavení spinneru
 * Xlisto 19.06.2023 13:12
 */
public class UrlBuilder {
    private static final String TAG = "UrlBuilder";
    final private Spinner spDistributionArea, spDistrict;
    final private EditText etHdoCode;

    public UrlBuilder(Spinner spDistributionArea, Spinner spDistrict, EditText etHdoCode) {
        this.spDistributionArea = spDistributionArea;
        this.spDistrict = spDistrict;
        this.etHdoCode = etHdoCode;
    }

    public String buildUrl() {
        String url = "";
        String distributionArea = spDistributionArea.getSelectedItem().toString();
        String district = spDistrict.getSelectedItem().toString();
        String hdoCode = etHdoCode.getText().toString().toUpperCase();

        switch (distributionArea) {
            case "ČEZ":
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
                url = "https://www.cezdistribuce.cz/webpublic/distHdo/adam/containers/" + region + "?code=" + hdoCode;

                break;
            case "EG.D":
                url = "https://www.egd.cz/casy-platnosti-nizkeho-tarifu";
                break;
            case "PRE":
                //Vzor:
                ////https://www.predistribuce.cz/cs/potrebuji-zaridit/zakaznici/stav-hdo/?povel=485&den_od=16&mesic_od=07&rok_od=2016&den_do=29&mesic_do=07&rok_do=2016&printable=1
                Calendar calendar = Calendar.getInstance();
                int denZacatek = calendar.get(Calendar.DAY_OF_MONTH);
                int mesicZacatek = calendar.get(Calendar.MONTH) + 1;
                int rokZacatek = calendar.get(Calendar.YEAR);
                int year = calendar.get(Calendar.YEAR);
                calendar.add(Calendar.DAY_OF_MONTH, 13);
                int denKonec = calendar.get(Calendar.DAY_OF_MONTH);
                int mesicKonec = calendar.get(Calendar.MONTH) + 1;
                int rokKonec = calendar.get(Calendar.YEAR);
                hdoCode = district.substring(0, 3);
                url = "https://www.predistribuce.cz/cs/potrebuji-zaridit/zakaznici/stav-hdo/?povel=" + hdoCode
                        + "&den_od=" + denZacatek + "&mesic_od=" + mesicZacatek + "&rok_od=" + rokZacatek
                        + "&den_do=" + denKonec + "&mesic_do=" + mesicKonec + "&rok_do=" + rokKonec + "&printable=1";

                break;
        }
        return url;
    }
}
