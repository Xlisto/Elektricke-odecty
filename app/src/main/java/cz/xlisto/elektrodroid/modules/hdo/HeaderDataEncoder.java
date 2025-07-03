package cz.xlisto.elektrodroid.modules.hdo;


import android.util.Base64;

import java.nio.charset.StandardCharsets;


/**
 * Pomocná třída pro kódování payloadu HDO.
 * Obsahuje metody pro převod řetězců na hexadecimální formát a speciální zakódování dotazů.
 */
public class HeaderDataEncoder {

    /**
     * Převod ASCII řetězce na hexadecimální řetězec (každý znak → 2 hex cifry).
     *
     * @param s Vstupní ASCII řetězec
     * @return Hexadecimální reprezentace vstupního řetězce
     */
    public static String bin2hex(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.US_ASCII);
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            int v = b & 0xFF;
            if (v < 0x10) sb.append('0');
            sb.append(Integer.toHexString(v));
        }
        return sb.toString();
    }


    /**
     * Java obdoba JS funkce getEncodedQueryVariables:
     * 1) Převede vstupní řetězec na Base64 z UTF-8 bajtů
     * 2) Výsledek převede na hexadecimální řetězec
     * 3) Výsledný hexadecimální řetězec otočí (reverse)
     *
     * @param query Vstupní řetězec
     * @return Zakódovaný a otočený hexadecimální řetězec
     */
    public static String getEncodedQueryVariables(String query) {
        // 1) Base64 (bez zalamování)
        byte[] utf8 = query.getBytes(StandardCharsets.UTF_8);
        String b64 = Base64.encodeToString(utf8, Base64.NO_WRAP);

        // 2) na hex
        String hex = bin2hex(b64);

        // 3) obrácení
        return new StringBuilder(hex).reverse().toString();
    }

}
