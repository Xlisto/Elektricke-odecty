# Elektrické Odečty - Aplikace Pro Android

## O aplikaci
Elektrické odečty Aplikace slouží k průběžnému zaznamenávání stavu elektroměru. Průběžně propočítává cenu spotřebované elektrické energie a porovnává ji se zaplacenými zálohami. Aplikace je navržena pro zařízení s operačním systémem Android, minimální verze SDK je 23 (Android 6.0, Marshmallow). Umožňuje zaznamenávat vystavené faktury za vyúčtování. Na základě stavu posledního záznamu na elektroměru a poslední obdržené vyúčtovací faktury dopočítává aktuální cenu spotřebované elektrické energie a zobrazí stav jestli vzniká přeplatek nebo nedoplatek.

### Odběrná místa
V aplikaci je možné vést evidenci několika odběrných míst. Vedení záznamů a další akce v aplikaci se vždy vztahují k aktuálně zvolenému místu. Pokud je odběrné místo odstraněno, jsou současně odstraněny všechny měsíční záznamy a faktury. Výjimkou je přehled. Pokud je uloženo více odběrných míst, lze pomocí šipek mezi nimi přepínat.
<p align="center">
  <img src="/odb_1.png" width="300" />
  <img src="/odb_2.png" width="300" />
</p>


### Ceník
Ceník je důležitou součástí výpočtu v aplikaci. Nesprávně nastavený ceník může způsobit výraznou odchylku ve výpočtech, proto je jeho vytvoření v aplikaci co nejvíce zjednodušeno.

V seznamu ceníků se zobrazuje název produktové řady, období platnosti, název produktu, distribuční sazba, distribuční území a dodavatel. Ve spodní části položky je uvedena cena za 1 kWh ve vysokém tarifu, cena za 1 kWh v nízkém tarifu a stálá měsíční platba.

#### Nový ceník
Nový ceník se přidává tlačítkem **+**. Při vytváření ceníku se zadává:

- název produktové řady,
- název produktu nebo sazby,
- dodavatel,
- distribuční sazba,
- distribuční území,
- období platnosti ceníku,
- celková cena za 1 kWh ve vysokém tarifu,
- celková cena za 1 kWh v nízkém tarifu,
- stálá měsíční platba.

Cena za 1 kWh se zadává jako celková cena včetně DPH, tedy jako součet obchodní části, regulovaných položek a daně. Stálá měsíční platba se zadává také jako celková částka včetně DPH. Zahrnuje stálý plat dodavatele, cenu za jistič a cenu za provoz nesíťové infrastruktury.

U jednotarifních sazeb, například D01d nebo D02d, se nízký tarif nepoužívá. Položku nízkého tarifu tedy není nutné vyplňovat podle samostatné ceny NT.

Aplikace obsahuje přednastavené regulované ceny od roku 2021 pro všechna tři distribuční území. Po výběru distribučního území, distribuční sazby a období platnosti se automaticky doplní regulované části ceníku. Uživatel proto obvykle zadává hlavně ceny podle svého dodavatele a výsledné hodnoty uvedené v ceníku.

Níže ve formuláři jsou zobrazeny regulované položky ceníku. V případě potřeby je lze upravit ručně, běžně to ale není doporučeno. Pokud je regulovaná cena změněna ručně, aplikace ji zvýrazní. Pokud v přednastavených regulovaných cenách najdete chybu, můžete ji nahlásit na e-mail **x.listo@seznam.cz**. Tlačítkem Uložit se změny uloží, Tlačítkem Zpět se veškteré změny zahodí a zobrazí se seznam ceníků.

Datum platnosti je důležité pro správné přiřazení ceníku k měsíčnímu odečtu nebo faktuře. Platnost ceníku se kontroluje proti datu záznamu. Pokud aplikace najde nesoulad, zobrazí v záhlaví měsíčního záznamu nebo faktury upozornění.

