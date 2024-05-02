# Elektrické Odečty - Aplikace Pro Android

## O aplikaci
Elektrické odečty Aplikace slouží k průběžnému zaznamenávání stavu elektroměru. Průběžně propočítává cenu spotřebované elektrické energie a porovnává ji se zaplacenými zálohami. Aplikace je navržena pro zařízení s operačním systémem Android, minimální verze SDK je 21 (Android 5.0, Lollipop). Dále umožňuje zaznamenávat vystavené faktury za vyúčtování. Na základě stavu posledního záznamu na elektroměru a poslední obdržené vyúčtovací faktury lze dopočítat aktuální cenu spotřebované elektrické energie.  

### Odběrná místa
V aplikaci je možné vést evidenci několika odběrných míst. Vedení záznamů a další akce v aplikaci se vždy vztahují k aktuálně zvolenému místu. Pokud je odběrné místo odstraněno, jsou současně odstraněny všechny měsíční záznamy a faktury. Výjimkou je přehled. Pokud je uloženo více odběrných míst, lze pomocí šipek mezi nimi přepínat.  

### Měsíční záznamy
Měsíční záznamy jsou srdcem aplikace. Zde se porovnává cena měsíční spotřeby elektrické energie oproti zaplacené záloze (za předpokladu, že záloha je placena v měsíčním intervalu). V měsíčním záznamu se zobrazuje stav měřiče elektroměru, a současně se propočítává cena za spotřebovanou elektrickou energii ve vysokém (VT) a nízkém tarifu (NT) a cena fixních plateb za měsíc. Cena VT se skládá z obchodní ceny pro VT (neregulovaná cena), distribuční ceny pro VT (regulovaná cena), daně z elektřiny a cen za systémové služby. Cena NT se skládá z obchodní ceny pro NT (neregulovaná cena), distribuční ceny pro NT (regulovaná cena), daně z elektřiny a cen za systémové služby. Fixní platba za měsíc se skládá z cen za příkon hlavního jističe a ceny za činnost operátora trhu (OTE). Poslední část výpočtu je výpočet ceny na podporu obnovitelných zdrojů. V současné době je maximální částka 495 Kč/MWh (598,95 Kč s DPH), s kterou se v měsíčních odečtech počítá. Pokud by tato částka byla ve skutečnosti nižší, bude zohledněna až při výpočtu faktury. Pro rok 2023 jsou stanoveny maximální ceny energie a cena za obnovitelné zdroje je stanovena na 0. V aplikaci je možné přepínat mezi maximální cenou a plnou cenou. Rozdíly se okamžitě promítnou do výpočtů.
<p align="center">
  <img src="/mes_1.png" width="300" />
  <img src="/mes_2.png" width="300" /> 
</p>

### Záznam faktur
Záznam faktur je podobný měsíčním záznamům. Faktura se zobrazuje ve třech záložkách. **První záložka „Faktura“ obsahuje vlastní záznamy odečtů (minimálně jeden). Každý záznam má přidělený svůj ceník, podle kterého se počítá cena spotřebované elektrické energie. Pokud je záznamů více, celkový součet faktury se zobrazuje v dolní liště. Kliknutím na lištu se změní zobrazovaný součet. Součty se zobrazují v tomto pořadí**: Celková spotřeba VT → Celková spotřeba NT → Celková spotřeba VT a NT → Cena VT bez DPH → Cena NT bez DPH → Cena stálých platů bez DPH → Cena na podporu obnovitelných zdrojů (POZE) → Celková cena bez DPH → Celková cena s DPH → Zaplacené zálohy → Bilance (rozdíl mezi celkovou cenou a zaplacenými zálohami). Druhá záložka „Detaily“ zobrazuje dílčí záznamy výpočtů faktury. Tato část se může využít pro kontrolu s obdrženou fakturou od vašeho dodavatele. Zde lze nalézt, v jakých částech se nachází případná odchylka. Třetí záložka „Platby“ zaznamenává všechny zaplacené zálohy (případně slevy).
<p align="center">
  <img src="/fak_1.png" width="300" />
  <img src="/fak_2.png" width="300" /> 
  <img src="/fak_3.png" width="300" /> 
</p>

V seznamu faktur se jako první záznam nachází „Období bez faktury“. Tato část automaticky vypočítává cenu spotřebované elektrické energie mezi posledním záznamem poslední faktury a posledním měsíčním odečtem. 

**Automatický výpočet lze nastavit do dvou režimů**: Plně automatický režim (výchozí nastavení): Plně kopíruje záznamy v měsíčních odečtech, včetně nastavení ceníku a výměny elektroměru. Editační tlačítka pro rozdělení a sloučení záznamů jsou v tomto režimu skryta. 

**Poloautomatický režim**: Z měsíčních odečtů přebírá pouze poslední záznam stavu elektroměru. Ostatní rozdělení období kvůli změně ceníku nebo výměně elektroměru je plně v kompetenci uživatele. K tomuto účelu se u každého záznamu zobrazují editační tlačítka pro rozdělení a sloučení záznamu.

