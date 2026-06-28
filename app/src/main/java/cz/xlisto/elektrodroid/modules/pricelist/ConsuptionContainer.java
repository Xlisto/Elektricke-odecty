package cz.xlisto.elektrodroid.modules.pricelist;


import java.io.Serializable;


/**
 * Kontejner vstupních parametrů pro porovnání ceníků.
 * Uchovává spotřebu, období, parametry jističe a další služby pro obě strany výpočtu.
 */
public class ConsuptionContainer implements Serializable {

    public double vt;
    public double nt;
    public double month;
    public double phaze;
    public double power;
    public double servicesL;
    public double servicesR;


    /**
     * Vytvoří kontejner se všemi explicitně zadanými hodnotami.
     */
    public ConsuptionContainer(double vt, double nt, double month, double phaze, double power, double servicesL, double servicesR) {
        this.vt = vt;
        this.nt = nt;
        this.month = month;
        this.phaze = phaze;
        this.power = power;
        this.servicesL = servicesL;
        this.servicesR = servicesR;
    }


    /**
     * Vytvoří kontejner s výchozími hodnotami pro porovnání ceníků.
     *
     * <p>Výchozí hodnoty: VT=1, NT=2, měsíc=12, fáze=3, příkon=25,
     * další služby vlevo/vpravo=0.</p>
     */
    public ConsuptionContainer() {
        this.vt = 1;
        this.nt = 2;
        this.month = 12;
        this.phaze = 3;
        this.power = 25;
        this.servicesL = 0;
        this.servicesR = 0;
    }

}

