package cz.xlisto.elektrodroid.modules.pricelist;


import java.io.Serializable;


/**
 * Kontejner vstupních parametrů pro porovnání ceníků.
 * Uchovává spotřebu, období, parametry jističe a další služby pro obě strany výpočtu.
 */
public record ConsuptionContainer(double vt, double nt, double month, double phaze, double power,
                                  double servicesL, double servicesR) implements Serializable {

    /**
     * Vytvoří kontejner se všemi explicitně zadanými hodnotami.
     */
    public ConsuptionContainer {
    }


    /**
     * Vytvoří kontejner s výchozími hodnotami pro porovnání ceníků.
     *
     * <p>Výchozí hodnoty: VT=1, NT=2, měsíc=12, fáze=3, příkon=25,
     * další služby vlevo/vpravo=0.</p>
     */
    public ConsuptionContainer() {
        this(1, 2, 12, 3, 25, 0, 0);
    }

}

