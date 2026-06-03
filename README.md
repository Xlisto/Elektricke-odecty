# Elektrické odečty - aplikace pro Android

## O aplikaci
Elektrické odečty jsou aplikace pro průběžné sledování spotřeby elektrické energie. Umožňují zapisovat stavy elektroměru, evidovat měsíční odečty, porovnávat odhadovanou cenu spotřebované elektřiny se zaplacenými zálohami a ukládat údaje z vyúčtovacích faktur.

Aplikace je určena pro zařízení s operačním systémem Android. Minimální podporovaná verze je Android 6.0 (Marshmallow, SDK 23).

Hlavním účelem aplikace je poskytnout uživateli průběžný přehled o tom, zda zaplacené zálohy přibližně odpovídají skutečné spotřebě. Po zadání stavu elektroměru, ceníku a údajů z faktur aplikace dopočítává orientační cenu spotřebované elektrické energie. Díky tomu lze včas odhadnout, zda při dalším vyúčtování pravděpodobně vznikne přeplatek, nebo nedoplatek.

Aplikace pracuje s vysokým tarifem (VT), nízkým tarifem (NT), měsíčními zálohami, ceníky, fakturami, stálými platbami a dalšími položkami, které se podílejí na výsledné ceně elektřiny. Výpočty slouží jako uživatelská kontrola a orientační přehled. Skutečné vyúčtování je vždy určeno fakturou dodavatele elektrické energie.

## Odběrná místa
Odběrné místo představuje konkrétní místo spotřeby elektřiny, například byt, dům, garáž nebo chatu. V aplikaci lze vést více odběrných míst současně. Veškeré měsíční odečty, faktury, použité ceníky a další záznamy se vždy vztahují k aktuálně vybranému odběrnému místu.

Aktuální odběrné místo se vybírá v horní části obrazovky pomocí rozbalovacího seznamu. Pod ním se zobrazují základní údaje vybraného místa, například poznámka, hodnota hlavního jističe, číslo elektroměru a číslo odběrného místa. Tyto údaje pomáhají odlišit jednotlivá místa a zároveň slouží jako podklad pro některé výpočty, zejména u hodnot souvisejících s hlavním jističem.

Nové odběrné místo lze přidat pomocí tlačítka **+** v pravé dolní části obrazovky. Stávající odběrné místo lze upravit tlačítkem **Upravit odběrné místo**. Při úpravě se zadává název odběrného místa, poznámka, počet fází, hodnota jističe, číslo elektroměru a číslo odběrného místa.

Odběrné místo lze také odstranit tlačítkem **Smazat odběrné místo**. Tuto akci je vhodné používat opatrně, protože odstraněním odběrného místa se smažou také záznamy, které k němu patří, například měsíční odečty a faktury.

<p align="center">
  <img src="screenshots/odb_1.png" width="300" />
  <img src="screenshots/odb_2.png" width="300" />
</p>

## Ceník
Ceník je důležitou součástí výpočtu v aplikaci. Nesprávně nastavený ceník může způsobit výraznou odchylku ve výpočtech, proto je jeho vytvoření v aplikaci co nejvíce zjednodušeno.

V seznamu ceníků se zobrazuje název produktové řady, období platnosti, název produktu, distribuční sazba, distribuční území a dodavatel. Ve spodní části položky je uvedena cena za 1 kWh ve vysokém tarifu, cena za 1 kWh v nízkém tarifu a stálá měsíční platba.

### Nový ceník
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

Níže ve formuláři jsou zobrazeny regulované položky ceníku. V případě potřeby je lze upravit ručně, běžně to ale není doporučeno. Pokud je regulovaná cena změněna ručně, aplikace ji zvýrazní. Pokud v přednastavených regulovaných cenách najdete chybu, můžete ji nahlásit na e-mail **x.listo@seznam.cz**.

Datum platnosti je důležité pro správné přiřazení ceníku k měsíčnímu odečtu nebo faktuře. Platnost ceníku se kontroluje proti datu záznamu. Pokud aplikace najde nesoulad, zobrazí v záhlaví měsíčního záznamu nebo faktury upozornění.

Tlačítkem **Uložit** se ceník uloží. Tlačítkem **Zpět** se provedené změny zahodí a zobrazí se seznam ceníků.

<p align="center">
  <img src="screenshots/cena_4.png" width="300" />
  <img src="screenshots/cena_5.png" width="300" />
</p>

<p align="center">
  <img src="screenshots/cena_1.png" width="200" />
  <img src="screenshots/cena_2.png" width="200" />
  <img src="screenshots/cena_3.png" width="200" />