<p align="center">
  <img src="screenshots/cena_4.png" width="300" />
  <img src="screenshots/cena_5.png" width="300" />
</p>

<p align="center">
  <img src="screenshots/cena_1.png" width="200" />
  <img src="screenshots/cena_2.png" width="200" />
  <img src="screenshots/cena_3.png" width="200" />
</p>

#### Úprava ceníků
Kliknutím na položku ceníku se zobrazí tři tlačítka Uprav, Detail a Smaž. Tlačítkem Uprav se zobrazí podobný jako pro přidání nového ceníku. Rozdíl je v již předvyplněných políčkách, které je možné podle potřeby změmnit. Je zlde tlačítko Načíst regulované platby. To je pro případ, pokud by se z nějakého důvodu nenačetlui regulované platby při změněn spinnerů vybírající distribuční území nebo sazbu. Tlačítkem Uložit se změny uloží, Tlačítkem Zpět se veškteré změny zahodí a zobrazí se seznam ceníků.

### Měsíční záznamy
Měsíční záznamy slouží k pravidelnému zapisování stavů elektroměru a k průběžnému výpočtu ceny spotřebované elektrické energie. Každý záznam se vztahuje k aktuálně vybranému odběrnému místu.

V seznamu měsíčních odečtů se zobrazují jednotlivé měsíce, spotřeba ve vysokém tarifu (VT), spotřeba v nízkém tarifu (NT), orientační cena spotřebované elektřiny, zaplacená záloha a rozdíl mezi vypočtenou cenou a zaplacenou zálohou. Záznam je barevně odlišen podle výsledku. Zelená barva označuje stav, kdy zaplacená záloha převyšuje vypočtenou spotřebu. Červená barva upozorňuje na stav, kdy vypočtená spotřeba převyšuje zaplacenou zálohu.

V horní části obrazovky je možné přepínat režim **Zkrácený**. Ve zkráceném zobrazení se zobrazují pouze nejdůležitější údaje. Po vypnutí zkráceného zobrazení se zobrazí podrobnější informace, například použitý ceník, jeho platnost, výše zálohy a grafická indikace stavu záloh.

Přepínač **Zastropované ceny v r. 2023** slouží pro přepočet záznamů podle pravidel platných pro rok 2023. U běžných novějších záznamů zůstává vypnutý.

Pravidelným používáním aplikace vzniká delší seznam odečtů. Pro lepší orientaci lze použít tlačítko **Filtr odečtů**. Filtr umožňuje zobrazit pouze záznamy v určitém období. Nastavuje se počáteční datum v části **Měsíční odečty OD** a koncové datum v části **Měsíční odečty DO**. Tlačítkem **Filtrovat** se filtr použije, tlačítkem **Vynulovat filtr** se omezení zruší a znovu se zobrazí všechny záznamy. Tlačítkem **Zrušit** se dialog zavře bez použití změn.

<p align="center">
  <img src="screenshots/mes_1.png" width="200" />
  <img src="screenshots/mes_2.png" width="200" /> 
  <img src="screenshots/mes_3.png" width="200" /> 
</p>

#### Přidání měsíčního záznamu
Nový měsíční záznam se přidává tlačítkem **+** v pravé dolní části obrazovky. Otevře se formulář pro zadání odečtu.

U prvního měsíčního záznamu je nutné zadat počáteční stav elektroměru. Tento první záznam slouží jako výchozí hodnota, od které se budou počítat další měsíční spotřeby. Datum záznamu se nastavuje tlačítkem **Datum**. Po jeho stisknutí se otevře kalendář, ve kterém vyberete den odečtu.

Do formuláře se zadává:

- datum odečtu,
- stav počitadla vysokého tarifu,
- stav počitadla nízkého tarifu,
- pravidelná měsíční platba neboli záloha (při prvním záznamu a typu záznamu Výměna elektroměru tato položka není dostupná).

