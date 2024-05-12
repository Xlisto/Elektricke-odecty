package cz.xlisto.elektrodroid.modules.hdo;

/**
 * Xlisto 25.06.2023 20:34
 */
public abstract class CodeWeb {
    private static final String TAG = "CodeWeb";
    public static String htmlPage = "<!DOCTYPE html>\n" +
            "<html lang=\"cs\">\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "    <title>Document</title>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div id=\"code\"></div>\n" +
            "    <div id=\"react-hdo-form-wrapper\" data-api-url=\"\"></div>\n" +
            "    \n" +
            "  <script type=\"application/javascript\" wfd-invisible=\"true\">\n" +
            "    window.dataLayer = window.dataLayer || [];\n" +
            "    dataLayer.push({ 'event': 'page' });\n" +
            "  </script>\n" +
            "  <script type=\"application/json\" data-drupal-selector=\"drupal-settings-json\"\n" +
            "    wfd-invisible=\"true\">{\n" +
            "\t\"path\": {\n" +
            "\t\t\"baseUrl\": \"\\/\",\n" +
            "\t\t\"scriptPath\": null,\n" +
            "\t\t\"pathPrefix\": \"\",\n" +
            "\t\t\"currentPath\": \"node\\/5\",\n" +
            "\t\t\"currentPathIsAdmin\": false,\n" +
            "\t\t\"isFront\": false,\n" +
            "\t\t\"currentLanguage\": \"cs\",\n" +
            "\t\t\"themeUrl\": \"themes\\/reason_eon\"\n" +
            "\t},\n" +
            "\t\"pluralDelimiter\": \"\\u0003\",\n" +
            "\t\"suppressDeprecationErrors\": true,\n" +
            "\t\"ajaxPageState\": {\n" +
            "\t\t\"libraries\": \"antibot\\/antibot.form,core\\/drupal.autocomplete,eon\\/calamity,eon\\/react_hdo,eon_gtm\\/scripts,mluvii_chat\\/mluvii_chat_active,paragraphs\\/drupal.paragraphs.unpublished,reason_eon\\/global-styling,reason_eon\\/scripts,search_api_autocomplete\\/search_api_autocomplete,system\\/base,typogrify\\/typogrify,usercentrics_gdprbar\\/usercentrics_gdprbar.bar,usercentrics_gdprbar\\/usercentrics_init,views\\/views.module\",\n" +
            "\t\t\"theme\": \"reason_eon\",\n" +
            "\t\t\"theme_token\": null\n" +
            "\t},\n" +
            "\t\"ajaxTrustedUrl\": {\n" +
            "\t\t\"\\/hledat\": true\n" +
            "\t},\n" +
            "\t\"eon\": {\n" +
            "\t\t\"HDO\": {\n" +
            "\t\t\t\"api_url\": \"kbWiCdDEsscQjA846IB+i+VjF8PmbqlYsxA9ti6A1+Q=\",\n" +
            "\t\t\t\"api_token\": \"***12345***\",\n" +
            "\t\t\t\"api_authbasic\": \"BP1625lj0r0R9x0UnelZag==\",\n" +
            "\t\t\t\"drupalContent\": {\n" +
            "\t\t\t\t\"intro\": \"\\u003Cp\\u003EZ\\u00e1kazn\\u00edci vyu\\u017e\\u00edvaj\\u00edc\\u00ed elekt\\u0159inu pro vyt\\u00e1p\\u011bn\\u00ed nebo oh\\u0159ev vody maj\\u00ed zpravidla sv\\u00e9 odb\\u011brn\\u00e9 m\\u00edsto vybaveno \\u003Cstrong\\u003Ep\\u0159ij\\u00edma\\u010dem HDO\\u003C\\/strong\\u003E (Hromadn\\u00e9 D\\u00e1lkov\\u00e9 Ovl\\u00e1d\\u00e1n\\u00ed), d\\u00edky kter\\u00e9mu mohou vyu\\u017e\\u00edvat i tzv. n\\u00edzk\\u00fd tarif. Pro zobrazen\\u00ed platnosti n\\u00edzk\\u00e9ho tarifu va\\u0161eho odb\\u011brn\\u00e9ho m\\u00edsta je t\\u0159eba \\u003Cstrong\\u003E\\u003Ca href=\\u0022\\/jak-vyhledat-kod-hdo\\u0022 target=\\u0022_blank\\u0022\\u003Ezn\\u00e1t k\\u00f3d HDO\\u003C\\/a\\u003E\\u003C\\/strong\\u003E, kter\\u00fd je \\u003Cstrong\\u003Euveden na p\\u0159ij\\u00edma\\u010di HDO\\u003C\\/strong\\u003E. P\\u0159ij\\u00edma\\u010d najdete v elektrom\\u011brov\\u00e9m\\u0026nbsp;rozvad\\u011b\\u010di.\\u003C\\/p\\u003E\\n\\u003Cp\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003EDal\\u0161\\u00ed mo\\u017enost, jak se m\\u016f\\u017ee m\\u011bnit doba sp\\u00edn\\u00e1n\\u00ed mezi n\\u00edzk\\u00fdm a vysok\\u00fdm tarifem je pomoc\\u00ed \\u003Cstrong\\u003Edigit\\u00e1ln\\u00edch p\\u0159ep\\u00ednac\\u00edch hodin\\u003C\\/strong\\u003E. Tento p\\u0159\\u00edstroj b\\u00fdv\\u00e1 tak\\u00e9 nainstalov\\u00e1n ve va\\u0161em rozvad\\u011b\\u010di spole\\u010dn\\u011b s\\u00a0elektrom\\u011brem. Pro zobrazen\\u00ed \\u010das\\u016f je nutn\\u00e9 \\u003Cstrong\\u003E\\u003Ca href=\\u0022\\/spinaci-hodiny\\u0022 target=\\u0022_blank\\u0022\\u003Ezn\\u00e1t povel\\u003C\\/a\\u003E\\u003C\\/strong\\u003E za\\u010d\\u00ednaj\\u00edc\\u00ed p\\u00edsmeny PH, kter\\u00fd b\\u00fdv\\u00e1 \\u003Cstrong\\u003Evylepen na p\\u0159edn\\u00ed stran\\u011b p\\u0159ep\\u00ednac\\u00edch hodin\\u003C\\/strong\\u003E.\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/p\\u003E\\n\\u003Cp\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003EU pr\\u016fb\\u011bhov\\u00fdch nebo chytr\\u00fdch elektrom\\u011br\\u016f je tento \\u003Cstrong\\u003Ep\\u0159ij\\u00edma\\u010d sou\\u010d\\u00e1st\\u00ed elektrom\\u011bru\\u003C\\/strong\\u003E. V\\u00a0rozvad\\u011b\\u010di naleznete pouze elektrom\\u011br.\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E \\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003EPovel pro zji\\u0161t\\u011bn\\u00ed \\u010das\\u016f n\\u00edzk\\u00e9ho tarifu \\u003Cstrong\\u003Enajdete na displeji elektrom\\u011bru\\u003C\\/strong\\u003E.\\u00a0\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003EPokud\\u00a0\\u003Cspan class=\\u0022msoIns\\u0022\\u003E\\u003Cspan\\u003Ese v\\u00e1m neda\\u0159\\u00ed tento \\u00fadaj zjistit, m\\u016f\\u017eete se pod\\u00edvat do na\\u0161ich \\u003Cstrong\\u003E\\u003Ca href=\\u0022\\/navody-k-elektromerum\\u0022 target=\\u0022_blank\\u0022\\u003En\\u00e1vod\\u016f k elektrom\\u011br\\u016fm\\u003C\\/a\\u003E\\u003C\\/strong\\u003E.\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/p\\u003E\\n\\u003Cp\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003EChcete se dozv\\u011bd\\u011bt v\\u00edce o p\\u0159\\u00edstroj\\u00edch, kter\\u00e9 pou\\u017e\\u00edv\\u00e1me pro p\\u0159ep\\u00edn\\u00e1n\\u00ed spot\\u0159eby mezi vysok\\u00fdm a n\\u00edzk\\u00fdm tarifem nebo hled\\u00e1te dal\\u0161\\u00ed n\\u00e1pov\\u011bdu, jak zjistit k\\u00f3d \\/ povel, m\\u016f\\u017eete se pod\\u00edvat na str\\u00e1nku v\\u011bnovanou \\u003Cstrong\\u003E\\u003Ca href=\\u0022\\/vse-o-nizkem-tarifu-hdo-nocni-proud\\u0022 target=\\u0022_blank\\u0022\\u003En\\u00edzk\\u00e9mu tarifu\\u003C\\/a\\u003E\\u003C\\/strong\\u003E. \\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/p\\u003E\\n\\u003Ch2\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cstrong\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003EVyhled\\u00e1n\\u00ed \\u010das\\u016f platnosti n\\u00edzk\\u00e9ho\\u0026nbsp;tarifu:\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/strong\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/h2\\u003E\\n\\u003Cp\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cstrong\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003EPro vyhled\\u00e1n\\u00ed \\u010das\\u016f n\\u00edzk\\u00e9ho tarifu pro va\\u0161e odb\\u011brn\\u00e9 m\\u00edsto pokra\\u010dujte v\\u00fdb\\u011brem typu sv\\u00e9ho p\\u0159\\u00edstroje.\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/strong\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E Pro n\\u00e1pov\\u011bdu k\\u00a0dan\\u00e9mu typu p\\u0159\\u00edstroje klikn\\u011bte na symbol plus v\\u00a0prav\\u00e9m horn\\u00edm rohu odpov\\u00eddaj\\u00edc\\u00edho\\u0026nbsp;bloku.\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/p\\u003E\",\n" +
            "\t\t\t\t\"infoBox\": \"\\u003Cp\\u003E\\u003Cstrong\\u003EVa\\u0161e odb\\u011brn\\u00e9 m\\u00edsto se nach\\u00e1z\\u00ed v oblasti, kde mohou b\\u00fdt v mal\\u00e9m mno\\u017estv\\u00ed instalov\\u00e1ny i p\\u0159\\u00edstroje s operativn\\u00edm \\u0159\\u00edzen\\u00edm. \\u003C\\/strong\\u003E\\u003Cbr \\/\\u003E\\nJak pozn\\u00e1te, \\u017ee pr\\u00e1v\\u011b Vy m\\u00e1te operativn\\u011b \\u0159\\u00edzen\\u00fd p\\u0159\\u00edstroj? Na uk\\u00e1zku toho p\\u0159\\u00edstroje se m\\u016f\\u017eete pod\\u00edvat\\u003Cstrong\\u003E\\u00a0\\u003C\\/strong\\u003E\\u003Ca href=\\u0022#modal-hdo-how-to\\u0022\\u003E\\u003Cstrong\\u003Ezde\\u003C\\/strong\\u003E\\u003C\\/a\\u003E.\\u003Cbr \\/\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003EPokud m\\u00e1te na Va\\u0161em m\\u00edst\\u011b p\\u0159ij\\u00edma\\u010d s n\\u00e1pisem \\u201ed\\u00e1lkov\\u00e1 parametrizace\\u201c, tak V\\u00e1m n\\u00edzk\\u00fd tarif bude sp\\u00ednat dle \\u010dasov\\u00e9ho rozvrhu \\u003Cstrong\\u003E\\u010d\\u00edslo\\u00a01\\u003C\\/strong\\u003E, v\\u00a0opa\\u010dn\\u00e9m p\\u0159\\u00edpad\\u011b plat\\u00ed \\u010dasov\\u00fd rozvrh \\u010d\\u00edslo\\u0026nbsp;2.\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/p\\u003E\",\n" +
            "\t\t\t\t\"hdo\": {\n" +
            "\t\t\t\t\t\"tile\": {\n" +
            "\t\t\t\t\t\t\"heading\": \"\\u003Ch2\\u003EP\\u0159ij\\u00edma\\u010de\\u0026nbsp;HDO\\u003C\\/h2\\u003E\",\n" +
            "\t\t\t\t\t\t\"content\": \"\\u003Cul\\u003E\\u003Cli\\u003EKlasick\\u00e9 typy p\\u0159ij\\u00edma\\u010d\\u016f HDO pro oblast v\\u00fdchodu\\n\\u003Cul\\u003E\\u003Cli\\u003EPovel se skl\\u00e1d\\u00e1 z kombinace \\u010d\\u00edsel a p\\u00edsmen (konkr\\u00e9tn\\u011b A, B, D, P \\u2013 ne v\\u017edy zde naleznete v\\u0161echna tato p\\u00edsmena \\u2013 krom\\u011b B a P, ta se objevuj\\u00ed\\u0026nbsp;v\\u017edy).\\u003C\\/li\\u003E\\n\\u003Cli\\u003EP\\u0159\\u00edklady povel\\u016f: A1B2DP6, 1B2DP5, 1B2P6, A1B2DP3, 1B2P5, A1B1DP1 A1B1DP2 A1B1DP6, 1B2P1 1B2P2,\\u0026nbsp;apod.\\u003C\\/li\\u003E\\n\\u003Cli\\u003EM\\u016f\\u017ee se skl\\u00e1dat z jednoho povelu nebo kombinace a\\u017e t\\u0159\\u00ed povel\\u016f v\\u0026nbsp;\\u0159ad\\u011b.\\u003C\\/li\\u003E\\n\\u003Cli\\u003EPokud si nejste jisti svoj\\u00ed kombinac\\u00ed, naleznete ji na va\\u0161em p\\u0159ij\\u00edma\\u010di\\u0026nbsp;HDO.\\u003C\\/li\\u003E\\n\\u003Cli\\u003E\\u003Ca href=\\u0022\\/jak-vyhledat-kod-hdo\\u0022 target=\\u0022_blank\\u0022\\u003EKde najdu sv\\u016fj\\u0026nbsp;povel?\\u003C\\/a\\u003E\\u003C\\/li\\u003E\\n\\u003C\\/ul\\u003E\\u003C\\/li\\u003E\\n\\u003Cli\\u003EKlasick\\u00e9 typy p\\u0159ij\\u00edma\\u010d\\u016f HDO pro oblast z\\u00e1padu\\n\\u003Cul\\u003E\\u003Cli\\u003EK\\u00f3d se skl\\u00e1d\\u00e1 z kombinace t\\u0159\\u00ed \\u010d\\u00edsel v rozmez\\u00ed 101 a\\u017e\\u0026nbsp;612.\\u003C\\/li\\u003E\\n\\u003Cli\\u003EP\\u0159\\u00edklady k\\u00f3d\\u016f: 101, 121 129 132, 102, 121,\\u0026nbsp;apod.\\u003C\\/li\\u003E\\n\\u003Cli\\u003EM\\u016f\\u017ee se skl\\u00e1dat z jednoho k\\u00f3du nebo kombinace a\\u017e t\\u0159\\u00ed k\\u00f3d\\u016f v\\u0026nbsp;\\u0159ad\\u011b.\\u003C\\/li\\u003E\\n\\u003Cli\\u003EPokud si nejste jisti svoj\\u00ed kombinac\\u00ed, naleznete ji na va\\u0161em p\\u0159ij\\u00edma\\u010di\\u0026nbsp;HDO.\\u003C\\/li\\u003E\\n\\u003Cli\\u003E\\u003Ca href=\\u0022\\/jak-vyhledat-kod-hdo\\u0022 target=\\u0022_blank\\u0022\\u003EKde najdu sv\\u016fj\\u0026nbsp;k\\u00f3d?\\u003C\\/a\\u003E\\u003C\\/li\\u003E\\n\\u003C\\/ul\\u003E\\u003C\\/li\\u003E\\n\\u003C\\/ul\\u003E\"\n" +
            "\t\t\t\t\t},\n" +
            "\t\t\t\t\t\"region\": {\n" +
            "\t\t\t\t\t\t\"exceptions\": \"\\u003Cul\\u003E\\u003Cli\\u003EZvolte obec \\u003Cstrong\\u003EDa\\u010dice\\u003C\\/strong\\u003E, pokud m\\u00e1te odb\\u011brn\\u00e9 m\\u00edsto v obc\\u00edch: B\\u00e1\\u0148ovice, B\\u011bl\\u010dovice, B\\u00edlkov, Borek (Jind\\u0159ich\\u016fv Hradec), Borov\\u00e1, Brandl\\u00edn (Jind\\u0159ich\\u016fv Hradec), Bude\\u010d (Jind\\u0159ich\\u016fv Hradec), Bud\\u00ed\\u0161kovice, Cizkrajov, \\u010cerven\\u00fd Hr\\u00e1dek, \\u010cesk\\u00fd Rudolec, Dan\\u010dovice, De\\u0161n\\u00e1 (Jind\\u0159ich\\u016fv Hradec), Dobroho\\u0161\\u0165, Doln\\u00ed Bol\\u00edkov, Doln\\u00ed N\\u011bm\\u010dice, Dv\\u016fr Hejnice, He\\u0159mane\\u010d (Jind\\u0159ich\\u016fv Hradec), Hlubok\\u00e1, Hole\\u0161ice, Horn\\u00ed N\\u011bm\\u010dice, Horn\\u00ed Rad\\u00edkov, Horn\\u00ed Slatina, Hostkovice, Hradi\\u0161\\u0165ko, H\\u0159\\u00ed\\u0161ice, Chlumec (Jind\\u0159ich\\u016fv Hradec), Chot\\u011bbudice, Chvalet\\u00edn, Chvalkovice (Jind\\u0159ich\\u016fv Hradec), Chytrov, Janov (Jind\\u0159ich\\u016fv Hradec), Jersice, Kadolec (Jind\\u0159ich\\u016fv Hradec), Karlov (B\\u00edlkov), Kl\\u00e1\\u0161ter, Kosteln\\u00ed Vyd\\u0159\\u00ed, Krokovice, Lid\\u00e9\\u0159ovice (Jind\\u0159ich\\u016fv Hradec), Lipnice (Jind\\u0159ich\\u016fv Hradec), Lipolec, Lipov\\u00e1 (Jind\\u0159ich\\u016fv Hradec), Lomy (Jind\\u0159ich\\u016fv Hradec), Louck\\u00fd Ml\\u00fdn, Lov\\u010dovice, M\\u00e1ch\\u016fv Ml\\u00fdn, Mal\\u00fd P\\u011b\\u010d\\u00edn, Mane\\u0161ovice, Mark\\u00e9ta, Markvarec (Jind\\u0159ich\\u016fv Hradec), Mar\\u0161ov (Jind\\u0159ich\\u016fv Hradec), Mat\\u011bjovec (Jind\\u0159ich\\u016fv Hradec), Menhartice, Modletice, Muti\\u0161ov, Mutn\\u00e1, Mysletice, Nov\\u00e1 Ves (Jind\\u0159ich\\u016fv Hradec), Nov\\u00e9 Dvory (Volf\\u00ed\\u0159ov), Nov\\u00e9 Dvory (Star\\u00e9 Hobz\\u00ed), Nov\\u00e9 Hobz\\u00ed, Ol\\u0161any (Jind\\u0159ich\\u016fv Hradec), Ol\\u0161\\u00ed (Jihlava), Ostojkovice, P\\u00e1lovice, Panensk\\u00e1, Pe\\u010d, Pen\\u00edkov, P\\u00edse\\u010dn\\u00e9 (Jind\\u0159ich\\u016fv Hradec), Pla\\u010dovice, Poldovka, Prost\\u0159edn\\u00ed Vyd\\u0159\\u00ed, Rad\\u00edkov, Radlice, Ranc\\u00ed\\u0159ov (Jind\\u0159ich\\u016fv Hradec), Ro\\u017enov, \\u0158e\\u010dice (Jind\\u0159ich\\u016fv Hradec), Slav\\u011bt\\u00edn, Slavonice, St\\u00e1lkov, Star\\u00e9 Hobz\\u00ed, Stoje\\u010d\\u00edn, \\u0160ach, Tou\\u017e\\u00edn, T\\u0159eb\\u011btice (Jind\\u0159ich\\u016fv Hradec), Urbane\\u010d, V\\u00e1clavov (Jind\\u0159ich\\u016fv Hradec), Valt\\u00ednov (TS2, Jind\\u0159ich\\u016fv Hradec), Velk\\u00e1 Lhota, Velk\\u00fd Jen\\u00edkov, Vesce (Jind\\u0159ich\\u016fv Hradec), Vesel\\u00ed\\u010dko, Vlastkovec, Vla\\u017einka, Vnorovice, Volf\\u00ed\\u0159ov, Vost\\u00e9zy, Zadn\\u00ed Vyd\\u0159\\u00ed,\\u0026nbsp;\\u017dupanovice.\\u003C\\/li\\u003E\\n\\u003Cli\\u003EZvolte okres \\u003Cstrong\\u003EB\\u0159eclav\\u003C\\/strong\\u003E, pokud m\\u00e1te odb\\u011brn\\u00e9 m\\u00edsto v obc\\u00edch: Cvr\\u010dovice (Brno-venkov), Iva\\u0148 (Brno-venkov), Pasohl\\u00e1vky (Brno-venkov), Poho\\u0159elice (Brno-venkov), P\\u0159ibice (Brno-venkov), Vlasatice\\u00a0(Brno-venkov) a\\u0026nbsp;Vranovice\\u00a0(Brno-venkov).\\u003C\\/li\\u003E\\n\\u003C\\/ul\\u003E\"\n" +
            "\t\t\t\t\t},\n" +
            "\t\t\t\t\t\"code\": {\n" +
            "\t\t\t\t\t\t\"intro\": \"\\u003Cp\\u003E\\u003Cstrong\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003EPokud si nejste jist\\u00ed, kde sv\\u016fj k\\u00f3d \\/ povel najdete nebo v\\u00a0jak\\u00e9 kombinaci jej m\\u00e1te zadat, pod\\u00edvejte se \\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/strong\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Ca href=\\u0022\\/jak-vyhledat-kod-hdo\\u0022 target=\\u0022_blank\\u0022\\u003E\\u003Cstrong\\u003Ezde\\u003C\\/strong\\u003E\\u003C\\/a\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003Cstrong\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E.\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/strong\\u003E\\u003C\\/p\\u003E\\n\\u003Cp\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003EPokud je va\\u0161e kombinace k\\u00f3du pouze \\u003Cstrong\\u003E\\u010d\\u00edseln\\u00e1\\u003C\\/strong\\u003E, vypl\\u0148ujete pole \\u003Cstrong\\u003EK\\u00f3d HDO\\u003C\\/strong\\u003E.\\u003Cbr \\/\\u003E\\nPokud je v\\u00e1\\u0161 povel kombinac\\u00ed \\u003Cstrong\\u003Ep\\u00edsmen a \\u010d\\u00edsel\\u003C\\/strong\\u003E, vypl\\u0148ujete pole \\u003Cstrong\\u003EPovel HDO\\u003C\\/strong\\u003E. I kdy\\u017e v\\u00e1\\u0161 povel neza\\u010d\\u00edn\\u00e1 p\\u0159\\u00edmo p\\u00edsmenem A ale \\u010d\\u00edslic\\u00ed, vypl\\u0148te tuto po\\u010d\\u00e1te\\u010dn\\u00ed \\u010d\\u00edslici do pol\\u00ed\\u010dka za p\\u00edsmeno A, a pak d\\u00e1le pokra\\u010dujte ve vypl\\u0148ov\\u00e1n\\u00ed povelu sm\\u011brem\\u0026nbsp;doprava.\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/p\\u003E\\n\\u003Cp\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003EPo zad\\u00e1n\\u00ed k\\u00f3du\\/povelu potvr\\u010fte sv\\u016fj v\\u00fdb\\u011br klikem v na\\u0161ept\\u00e1va\\u010di, kter\\u00fd se v\\u00e1m zobrazil n\\u00ed\\u017ee, a pokra\\u010dujte k zobrazen\\u00ed\\u0026nbsp;\\u010das\\u016f.\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/p\\u003E\",\n" +
            "\t\t\t\t\t\t\"hint\": \"\\u003Cdiv class=\\u0022tooltip-grid\\u0022\\u003E\\n\\u003Cp class=\\u0022kody\\u0022\\u003E\\u003Cstrong\\u003EN\\u00e1pov\\u011bda ke\\u0026nbsp;k\\u00f3d\\u016fm:\\u003C\\/strong\\u003E\\u003C\\/p\\u003E\\n\\u003Cul\\u003E\\u003Cli\\u003EK\\u00f3d se skl\\u00e1d\\u00e1 z kombinace t\\u0159\\u00ed \\u010d\\u00edsel v rozmez\\u00ed 101 a\\u017e\\u0026nbsp;612.\\u003C\\/li\\u003E\\n\\u003Cli\\u003EP\\u0159\\u00edklady k\\u00f3d\\u016f: 101, 121 129 132, 102, 121,\\u0026nbsp;apod.\\u003C\\/li\\u003E\\n\\u003Cli\\u003EM\\u016f\\u017ee se skl\\u00e1dat z jednoho k\\u00f3du nebo kombinace a\\u017e t\\u0159\\u00ed k\\u00f3d\\u016f v\\u0026nbsp;\\u0159ad\\u011b.\\u003C\\/li\\u003E\\n\\u003Cli\\u003EPokud si nejste jisti svoj\\u00ed kombinac\\u00ed, naleznete ji na va\\u0161em p\\u0159ij\\u00edma\\u010di\\u0026nbsp;HDO.\\u003C\\/li\\u003E\\n\\u003C\\/ul\\u003E\\u003Cp class=\\u0022povely\\u0022\\u003E\\u003Cstrong\\u003EN\\u00e1pov\\u011bda k\\u0026nbsp;povel\\u016fm:\\u003C\\/strong\\u003E\\u003C\\/p\\u003E\\n\\u003Cul\\u003E\\u003Cli\\u003EPovel se skl\\u00e1d\\u00e1 z kombinace \\u010d\\u00edsel a p\\u00edsmen (konkr\\u00e9tn\\u011b A, B, D, P \\u2013 ne v\\u017edy zde naleznete v\\u0161echna tato p\\u00edsmena \\u2013 krom\\u011b B a P, ta se objevuj\\u00ed\\u0026nbsp;v\\u017edy).\\u003C\\/li\\u003E\\n\\u003Cli\\u003EP\\u0159\\u00edklady povel\\u016f: A1B2DP6, 1B2DP5, 1B2P6, A1B2DP3, 1B2P5, A1B1DP1 A1B1DP2 A1B1DP6, 1B2P1 1B2P2,\\u0026nbsp;apod.\\u003C\\/li\\u003E\\n\\u003Cli\\u003EM\\u016f\\u017ee se skl\\u00e1dat z jednoho povelu nebo kombinace a\\u017e t\\u0159\\u00ed povel\\u016f v\\u0026nbsp;\\u0159ad\\u011b.\\u003C\\/li\\u003E\\n\\u003Cli\\u003EPokud si nejste jisti svoj\\u00ed kombinac\\u00ed, naleznete ji na va\\u0161em p\\u0159ij\\u00edma\\u010di HDO.\\u003Cbr \\/\\u003E\\n\\t\\u00a0\\u003C\\/li\\u003E\\n\\u003C\\/ul\\u003E\\u003Cp\\u003E\\u003Ca href=\\u0022\\/jak-vyhledat-kod-hdo\\u0022 target=\\u0022_blank\\u0022\\u003EJak vyhledat k\\u00f3d\\/povel\\u0026nbsp;HDO?\\u00a0\\u003C\\/a\\u003E\\u003C\\/p\\u003E\\n\\u003C\\/div\\u003E\"\n" +
            "\t\t\t\t\t},\n" +
            "\t\t\t\t\t\"wrongVariant\": \"\\u003Cp\\u003EZkontrolujte si, zda jste v\\u00e1\\u0161 k\\u00f3d\\/povel zadali spr\\u00e1vn\\u011b. Pokud je v\\u00e1\\u0161 k\\u00f3d\\/povel v jin\\u00e9m form\\u00e1tu ne\\u017e t\\u0159i \\u010d\\u00edsla (nap\\u0159. 101, 121) nebo AXBXDPX \\/ AXBXPX \\/ XBXDPX \\/ XBXPX (nap\\u0159. A1B2DP6, 1B2DP6), pou\\u017e\\u00edv\\u00e1te nespr\\u00e1vn\\u00fd blok pro vyhled\\u00e1v\\u00e1n\\u00ed. V tomto p\\u0159\\u00edpad\\u011b pros\\u00edm v prvn\\u00edm kroku naho\\u0159e na str\\u00e1nce vyberte mo\\u017enost \\u201cSpeci\\u00e1ln\\u00ed p\\u0159\\u00edstroje\\u201d a pokra\\u010dujte zad\\u00e1n\\u00edm va\\u0161eho\\u0026nbsp;k\\u00f3du\\/povelu.\\u003C\\/p\\u003E\"\n" +
            "\t\t\t\t},\n" +
            "\t\t\t\t\"special\": {\n" +
            "\t\t\t\t\t\"tile\": {\n" +
            "\t\t\t\t\t\t\"heading\": \"\\u003Ch2\\u003ESpeci\\u00e1ln\\u00ed\\u0026nbsp;p\\u0159\\u00edstroje\\u003C\\/h2\\u003E\",\n" +
            "\t\t\t\t\t\t\"content\": \"\\u003Cul\\u003E\\u003Cli\\u003EP\\u0159ep\\u00ednac\\u00ed hodiny\\n\\u003Cul\\u003E\\u003Cli\\u003EPovel za\\u010d\\u00edn\\u00e1 p\\u00edsmeny PH a je vylepen (od roku 2017) na p\\u0159edn\\u00ed stran\\u011b\\u0026nbsp;p\\u0159\\u00edstroje.\\u003C\\/li\\u003E\\n\\u003Cli\\u003EU star\\u0161\\u00edch typ\\u016f, kde vylepen\\u00fd \\u0161t\\u00edtek nenajdete, se pros\\u00edm obra\\u0165te na na\\u0161i linku 800 22 55 77, r\\u00e1di v\\u00e1m\\u0026nbsp;porad\\u00ed.\\u003C\\/li\\u003E\\n\\u003Cli\\u003EP\\u0159\\u00edklady povel\\u016f: PH20_1, PH20_52, PH16, PH8_14, PH8_00,\\u0026nbsp;apod.\\u003C\\/li\\u003E\\n\\u003Cli\\u003EV\\u00edce informac\\u00ed o star\\u0161\\u00edch typech p\\u0159\\u00edstroj\\u016f najdete \\u003Ca href=\\u0022\\/spinaci-hodiny\\u0022 target=\\u0022_blank\\u0022\\u003Ena t\\u00e9to\\u00a0str\\u00e1nce\\u003C\\/a\\u003E.\\u003C\\/li\\u003E\\n\\u003C\\/ul\\u003E\\u003C\\/li\\u003E\\n\\u003Cli\\u003EPr\\u016fb\\u011bhov\\u00e9 elektrom\\u011bry s intern\\u00edm \\u0159\\u00edzen\\u00edm tarif\\u016f (bez p\\u0159ij\\u00edma\\u010de HDO)\\n\\u003Cul\\u003E\\u003Cli\\u003EPovel najdete na \\u0161t\\u00edtku\\/displeji\\u0026nbsp;elektrom\\u011bru.\\u003C\\/li\\u003E\\n\\u003Cli\\u003EP\\u0159\\u00edklady povel\\u016f: C25, CD2526_1, D57, C46, tou2, tou3,\\u0026nbsp;apod.\\u003C\\/li\\u003E\\n\\u003C\\/ul\\u003E\\u003C\\/li\\u003E\\n\\u003Cli\\u003EChytr\\u00e9 elektrom\\u011bry (Smart metery)\\n\\u003Cul\\u003E\\u003Cli\\u003EPovel najdete na \\u0161t\\u00edtku\\/displeji chytr\\u00e9ho elektrom\\u011bru a za\\u010d\\u00edn\\u00e1 v\\u017edy p\\u00edsmenem A, kter\\u00e9 n\\u00e1sleduje C nebo\\u0026nbsp;d.\\u003C\\/li\\u003E\\n\\u003Cli\\u003EP\\u0159\\u00edklady povel\\u016f: ACd25, Ad57, ACd56, Ad572, Ad61, ACd271, AC46,\\u0026nbsp;apod.\\u003C\\/li\\u003E\\n\\u003Cli\\u003EV\\u00edce o chytr\\u00e9m m\\u011b\\u0159en\\u00ed najdete \\u003Ca href=\\u0022\\/chytre-mereni\\u0022 target=\\u0022_blank\\u0022\\u003Ena t\\u00e9to str\\u00e1nce\\u003C\\/a\\u003E.\\u003C\\/li\\u003E\\n\\u003C\\/ul\\u003E\\u003C\\/li\\u003E\\n\\u003C\\/ul\\u003E\"\n" +
            "\t\t\t\t\t},\n" +
            "\t\t\t\t\t\"code\": {\n" +
            "\t\t\t\t\t\t\"intro\": \"\\u003Cp\\u003E\\u003Cstrong\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003EPokud si nejste jist\\u00ed, kde sv\\u016fj k\\u00f3d \\/ povel najdete nebo v\\u00a0jak\\u00e9 kombinaci jej m\\u00e1te zadat, pod\\u00edvejte se \\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/strong\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Ca href=\\u0022\\/jak-vyhledat-kod-hdo\\u0022 target=\\u0022_blank\\u0022\\u003E\\u003Cstrong\\u003Ezde\\u003C\\/strong\\u003E\\u003C\\/a\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003Cstrong\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E.\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/strong\\u003E\\u003C\\/p\\u003E\\n\\u003Cp\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003EPo zad\\u00e1n\\u00ed k\\u00f3du\\/povelu potvr\\u010fte sv\\u016fj v\\u00fdb\\u011br klikem v na\\u0161ept\\u00e1va\\u010di, kter\\u00fd se v\\u00e1m zobrazil n\\u00ed\\u017ee, a pokra\\u010dujte k zobrazen\\u00ed\\u0026nbsp;\\u010das\\u016f.\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/p\\u003E\",\n" +
            "\t\t\t\t\t\t\"hint\": \"\\u003Cp class=\\u0022MsoCommentText\\u0022\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cstrong\\u003E\\u003Cspan\\u003EP\\u0159ep\\u00ednac\\u00ed hodiny\\u003C\\/span\\u003E\\u003C\\/strong\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003Cbr \\/\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003EU tohoto za\\u0159\\u00edzen\\u00ed je d\\u016fle\\u017eit\\u00e9 zjistit ze \\u0161t\\u00edtku k\\u00f3d pro vyhled\\u00e1n\\u00ed va\\u0161ich \\u010das\\u016f sp\\u00edn\\u00e1n\\u00ed za\\u010d\\u00ednaj\\u00edc\\u00ed p\\u00edsmeny PH, kter\\u00fd b\\u00fdv\\u00e1 vylepen na p\\u0159edn\\u00ed stran\\u011b p\\u0159ep\\u00ednac\\u00edch hodin, nap\\u0159. \\u003Cem\\u003EPH20_1\\u003C\\/em\\u003E.\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003Cbr \\/\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003EPokud tam tento \\u00fadaj nenajdete, pros\\u00edm obra\\u0165te na na\\u0161i Nonstop linku EG.D, r\\u00e1di v\\u00e1m s k\\u00f3dem\\/povelem\\u0026nbsp;porad\\u00edme.\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/p\\u003E\\n\\u003Cdiv class=\\u0022tooltip__hdo-photo\\u0022\\u003E\\u003Cimg alt=\\u0022Povel na p\\u0159edn\\u00ed stran\\u011b p\\u0159\\u00edstroje\\u0022 src=\\u0022\\/themes\\/reason_eon\\/img\\/hdo-ph-web.jpg\\u0022 \\/\\u003E\\u003C\\/div\\u003E\\n\\u003Cp\\u003E\\u003Cstrong\\u003EPr\\u016fb\\u011bhov\\u00fd elektrom\\u011br s\\u00a0intern\\u00edm \\u0159\\u00edzen\\u00edm tarifu\\u003C\\/strong\\u003E\\u003Cbr \\/\\u003E\\nPovel najdete na \\u0161t\\u00edtku elektrom\\u011bru, nap\\u0159. ve tvaru \\u003Cem\\u003EC25, CD2526_1, D57, C46, tou2, tou3,\\u003C\\/em\\u003E aj.\\u003C\\/p\\u003E\\n\\u003Cp class=\\u0022MsoCommentText\\u0022\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cstrong\\u003E\\u003Cspan\\u003EChytr\\u00e9 m\\u011b\\u0159en\\u00ed (Smart metery)\\u003C\\/span\\u003E\\u003C\\/strong\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003Cbr \\/\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003E\\u003Cspan\\u003EPovel najdete na displeji elektrom\\u011bru, nap\\u0159. ve tvaru \\u003Cem\\u003EACd25,\\u0026nbsp;Ad57.\\u003C\\/em\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/span\\u003E\\u003C\\/p\\u003E\\n\\u003Cdiv class=\\u0022tooltip__hdo-photo--bottom\\u0022\\u003E\\u003Cimg alt=\\u0022Povel na p\\u0159edn\\u00ed stran\\u011b p\\u0159\\u00edstroje\\u0022 src=\\u0022\\/themes\\/reason_eon\\/img\\/hdo-smart-web.png\\u0022 \\/\\u003E\\u003C\\/div\\u003E\"\n" +
            "\t\t\t\t\t},\n" +
            "\t\t\t\t\t\"wrongVariant\": \"\\u003Cp\\u003EZkontrolujte si, zda jste v\\u00e1\\u0161 k\\u00f3d\\/povel zadali spr\\u00e1vn\\u011b. Pokud je v\\u00e1\\u0161 k\\u00f3d\\/povel ve form\\u00e1tu t\\u0159\\u00ed \\u010d\\u00edsel (nap\\u0159. 101, 121) nebo AXBXDPX \\/ AXBXPX \\/ XBXDPX \\/ XBXPX (nap\\u0159. A1B2DP6, 1B2DP6), pou\\u017e\\u00edv\\u00e1te nespr\\u00e1vn\\u00fd blok pro vyhled\\u00e1v\\u00e1n\\u00ed. V tomto p\\u0159\\u00edpad\\u011b pros\\u00edm v p\\u0159edchoz\\u00edm kroku vyberte mo\\u017enost \\u201cP\\u0159ij\\u00edma\\u010de HDO\\u201d a pokra\\u010dujte v\\u00fdb\\u011brem va\\u0161\\u00ed lokality z mapky nebo\\u0026nbsp;v\\u00fdjimek.\\u003C\\/p\\u003E\"\n" +
            "\t\t\t\t},\n" +
            "\t\t\t\t\"mobile_app\": \"\\u003Cdiv class=\\u0022mobile-app\\u0022\\u003E\\n    \\u003Cdiv class=\\u0022mobile-app__title\\u0022\\u003E\\n                        St\\u00e1hn\\u011bte si mobiln\\u00ed aplikaci EG.D Distribuce\\n\\n            \\u003C\\/div\\u003E\\n\\n        \\u003Cul\\u003E\\u003Cli\\u003Ena\\u010dten\\u00ed polohy odb\\u011brn\\u00fdch m\\u00edst pomoc\\u00ed\\u0026nbsp;GPS\\u003C\\/li\\u003E\\n\\u003C\\/ul\\u003E\\u003Cul\\u003E\\u003Cli\\u003Enotifikace p\\u0159i zm\\u011bn\\u011b \\u010dasu n\\u00edzk\\u00e9ho tarifu\\u0026nbsp;(HDO)\\u003C\\/li\\u003E\\n\\u003C\\/ul\\u003E\\u003Cul\\u003E\\u003Cli\\u003Enotifikace pro pl\\u00e1novan\\u00e9\\u0026nbsp;odst\\u00e1vky\\u003C\\/li\\u003E\\n\\u003C\\/ul\\u003E\\u003Cul\\u003E\\u003Cli\\u003Enotifikace pro aktu\\u00e1ln\\u00ed poruchy\\u0026nbsp;elekt\\u0159iny\\u003C\\/li\\u003E\\n\\u003C\\/ul\\u003E\\u003Cul\\u003E\\u003Cli\\u003Emo\\u017enost hl\\u00e1\\u0161en\\u00ed z\\u00e1vad v\\u010detn\\u011b obrazov\\u00e9\\u0026nbsp;p\\u0159\\u00edlohy\\u003C\\/li\\u003E\\n\\u003C\\/ul\\u003E\\u003Cul\\u003E\\u003Cli\\u003Eonline nahl\\u00e1\\u0161en\\u00ed\\u0026nbsp;samoode\\u010dtu\\u003C\\/li\\u003E\\n\\u003C\\/ul\\u003E\\u003Cul\\u003E\\u003Cli\\u003E\\u017e\\u00e1dost o odplombov\\u00e1n\\u00ed online pro odbornou\\u0026nbsp;ve\\u0159ejnost\\u003C\\/li\\u003E\\n\\u003C\\/ul\\u003E\\u003Cul\\u003E\\u003Cli\\u003Enov\\u00fd p\\u0159ehledn\\u00fd design\\u003Cbr \\/\\u003E\\n\\t\\u00a0\\u003C\\/li\\u003E\\n\\u003Cli\\u003Ev\\u00edce info o aplikaci \\u003Ca data-entity-substitution=\\u0022canonical\\u0022 data-entity-type=\\u0022node\\u0022 data-entity-uuid=\\u00227053c8ed-df8c-47d3-a648-129393dc240b\\u0022 href=\\u0022\\/mobilni-aplikace-distribuce\\u0022 rel=\\u0022noopener\\u0022 target=\\u0022_blank\\u0022\\u003EZDE\\u003C\\/a\\u003E\\u003C\\/li\\u003E\\n\\u003C\\/ul\\u003E\\u003Cp\\u003E\\u00a0\\u003C\\/p\\u003E\\n\\u003Cp\\u003E\\u003Ciframe frameborder=\\u00220\\u0022 height=\\u0022200\\u0022 src=\\u0022https:\\/\\/www.youtube.com\\/embed\\/-FToyxshGo0\\u0022 width=\\u0022350\\u0022\\u003E\\u003C\\/iframe\\u003E\\u003C\\/p\\u003E\\n\\n\\n    \\u003Cdiv class=\\u0022columns\\u0022\\u003E\\n        \\u003Cdiv class=\\u0022column column--s-1of2 mobile-app__image-link-wrapper\\u0022\\u003E\\n            \\u003Ca target=\\u0022_blank\\u0022 rel=\\u0022noopener noreferrer\\u0022 href=\\u0022https:\\/\\/play.google.com\\/store\\/apps\\/details?id=com.eon.distribuce\\u0022 class=\\u0022mobile-app__image-link\\u0022\\u003E\\n                \\u003Cimg src=\\u0022\\/themes\\/reason_eon\\/img\\/google-play.jpg\\u0022 alt=\\u0022Google Play\\u0022\\u003E\\n            \\u003C\\/a\\u003E\\n            \\u003Ca target=\\u0022_blank\\u0022 rel=\\u0022noopener noreferrer\\u0022 href=\\u0022https:\\/\\/apps.apple.com\\/cz\\/app\\/distribuce\\/id1270799695?l=cs\\u0022 class=\\u0022mobile-app__image-link\\u0022\\u003E\\n                \\u003Cimg src=\\u0022\\/themes\\/reason_eon\\/img\\/app-store.jpg\\u0022 alt=\\u0022App Store\\u0022\\u003E\\n            \\u003C\\/a\\u003E\\n        \\u003C\\/div\\u003E\\n        \\u003Cdiv class=\\u0022column column--s-1of2\\u0022\\u003E\\n            \\u003Cdiv class=\\u0022mobile-app__phone\\u0022\\u003E\\n                \\u003Cimg src=\\u0022\\/themes\\/reason_eon\\/img\\/app.png\\u0022 alt=\\u0022App\\u0022\\u003E\\n            \\u003C\\/div\\u003E\\n        \\u003C\\/div\\u003E\\n    \\u003C\\/div\\u003E\\n\\u003C\\/div\\u003E\\n\",\n" +
            "\t\t\t\t\"title\": {\n" +
            "\t\t\t\t\t\"device\": \"\\u003Ch2\\u003EVyberte v\\u00e1\\u0161 typ\\u0026nbsp;p\\u0159\\u00edstroje\\u003C\\/h2\\u003E\",\n" +
            "\t\t\t\t\t\"locality\": \"\\u003Ch2\\u003EVyberte, v jak\\u00e9 lokalit\\u011b je va\\u0161e odb\\u011brn\\u00e9\\u0026nbsp;m\\u00edsto\\u003C\\/h2\\u003E\",\n" +
            "\t\t\t\t\t\"code\": \"\\u003Ch2\\u003EZadejte v\\u00e1\\u0161 k\\u00f3d \\/\\u0026nbsp;povel\\u003C\\/h2\\u003E\"\n" +
            "\t\t\t\t}\n" +
            "\t\t\t},\n" +
            "\t\t\t\"regionExceptions\": [\n" +
            "\t\t\t\t[\"B\\u00e1\\u0148ovice\", 16, 24],\n" +
            "\t\t\t\t[\"B\\u011bl\\u010dovice\", 16, 24],\n" +
            "\t\t\t\t[\"B\\u00edlkov\", 16, 24],\n" +
            "\t\t\t\t[\"Borek\", 16, 24],\n" +
            "\t\t\t\t[\"Borov\\u00e1\", 16, 24],\n" +
            "\t\t\t\t[\"Brandl\\u00edn\", 16, 24],\n" +
            "\t\t\t\t[\"Bude\\u010d\", 16, 24],\n" +
            "\t\t\t\t[\"Bud\\u00ed\\u0161kovice\", 16, 24],\n" +
            "\t\t\t\t[\"Cizkrajov\", 16, 24],\n" +
            "\t\t\t\t[\"\\u010cerven\\u00fd Hr\\u00e1dek\", 16, 24],\n" +
            "\t\t\t\t[\"\\u010cesk\\u00fd Rudolec\", 16, 24],\n" +
            "\t\t\t\t[\"Dan\\u010dovice\", 16, 24],\n" +
            "\t\t\t\t[\"De\\u0161n\\u00e1\", 16, 24],\n" +
            "\t\t\t\t[\"Dobroho\\u0161\\u0165\", 16, 24],\n" +
            "\t\t\t\t[\"Doln\\u00ed Bol\\u00edkov\", 16, 24],\n" +
            "\t\t\t\t[\"Doln\\u00ed N\\u011bm\\u010dice\", 16, 24],\n" +
            "\t\t\t\t[\"Dv\\u016fr Hejnice\", 16, 24],\n" +
            "\t\t\t\t[\"He\\u0159mane\\u010d\", 16, 24],\n" +
            "\t\t\t\t[\"Hlubok\\u00e1\", 16, 24],\n" +
            "\t\t\t\t[\"Hole\\u0161ice\", 16, 24],\n" +
            "\t\t\t\t[\"Horn\\u00ed N\\u011bm\\u010dice\", 16, 24],\n" +
            "\t\t\t\t[\"Horn\\u00ed Rad\\u00edkov\", 16, 24],\n" +
            "\t\t\t\t[\"Horn\\u00ed Slatina\", 16, 24],\n" +
            "\t\t\t\t[\"Hostkovice\", 16, 24],\n" +
            "\t\t\t\t[\"Hradi\\u0161\\u0165ko\", 16, 24],\n" +
            "\t\t\t\t[\"H\\u0159\\u00ed\\u0161ice\", 16, 24],\n" +
            "\t\t\t\t[\"Chlumec\", 16, 24],\n" +
            "\t\t\t\t[\"Chot\\u011bbudice\", 12, 24],\n" +
            "\t\t\t\t[\"Chvalet\\u00edn\", 16, 24],\n" +
            "\t\t\t\t[\"Chvalkovice\", 16, 24],\n" +
            "\t\t\t\t[\"Chytrov\", 16, 24],\n" +
            "\t\t\t\t[\"Janov\", 16, 24],\n" +
            "\t\t\t\t[\"Jersice\", 16, 24],\n" +
            "\t\t\t\t[\"Kadolec\", 16, 24],\n" +
            "\t\t\t\t[\"Karlov\", 16, 24],\n" +
            "\t\t\t\t[\"Kl\\u00e1\\u0161ter\", 16, 24],\n" +
            "\t\t\t\t[\"Kosteln\\u00ed Vyd\\u0159\\u00ed\", 16, 24],\n" +
            "\t\t\t\t[\"Krokovice\", 16, 24],\n" +
            "\t\t\t\t[\"Lid\\u00e9\\u0159ovice\", 16, 24],\n" +
            "\t\t\t\t[\"Lipnice\", 16, 24],\n" +
            "\t\t\t\t[\"Lipolec\", 16, 24],\n" +
            "\t\t\t\t[\"Lipov\\u00e1\", 16, 24],\n" +
            "\t\t\t\t[\"Lomy\", 16, 24],\n" +
            "\t\t\t\t[\"Louck\\u00fd Ml\\u00fdn\", 16, 24],\n" +
            "\t\t\t\t[\"Lov\\u010dovice\", 12, 24],\n" +
            "\t\t\t\t[\"M\\u00e1ch\\u016fv Ml\\u00fdn\", 16, 24],\n" +
            "\t\t\t\t[\"Mal\\u00fd P\\u011b\\u010d\\u00edn\", 16, 24],\n" +
            "\t\t\t\t[\"Mane\\u0161ovice\", 16, 24],\n" +
            "\t\t\t\t[\"Marketa\", 16, 24],\n" +
            "\t\t\t\t[\"Markvarec\", 16, 24],\n" +
            "\t\t\t\t[\"Mar\\u0161ov\", 16, 24],\n" +
            "\t\t\t\t[\"Mat\\u011bjovec\", 16, 24],\n" +
            "\t\t\t\t[\"Menhartice\", 12, 24],\n" +
            "\t\t\t\t[\"Modletice\", 16, 24],\n" +
            "\t\t\t\t[\"Muti\\u0161ov\", 16, 24],\n" +
            "\t\t\t\t[\"Mutn\\u00e1\", 16, 24],\n" +
            "\t\t\t\t[\"Mysletice\", 14, 24],\n" +
            "\t\t\t\t[\"Nov\\u00e1 Ves\", 16, 24],\n" +
            "\t\t\t\t[\"Nov\\u00e9 Dvory (Volf\\u00ed\\u0159ov)\", 16, 24],\n" +
            "\t\t\t\t[\"Nov\\u00e9 Dvory (Star\\u00e9 Hobz\\u00ed)\", 16, 24],\n" +
            "\t\t\t\t[\"Nov\\u00e9 Hobz\\u00ed\", 16, 24],\n" +
            "\t\t\t\t[\"Ol\\u0161any\", 16, 24],\n" +
            "\t\t\t\t[\"Ol\\u0161\\u00ed\", 14, 24],\n" +
            "\t\t\t\t[\"Ostojkovice\", 16, 24],\n" +
            "\t\t\t\t[\"P\\u00e1lovice\", 12, 24],\n" +
            "\t\t\t\t[\"Panensk\\u00e1\", 12, 24],\n" +
            "\t\t\t\t[\"Pe\\u010d\", 16, 24],\n" +
            "\t\t\t\t[\"Pen\\u00edkov\", 16, 24],\n" +
            "\t\t\t\t[\"P\\u00edse\\u010dn\\u00e9\", 16, 24],\n" +
            "\t\t\t\t[\"Pla\\u010dovice\", 16, 24],\n" +
            "\t\t\t\t[\"Poldovka\", 16, 24],\n" +
            "\t\t\t\t[\"Prost\\u0159edn\\u00ed Vyd\\u0159\\u00ed\", 16, 24],\n" +
            "\t\t\t\t[\"Rad\\u00edkov\", 16, 24],\n" +
            "\t\t\t\t[\"Radlice\", 16, 24],\n" +
            "\t\t\t\t[\"Ranc\\u00ed\\u0159ov\", 16, 24],\n" +
            "\t\t\t\t[\"Ro\\u017enov\", 16, 24],\n" +
            "\t\t\t\t[\"\\u0158e\\u010dice\", 16, 24],\n" +
            "\t\t\t\t[\"Slav\\u011bt\\u00edn\", 16, 24],\n" +
            "\t\t\t\t[\"Slavonice\", 16, 24],\n" +
            "\t\t\t\t[\"St\\u00e1lkov\", 16, 24],\n" +
            "\t\t\t\t[\"Star\\u00e9 Hobz\\u00ed\", 16, 24],\n" +
            "\t\t\t\t[\"Stoje\\u010d\\u00edn\", 16, 24],\n" +
            "\t\t\t\t[\"\\u0160ach\", 16, 24],\n" +
            "\t\t\t\t[\"Tou\\u017e\\u00edn\", 16, 24],\n" +
            "\t\t\t\t[\"T\\u0159eb\\u011btice\", 16, 24],\n" +
            "\t\t\t\t[\"Urbane\\u010d\", 16, 24],\n" +
            "\t\t\t\t[\"V\\u00e1clavov\", 16, 24],\n" +
            "\t\t\t\t[\"Valt\\u00ednov (TS2)\", 16, 24],\n" +
            "\t\t\t\t[\"Velk\\u00e1 Lhota\", 16, 24],\n" +
            "\t\t\t\t[\"Velk\\u00fd Jen\\u00edkov\", 16, 24],\n" +
            "\t\t\t\t[\"Vesce\", 16, 24],\n" +
            "\t\t\t\t[\"Vesel\\u00ed\\u010dko\", 16, 24],\n" +
            "\t\t\t\t[\"Vlastkovec\", 16, 24],\n" +
            "\t\t\t\t[\"Vla\\u017einka\", 16, 24],\n" +
            "\t\t\t\t[\"Vnorovice\", 16, 24],\n" +
            "\t\t\t\t[\"Volf\\u00ed\\u0159ov\", 16, 24],\n" +
            "\t\t\t\t[\"Vostezy\", 16, 24],\n" +
            "\t\t\t\t[\"Zadn\\u00ed Vyd\\u0159\\u00ed\", 14, 24],\n" +
            "\t\t\t\t[\"\\u017dupanovice\", 16, 24],\n" +
            "\t\t\t\t[\"Cvr\\u010dovice\", 8, 10],\n" +
            "\t\t\t\t[\"Iva\\u0148\", 8, 10],\n" +
            "\t\t\t\t[\"Pasohl\\u00e1vky\", 8, 10],\n" +
            "\t\t\t\t[\"Poho\\u0159elice\", 8, 10],\n" +
            "\t\t\t\t[\"P\\u0159ibice\", 8, 10],\n" +
            "\t\t\t\t[\"Vlasatice\", 8, 10],\n" +
            "\t\t\t\t[\"Vranovice\", 8, 10],\n" +
            "\t\t\t\t[\"Nosislav\", 8, 23]\n" +
            "\t\t\t]\n" +
            "\t\t}\n" +
            "\t},\n" +
            "\t\"antibot\": {\n" +
            "\t\t\"forms\": {\n" +
            "\t\t\t\"views-exposed-form-search-autocomplete-header\": {\n" +
            "\t\t\t\t\"id\": \"views-exposed-form-search-autocomplete-header\",\n" +
            "\t\t\t\t\"key\": \"0gWCkk9aB1Nf5ieljb5Mc3-C2g5PDCFl_aVIGP5wD1k\"\n" +
            "\t\t\t}\n" +
            "\t\t}\n" +
            "\t},\n" +
            "\t\"search_api_autocomplete\": {\n" +
            "\t\t\"search_autocomplete\": {\n" +
            "\t\t\t\"auto_submit\": true,\n" +
            "\t\t\t\"min_length\": 3\n" +
            "\t\t}\n" +
            "\t},\n" +
            "\t\"language\": \"cs\",\n" +
            "\t\"user\": {\n" +
            "\t\t\"uid\": 0,\n" +
            "\t\t\"permissionsHash\": \"c8042ad81050fc1d07198ccf7b968746f2f9c113afed377f6a8989a0cb414692\"\n" +
            "\t}\n" +
            "}</script>\n" +
            "  <script type=\"text/javascript\" src=\"file:///android_asset/js_cw_-PLrgwIOnxx8gqxTJZGcUBNk2wzv8rhOGybK2Y-4.js\"\n" +
            "    wfd-invisible=\"true\"></script>\n" +
            "  <script type=\"text/javascript\" src=\"file:///android_asset/js_7F9oylb6BkJXuZtGzY-o84sxM-U-Ax0FPeZnmP2soSk.js\"\n" +
            "    wfd-invisible=\"true\"></script>\n" +
            "  <script type=\"text/javascript\" src=\"file:///android_asset/js_lYFLFrumHanZQsyOELvAkfM4y2o7A25j0-bhpkjz4Hc.js\"\n" +
            "    wfd-invisible=\"true\"></script>\n" +
            "  <script type=\"text/javascript\" src=\"file:///android_asset/js_1DF26LOAETnEz4Q1tK4rzQiWvRhLoiNeAJZWOWUmdes.js\"\n" +
            "    wfd-invisible=\"true\"></script>\n" +
            "  <script type=\"text/javascript\" src=\"file:///android_asset/js_3f-AkdHM-oWDcAiTwNrcaCq3xP9rCv1d63p8-bNnBIA.js\"\n" +
            "    wfd-invisible=\"true\"></script>\n" +
            "</body>\n" +
            "</html>";
}