</p>

### Úprava ceníků
Klepnutím na položku ceníku v seznamu se zobrazí dostupné akce **Uprav**, **Detail** a **Smaž**.

Tlačítkem **Uprav** se otevře formulář podobný formuláři pro přidání nového ceníku. Rozdíl je v tom, že jednotlivá pole jsou již předvyplněna údaji vybraného ceníku. Tyto hodnoty lze podle potřeby změnit a uložit.

Ve formuláři je také tlačítko **Načíst regulované platby**. Slouží pro opětovné načtení regulovaných položek podle zvoleného distribučního území, distribuční sazby a období platnosti. To se hodí například v případě, kdy se regulované položky po změně výběru distribučního území nebo sazby z nějakého důvodu nenačtou automaticky.

Tlačítkem **Uložit** se změny uloží. Tlačítkem **Zpět** se provedené změny zahodí a zobrazí se seznam ceníků.

Tlačítkem **Detail** lze zobrazit podrobnosti vybraného ceníku bez úprav. Tlačítkem **Smaž** lze ceník odstranit. Mazání je vhodné používat opatrně, zejména pokud je ceník již použitý u měsíčních odečtů nebo faktur.

## Měsíční záznamy
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

### Přidání měsíčního záznamu
Nový měsíční záznam se přidává tlačítkem **+** v pravé dolní části obrazovky. Otevře se formulář pro zadání odečtu.

U prvního měsíčního záznamu je nutné zadat počáteční stav elektroměru. Tento první záznam slouží jako výchozí hodnota, od které se budou počítat další měsíční spotřeby. Datum záznamu se nastavuje tlačítkem **Datum**. Po jeho stisknutí se otevře kalendář, ve kterém vyberete den odečtu.

Do formuláře se zadává:

- datum odečtu,
- stav počitadla vysokého tarifu,
- stav počitadla nízkého tarifu,
- pravidelná měsíční platba neboli záloha.

Při prvním záznamu a při záznamu typu **Výměna elektroměru** není položka měsíční zálohy dostupná.

Volba **Další údaje** zobrazí doplňková pole. Do poznámky lze zapsat vlastní informaci k odečtu. Položka **Další doplňkové služby nebo slevy** slouží pro jednorázové částky, které mají být připočteny nebo odečteny od výsledku za daný měsíc.

Tlačítkem **Uložit** se záznam uloží. Tlačítkem **Zpět** se formulář zavře bez uložení změn.

Při ukládání prvních měsíčních záznamů se může zobrazit upozornění na chybějící poslední fakturu. Toto upozornění souvisí s přesnějším navázáním měsíčních odečtů na fakturaci. Pokud aplikaci teprve začínáte používat a fakturu ještě nemáte zadanou, lze upozornění potvrdit a v zadávání pokračovat.

<p align="center">
  <img src="screenshots/mes_4.png" width="200" />
  <img src="screenshots/mes_5.png" width="200" />
  <img src="screenshots/mes_6.png" width="200" />
</p>

### Další měsíční záznamy
Při zadávání dalších měsíců se postup opakuje. Tlačítkem **+** otevřete nový záznam, nastavíte datum odečtu, zadáte aktuální stavy elektroměru a doplníte výši měsíční zálohy.

Aplikace porovná nový stav elektroměru s předchozím uloženým záznamem. Z rozdílu stavů vypočítá spotřebu ve VT a NT a podle vybraného ceníku dopočítá orientační cenu spotřebované elektřiny.

Ve spodní části formuláře jsou dostupné další volby:

- **Přidat zálohu do seznamu plateb Fakturace** – zadaná měsíční záloha se zároveň zapíše do plateb ve fakturaci, aby ji nebylo nutné doplňovat ručně.
- **Vytvořit záložní zip soubor při uložení odečtu** – při uložení záznamu se automaticky vytvoří lokální záloha dat.
- **Výměna elektroměru** – tato volba se používá při výměně elektroměru. Do záznamu se zadá konečný stav starého elektroměru, aby bylo možné správně navázat další odečty.

### Výběr ceníku pro měsíční záznam
Každý měsíční záznam musí mít přiřazený ceník, podle kterého se provede výpočet ceny. Pokud již byl v předchozím záznamu ceník použit, aplikace jej při dalším odečtu nabídne automaticky.

Ceník lze změnit tlačítkem pro výběr ceníku. Po jeho stisknutí se zobrazí seznam dostupných ceníků. Vybraný ceník označíte klepnutím na položku a potvrdíte tlačítkem **Vybrat**.