Volba **Další údaje** zobrazí doplňková pole. Do poznámky lze zapsat vlastní informaci k odečtu. Položka **Další doplňkové služby nebo slevy** slouží pro jednorázové částky, které mají být připočteny nebo odečteny od výsledku za daný měsíc.

Tlačítkem **Uložit** se záznam uloží. Tlačítkem **Zpět** se formulář zavře bez uložení změn.

Při ukládání prvních měsíčních záznamů se může zobrazit upozornění na chybějící poslední fakturu. Toto upozornění souvisí s přesnějším navázáním měsíčních odečtů na fakturaci. Pokud aplikaci teprve začínáte používat a fakturu ještě nemáte zadanou, lze upozornění potvrdit a v zadávání pokračovat.

<p align="center">
  <img src="screenshots/mes_4.png" width="200" />
  <img src="screenshots/mes_5.png" width="200" /> 
  <img src="screenshots/mes_6.png" width="200" />
</p>

#### Další měsíční záznamy
Při zadávání dalších měsíců se postup opakuje. Tlačítkem **+** otevřete nový záznam, nastavíte datum odečtu, zadáte aktuální stavy elektroměru a doplníte výši měsíční zálohy.

Aplikace porovná nový stav elektroměru s předchozím uloženým záznamem. Z rozdílu stavů vypočítá spotřebu ve VT a NT a podle vybraného ceníku dopočítá orientační cenu spotřebované elektřiny.

Ve spodní části formuláře jsou dostupné další volby:

- **Přidat zálohu do seznamu plateb Fakturace** – zadaná měsíční záloha se zároveň zapíše do plateb ve fakturaci, aby ji nebylo nutné doplňovat ručně.
- **Vytvořit záložní zip soubor při uložení odečtu** – při uložení záznamu se automaticky vytvoří lokální záloha dat.
- **Výměna elektroměru** – tato volba se používá při výměně elektroměru. Do záznamu se zadá konečný stav starého elektroměru, aby bylo možné správně navázat další odečty.

#### Výběr ceníku pro měsíční záznam
Každý měsíční záznam musí mít přiřazený ceník, podle kterého se provede výpočet ceny. Pokud již byl v předchozím záznamu ceník použit, aplikace jej při dalším odečtu nabídne automaticky.

Ceník lze změnit tlačítkem pro výběr ceníku. Po jeho stisknutí se zobrazí seznam dostupných ceníků. Vybraný ceník označíte klepnutím na položku a potvrdíte tlačítkem **Vybrat**.