### Ceník
Ceník je důležitou součástí výpočtu v aplikaci. Špatně nastavený ceník může vytvořit velkou chybu ve výpočtech. Proto je vytváření ceníku co nejvíce zautomatizováno. V aplikaci jsou nastaveny regulované ceny od roku 2021. Uživatel zadává pouze neregulovanou cenu, kterou stanovuje jeho dodavatel. Dále se zadává platnost ceníku, zpravidla od 1. ledna do 31. prosince. Datum platnosti je důležité pro použití při hledání ceníku ve výběru. Platnost ceníku se kontroluje s datem měsíčního záznamu a záznamu ve faktuře. Nalezené chyby se zobrazí žlutým vykřičníkem v záhlaví záznamu v měsíčním odečtu nebo faktury. Dalším důležitým parametrem je výběr distribučního území a sazby. Po nastavení těchto dvou údajů a platnosti ceníku se automaticky doplní regulované části ceníku. Případně lze použít tlačítko pro znovu nastavení regulovaných cen. Filtr Postupem času v aplikaci bude velké množství ceníků. K zobrazení pouze relevantních ceníků slouží filtr. Lze ceníky filtrovat podle názvu celého ceníku, názvu jednotlivých sazeb ceníku, sazby, dodavatele, distribučního území a data platnosti. U data platnosti, pokud je vybráno pouze počáteční datum, zobrazí se jen ty ceníky, které přesně odpovídají zvolenému datu. Pokud je vybrán i druhý datum platnosti, zobrazí se všechny ceníky, které odpovídají datu platnosti od a do. Tímto parametrem lze docílit zobrazení ceníku s platností přes dva a více let. Pro snadné hledání ceníků lze aplikovat filtr podle jednotlivých parametrů.
<p align="center">
  <img src="/cena_1.png" width="300" />
  <img src="/cena_2.png" width="300" /> 
  <img src="/cena_3.png" width="300" /> 
</p>

### Porovnání ceníků
Výhodnost jednotlivých ceníků nemusí být vždy na první pohled zřejmá. Proto aplikace umožňuje porovnání dvou ceníků mezi sebou. Porovnání probíhá ve formě výpočtu, kdy se zpravidla zadává do parametrů 12 měsíců a přibližná roční spotřeba ve vysokém (VT) a nízkém tarifu (NT). V porovnání se zobrazují rozdíly v jednotlivých částech a také v celkovém výpočtu.
<p align="center">
  <img src="/por_1.png" width="300" /> 
</p>

### Čas HDO
V aplikaci lze snadno uložit časy NT. Pokud je elektroměr vybaven spínačem HDO, lze podle kodu načíst jednotlivé časy NT a uložit je v aplikaci. V horní části je čas elektroměru. Pokud elektroměr není vybaven přijímačem HDO je možné pomocí tlačítek pod ním čas upravit, aby se shodoval s časem nastaveným na elektroměru. V seznamu se nachází jednotlivé časové intervaly.  Pokud elektroměr disponuje přijímačem HDO je možné podle kódu najít příslušné časy NT. Nalezené seznamy časů je možné uložit do paměti. Touto akcí se přepíší původní uložené časy. Nebo seznam časů uložit v textové podobě do schránky např. pro uložení v některé textové aplikaci.
<p align="center">
  <img src="/hdo_1.jpg" width="300" />
  <img src="/hdo_2.jpg" width="300" /> 
  <img src="/hdo_3.jpg" width="300" /> 
</p>

### Statistika spotřeby
Historickou spotřebu lze zobrazit ve sloupcovém nebo čárovém grafu, a to v ročních nebo měsíčních intervalech. Případně je možné porovnávat spotřebu mezi stejnými měsíci. **Prvním tlačítkem se lze přepínat mezi sloupcovým a čárovým grafem. Ikona tlačítka se mění podle zobrazeného druhu grafu. Druhým tlačítkem od shora se mění interval zobrazených dat. Podle druhu intervalu se také mění ikona tlačítka. Při kliknutí se mění interval spotřeby v tomto pořadí**: měsíční spotřeba → roční spotřeba → spotřeba v jednotlivých měsících (měsíce se mění tlačítky s ikonou šipky). Tlačítky „+“ a „-“ lze měnit hustotu vodorovných os pro zobrazení spotřeby.
<p align="center">
  <img src="/graf_1.jpg" width="600" /> 
</p>

### Záloha, Import ceníků, Export ceníků
Záloha dat je nedílnou a důležitou součástí každé aplikace. V této části lze obnovit data z vybraného záložního souboru nebo staré záložní soubory smazat. Pro bezpečné zálohování je vhodné ukládat záložní soubory na jiných místech než na telefonu/tabletu. Pro přenos záložních souborů do počítače nebo cloudu použijte kabel na propojení a přenos souborů s počítačem nebo synchronizační aplikaci dané cloudové služby. Záloha uloží celou vnitřní databázi měsíčních záznamů, ceníků a faktur. Export/import ceníků slouží pro rychlé vyexportování/importování jednotlivých ceníků z nebo do aplikace.

### Přehled
Zde najdeme celkový výpočet ceny spotřeby, zaplacených záloh a rozdílu mezi těmito částkami. Údaje o času NT a aktuální poslední záznam stavu elektroměru jsou rovněž k dispozici. Pomocí šipek je možné přepínat mezi jednotlivými odběrnými místy. Dolní seznam zobrazuje historickou spotřebu ve formě již obdržených faktur.
<p align="center">
  <img src="/prehled_1.jpg" width="300" />
</p>