Pokud je v aplikaci uloženo větší množství ceníků, je vhodné použít [filtraci ceníků](#filtrace-ceniku). Pomocí filtru lze rychle zobrazit pouze ceníky odpovídající požadované distribuční sazbě, dodavateli, distribučnímu území nebo datu platnosti.

<p align="center">
  <img src="screenshots/mes_7.png" width="200" />
  <img src="screenshots/mes_8.png" width="200" />
  <img src="screenshots/mes_9.png" width="200" />
</p>

## Záznam faktur
Záznam faktur je podobný měsíčním záznamům. Faktura se zobrazuje ve třech záložkách.

První záložka **Faktura** obsahuje vlastní záznamy odečtů. Každý záznam má přiřazený ceník, podle kterého se počítá cena spotřebované elektrické energie. Pokud je záznamů více, celkový součet faktury se zobrazuje v dolní liště.

Klepnutím na spodní součet se zobrazí panel se všemi dostupnými součty. Lze vybrat položku, která se bude zobrazovat ve spodní části obrazovky:

- celková spotřeba VT,
- celková spotřeba NT,
- celková spotřeba VT a NT,
- cena VT bez DPH,
- cena NT bez DPH,
- cena stálých plateb bez DPH,
- cena na podporu obnovitelných zdrojů (POZE),
- celková cena bez DPH,
- celková cena s DPH,
- zaplacené zálohy,
- bilance.

Ve spodní části se zobrazuje také rychlá informace o poměru zaplacených záloh k aktuální spotřebě. Zeleně je označen přeplatek, červeně nedoplatek.

Druhá záložka **Detaily** zobrazuje dílčí výpočty faktury. Tuto část lze využít pro kontrolu proti faktuře od dodavatele a pro hledání případných rozdílů. Třetí záložka **Platby** slouží k evidenci zaplacených záloh nebo slev.

<p align="center">
  <img src="screenshots/fak_1.png" width="200" />
  <img src="screenshots/fak_2.png" width="200" />
  <img src="screenshots/fak_3.png" width="200" />
  <img src="screenshots/fak_4.png" width="200" />
</p>

V seznamu faktur se jako první záznam nachází **Období bez faktury**. Tato část automaticky vypočítává cenu spotřebované elektrické energie mezi posledním záznamem poslední faktury a posledním měsíčním odečtem.

Automatický výpočet lze nastavit do dvou režimů:

- **Plně automatický režim** – výchozí nastavení. Kopíruje záznamy z měsíčních odečtů včetně nastavení ceníku a výměny elektroměru. Editační tlačítka pro rozdělení a sloučení záznamů jsou v tomto režimu skryta.
- **Poloautomatický režim** – z měsíčních odečtů přebírá pouze poslední záznam stavu elektroměru. Ostatní rozdělení období kvůli změně ceníku nebo výměně elektroměru je na uživateli.

<a id="filtrace-ceniku"></a>

## Filtrace ceníků
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

## Porovnání ceníků
Výhodnost jednotlivých ceníků nemusí být vždy na první pohled zřejmá. Proto aplikace umožňuje porovnání dvou ceníků mezi sebou. Porovnání probíhá formou výpočtu, kdy se zpravidla zadává období 12 měsíců a přibližná roční spotřeba ve vysokém (VT) a nízkém tarifu (NT). V porovnání se zobrazují rozdíly v jednotlivých částech i v celkovém výpočtu.

<p align="center">
  <img src="screenshots/por_1.png" width="300" />
</p>

## Čas HDO
V aplikaci lze uložit časy nízkého tarifu (NT) a zobrazovat notifikace na jeho začátek a konec. Jednotlivé časy lze vložit nebo upravit ručně. Pokud je elektroměr vybaven spínačem HDO, lze podle HDO kódu načíst jednotlivé časy NT a uložit je v aplikaci.

V horní části je zobrazen čas elektroměru. Pokud elektroměr není vybaven přijímačem HDO se synchronizací času, jeho čas se nemusí shodovat se skutečným časem. Pomocí tlačítek lze čas upravit tak, aby odpovídal času nastavenému na elektroměru.

Nalezené seznamy časů je možné uložit do paměti aplikace. Touto akcí se přepíší původně uložené časy. Seznam časů lze také zkopírovat v textové podobě do schránky.

<p align="center">
  <img src="screenshots/hdo_1.jpg" width="200" />
  <img src="screenshots/hdo_2.jpg" width="200" />
  <img src="screenshots/hdo_3.jpg" width="200" />
</p>

## Statistika spotřeby
Historickou spotřebu lze zobrazit ve sloupcovém nebo čárovém grafu, a to v ročních nebo měsíčních intervalech. Případně je možné porovnávat spotřebu mezi stejnými měsíci.

Prvním tlačítkem se přepíná mezi sloupcovým a čárovým grafem. Ikona tlačítka se mění podle aktuálně zobrazeného typu grafu. Druhým tlačítkem se mění interval zobrazených dat. Při kliknutí se interval spotřeby mění v pořadí: měsíční spotřeba → roční spotřeba → spotřeba v jednotlivých měsících.

Tlačítky **+** a **-** lze měnit hustotu vodorovných os pro zobrazení spotřeby.

<p align="center">
  <img src="screenshots/graf_1.jpg" width="600" />
</p>

## Záloha lokální
Záloha dat je důležitou součástí aplikace. V této části lze vytvořit záložní soubor, obnovit data z vybrané zálohy nebo staré záložní soubory smazat. Pro bezpečné zálohování je vhodné ukládat záložní soubory i mimo telefon nebo tablet.

Zálohování do lokálního úložiště vyžaduje oprávnění pro přístup do vybrané složky. Tento přístup se uděluje při prvním nastavení složky, případně při její změně. Stejná složka se používá také pro export ceníků.

Záloha se vytvoří stiskem ikony aktovky se šipkou dolů. Záložní soubor lze také vytvářet automaticky při každém zadání měsíčního odečtu, pokud je zapnuta příslušná volba.

Krátkým klepnutím na položku zálohy se zobrazí tlačítka pro obnovení zálohy, odeslání na Google Drive a smazání. Dlouhým podržením položky se aktivuje vícenásobný výběr, pomocí kterého lze pracovat s více zálohami najednou.

Pokud zvolíte odeslání na Google Drive, vybraná záloha nebo skupina záloh se odešle na váš Google Drive, pokud jste přihlášeni a udělili jste aplikaci potřebné oprávnění. Pokud není dostupné internetové připojení, úloha odeslání počká na dostupné mobilní nebo Wi-Fi připojení.

<p align="center">
  <img src="screenshots/zal_1.jpg" width="200" />
  <img src="screenshots/zal_2.jpg" width="200" />
  <img src="screenshots/zal_3.jpg" width="200" />
</p>

## Záloha Google
Tato část slouží k zobrazení záloh uložených v Google Drive v systémové části aplikace. Pro používání záloh na Google Drive je vyžadováno přihlášení Google účtem a udělení oprávnění aplikaci.

Funkce jsou podobné jako u lokální zálohy. Krátkým klepnutím na položku se zobrazí tlačítka pro obnovení zálohy, uložení záložního souboru do lokálního úložiště a smazání. Dlouhým klepnutím lze aktivovat vícenásobný výběr.

<p align="center">
  <img src="screenshots/gog_1.jpg" width="200" />
  <img src="screenshots/gog_2.jpg" width="200" />
  <img src="screenshots/gog_3.jpg" width="200" />
</p>

## Export ceníků
Export ceníků slouží k uložení vybraných ceníků z aplikace do souboru. Tato část vyžaduje přístup do lokální složky, která je společná se složkou pro ukládání záloh.

Ceníky se ukládají ve formátu JSON. Seznam obsahuje ceníky uložené v aplikaci a seskupené podle názvu. Jeden ceník může obsahovat více distribučních sazeb, například D01d, D02d nebo D25d.

Kliknutím na položku ceníku a potvrzením dialogu se ceník uloží do vybrané složky. Takto uložený soubor lze přenést do jiné instalace aplikace na jiném zařízení.

<p align="center">
  <img src="screenshots/exp_1.jpg" width="300" />
</p>

## Import ceníků
Import ceníků slouží k rychlému nahrání ceníku do aplikace. Importovaný ceník může obsahovat více distribučních sazeb.

Ceník přidáte krátkým klepnutím na položku. V zobrazeném dialogu vyberete distribuční sazby, které chcete přidat, a potvrdíte výběr tlačítkem **OK**. Pokud již v aplikaci existuje ceník se stejným názvem, aplikace se zeptá, zda jej chcete přepsat.

<p align="center">
  <img src="screenshots/imp_1.jpg" width="300" />
</p>

## Přehled
Přehled zobrazuje celkový výpočet ceny spotřeby, zaplacených záloh a rozdílu mezi těmito částkami. K dispozici jsou také údaje o časech NT a poslední uložený stav elektroměru.

Pomocí šipek je možné přepínat mezi jednotlivými odběrnými místy. Dolní seznam zobrazuje historickou spotřebu podle již uložených faktur.

<p align="center">
  <img src="screenshots/prehled_1.jpg" width="300" />
</p>