Pokud je v aplikaci uloženo větší množství ceníků, je vhodné použít [filtraci ceníků](#filtrace-ceníků). Pomocí filtru lze rychle zobrazit pouze ceníky odpovídající požadované distribuční sazbě, dodavateli, distribučnímu území nebo datu platnosti.

<p align="center">
  <img src="screenshots/mes_7.png" width="200" />
  <img src="screenshots/mes_8.png" width="200" /> 
  <img src="screenshots/mes_9.png" width="200" />
</p>



### Záznam faktur
Záznam faktur je podobný měsíčním záznamům. Faktura se zobrazuje ve třech záložkách. **První záložka „Faktura“ obsahuje vlastní záznamy odečtů (minimálně jeden). Každý záznam má přidělený svůj ceník, podle kterého se počítá cena spotřebované elektrické energie. Pokud je záznamů více, celkový součet faktury se zobrazuje v dolní liště.**
Tapnutí na spodní sumu součtů zobrazí panel se všemi dostupnými součty, lze vybrat položku, která se bude zobrazovat ve spodní části obrazovky:
→ Celková spotřeba VT
→ Celková spotřeba NT
→ Celková spotřeba VT a NT
→ Cena VT bez DPH
→ Cena NT bez DPH
→ Cena stálých platů bez DPH
→ Cena na podporu obnovitelných zdrojů (POZE)
→ Celková cena bez DPH
→ Celková cena s DPH
→ Zaplacené zálohy
→ Bilance (rozdíl mezi celkovou cenou a zaplacenými zálohami).
Ve spodní části se též zobrazuje rychlá informace o aktuálním stavu poměru zaplacených záloh k aktuální spotřebě. Zeleně je označena, pokud se jedná přeplatek. Červeně, pokud vzniká nedoplatek
Druhá záložka „Detaily“ zobrazuje dílčí záznamy výpočtů faktury. Tato část se může využít pro kontrolu s obdrženou fakturou od vašeho dodavatele. Zde lze nalézt, v jakých částech se nachází případná odchylka. Třetí záložka „Platby“ zaznamenává všechny zaplacené zálohy (případně slevy).
<p align="center">
  <img src="/fak_1.png" width="200" />
  <img src="/fak_2.png" width="200" /> 
  <img src="/fak_3.png" width="200" /> 
  <img src="/fak_4.png" width="200" /> 
</p>

V seznamu faktur se jako první záznam nachází „Období bez faktury“. Tato část automaticky vypočítává cenu spotřebované elektrické energie mezi posledním záznamem poslední faktury a posledním měsíčním odečtem.

**Automatický výpočet lze nastavit do dvou režimů**: Plně automatický režim (výchozí nastavení): Plně kopíruje záznamy v měsíčních odečtech, včetně nastavení ceníku a výměny elektroměru. Editační tlačítka pro rozdělení a sloučení záznamů jsou v tomto režimu skryta.

**Poloautomatický režim**: Z měsíčních odečtů přebírá pouze poslední záznam stavu elektroměru. Ostatní rozdělení období kvůli změně ceníku nebo výměně elektroměru je plně v kompetenci uživatele. K tomuto účelu se u každého záznamu zobrazují editační tlačítka pro rozdělení a sloučení záznamu.

### Filtrace ceníků
Postupem času může být v aplikaci uloženo větší množství ceníků. Filtrace ceníků slouží k rychlému vyhledání pouze těch ceníků, které odpovídají konkrétnímu odběrnému místu, distribuční sazbě, dodavateli nebo období platnosti.

Filtr ceníků lze otevřít ze seznamu ceníků pomocí položky **Filtr ceníků** v horní liště. Stejný filtr se používá také při výběru ceníku pro měsíční odečet. Pokud se při zadávání měsíčního záznamu zobrazí příliš mnoho ceníků, je vhodné filtr použít a zúžit seznam pouze na relevantní položky.

V dialogu filtru lze nastavit více podmínek současně:

- **Produktová řada** – název celého ceníku nebo skupiny ceníků.
- **Produkt** – název konkrétního produktu nebo sazby v rámci ceníku.
- **Sazba dist.** – distribuční sazba, například D01d, D02d, D25d nebo D26d.
- **Dodavatel** – dodavatel elektřiny.
- **Distribuční území** – distribuční oblast, například ČEZ Distribuce, EG.D nebo PREdistribuce.
- **Datum platnosti** – období, ve kterém má být ceník platný.

U položky **Datum platnosti** lze použít pouze počáteční datum, nebo počáteční i koncové datum. Pokud je nastaveno pouze počáteční datum a druhé pole zůstane na hodnotě **[VŠE]**, filtr vyhledá ceníky, které začínají přesně zvoleným datem. Pokud je nastaveno i koncové datum, zobrazí se ceníky odpovídající celému zadanému období platnosti.

Toto nastavení je užitečné například v situaci, kdy ceník neplatí celý kalendářní rok, ale pouze část roku. Pomocí filtru lze vyhledat jak ceník končící v průběhu roku, tak navazující ceník začínající od dalšího období.

Tlačítkem **Nastav filtr** se zvolené podmínky použijí. Tlačítkem **Resetovat všechny filtry** se filtr vrátí do výchozího stavu. Tlačítkem **Zrušit** se dialog zavře bez použití změn.

Po použití filtru se ve spodní části seznamu zobrazí počet uložených a aktuálně zobrazených ceníků. Například údaj **254/1** znamená, že v aplikaci je uloženo 254 ceníků, ale podle aktuálního filtru je zobrazen pouze jeden z nich.

Ceník se vybere označením příslušné položky v seznamu a potvrzením tlačítkem **Vybrat**. Po návratu do měsíčního záznamu se vybraný ceník zobrazí ve formuláři odečtu a použije se pro výpočet spotřeby.

<p align="center">
  <img src="screenshots/mes_10.png" width="200" />
  <img src="screenshots/mes_11.png" width="200" /> 
</p>

### Porovnání ceníků
Výhodnost jednotlivých ceníků nemusí být vždy na první pohled zřejmá. Proto aplikace umožňuje porovnání dvou ceníků mezi sebou. Porovnání probíhá ve formě výpočtu, kdy se zpravidla zadává do parametrů 12 měsíců a přibližná roční spotřeba ve vysokém (VT) a nízkém tarifu (NT). V porovnání se zobrazují rozdíly v jednotlivých částech a také v celkovém výpočtu.
<p align="center">
  <img src="/por_1.png" width="300" /> 
</p>

### Čas HDO
V aplikaci lze snadno uložit časy NT a zobrazovat notifikace na začátek a konec. Jednotlivé časy lze vložit či upravovat ručně. Nebo pokud je elektroměr vybaven spínačem HDO, lze podle kodu načíst jednotlivé časy NT a uložit je v aplikaci. V horní části je čas elektroměru. Pokud elektroměr není vybaven přijímačem HDO a synchronizací času. Jeho čas není synchronizován, je možné pomocí tlačítek pod ním čas upravit, aby se shodoval s časem nastaveným na elektroměru. V seznamu se nachází jednotlivé časové intervaly.
Nalezené seznamy časů je možné uložit do paměti. Touto akcí se přepíší původní uložené časy. Nebo seznam časů uložit v textové podobě do schránky např. pro uložení v některé textové aplikaci.
<p align="center">
  <img src="/hdo_1.jpg" width="200" />
  <img src="/hdo_2.jpg" width="200" /> 
  <img src="/hdo_3.jpg" width="200" /> 
</p>

### Statistika spotřeby
Historickou spotřebu lze zobrazit ve sloupcovém nebo čárovém grafu, a to v ročních nebo měsíčních intervalech. Případně je možné porovnávat spotřebu mezi stejnými měsíci. **Prvním tlačítkem se lze přepínat mezi sloupcovým a čárovým grafem. Ikona tlačítka se mění podle zobrazeného druhu grafu. Druhým tlačítkem od shora se mění interval zobrazených dat. Podle druhu intervalu se také mění ikona tlačítka. Při kliknutí se mění interval spotřeby v tomto pořadí**: měsíční spotřeba → roční spotřeba → spotřeba v jednotlivých měsících (měsíce se mění tlačítky s ikonou šipky). Tlačítky „+“ a „-“ lze měnit hustotu vodorovných os pro zobrazení spotřeby.
<p align="center">
  <img src="/graf_1.jpg" width="600" /> 
</p>

### Záloha lokální
Záloha dat je nedílnou a důležitou součástí každé aplikace. V této části lze obnovit data z vybraného záložního souboru nebo staré záložní soubory smazat. Pro bezpečné zálohování je vhodné ukládat záložní soubory na jiných místech než na telefonu/tabletu. Zálohování do lokálního uložiště vyžaduje oprávnění pro přístup do vámi vybrané složky. Tento přístup se uděluje jen jen jednou, případně při změně složky a je společný pro funkci exportu ceníků. Dialog pro výběr složky se zobrazí kliknutím na ikonu složky, výběrem složky a následně stisknutím tlačítka pro povolení přístupu.
Zálohu se vytvoří stiskem ikony aktovky se šipkou dolu. Záložní soubor lze také nechat vytvořit automaticky při každém zadání měsičního odečtu zatržením příslušného chcekboxu. Krátké kliknutí na položku zálohy se zobrazí tlačítka pro obnovení zálohy, odeslání do GoogleDrive a smazání. Pokud cheme pracovat s více soubory. Dlouhým podržením kterékoliv položky se aktivuje multivýběr. Pomocí chceckboxu vybereme potřebné zálohy.  V menu jsou položky pro smazání záloh nebo odeslání na GoogleDrive.
Pro přenos záložních souborů do počítače nebo cloudu použijte kabel na propojení a přenos souborů s počítačem nebo synchronizační aplikaci dané cloudové služby. Záloha uloží celou vnitřní databázi měsíčních záznamů, ceníků a faktur.  
Pokud zvolíte odeslání do GoogleDrive. Jeden nebo vybraná skupina záloh se začne odesílat na váš GoogleDrive (pokud jste přihlášeni a udělili jste oprávnění).Pokud není dostupné internetové připojení, úloha odeslání počká, až se budete nacházet v oblasti s mobilním/wifi připojením.
<p align="center">
  <img src="/zal_1.jpg" width="200" />
  <img src="/zal_2.jpg" width="200" /> 
  <img src="/zal_3.jpg" width="200" /> 
</p>

### Záloha Google
Slouží k zobrazení záloh uložení na vašem GoogleDrive v systémové části. Pro povolení ukládání do GoogleDrive uložiště je vyžadování přihlášení vaším Google účtem a udělení oprávnění aplikace pro přístuo do GoogleDrive. appdata - prostor pro ukládání systémových souboru. Funkce jsou podobné jako v Lokální záloze. Krátké kliknutí na položku se zobrazí tlačítka pro obnovení zálohy, uložení záložního souboru do lokálního uložiště a smazání. Dlouhým kliknutím na jakoukoliv položku lze aktivovat multivýběr. V menu vybrat pro uložení záloh do lokalní uložiště nebo smazání.
<p align="center">
  <img src="/gog_1.jpg" width="200" />
  <img src="/gog_2.jpg" width="200" /> 
  <img src="/gog_3.jpg" width="200" /> 
</p>

### Export ceníků
Export ceníků slouží pro rychlé vyexportování jednotlivých ceníků z aplikace. Tato část vyžaduje přístup do lokální složky, která je společná se složkou pro ukládání záloh. Ceníky se ukládájí ve formátu JSON. Seznam obsahuje seznam všech ceníku uložených v aplikaci a seskupené podle názvu. Tzn jeden ceník může obsahovat až 10 distribučních sazeb (D01d, D02d, D25a, atd.). Kliknutím na položku ceníku a potvrzení dialogu se ceník uloží dovámy vybrané složky a lze jej přenést do jiné aplikace na jiném zařízení.
<p align="center">
  <img src="/exp_1.jpg" width="300" />
</p>

### Import ceníků
Import ceníků slouží pro rychlé nahrání ceníku (může obsahovat až 10 sazeb jako jsou D01a, D02a, D26a, atd.). Ceník přidáte krátkým kliknutím na položku. V zobrazeném dialogu si yvberete distribuční sazby, kteréchcte přidat (pokud jich je více) a stislnutík tlačítka OK se přidají do aplikace. Pokud Ceník se stejným názvem již v aplikace existuje, budete dotázání zdali jej přepsat či nikoliv.
<p align="center">
  <img src="/imp_1.jpg" width="300" />
</p>

### Přehled
Zde najdeme celkový výpočet ceny spotřeby, zaplacených záloh a rozdílu mezi těmito částkami. Údaje o času NT a aktuální poslední záznam stavu elektroměru jsou rovněž k dispozici. Pomocí šipek je možné přepínat mezi jednotlivými odběrnými místy. Dolní seznam zobrazuje historickou spotřebu ve formě již obdržených faktur.
<p align="center">
  <img src="/prehled_1.jpg" width="300" />
</p>
