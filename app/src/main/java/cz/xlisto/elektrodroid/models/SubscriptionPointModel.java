package cz.xlisto.elektrodroid.models;

/**
 * Model odběrného místa
 * Created by xlisto on 18.04.2023
 */
public class SubscriptionPointModel {
    private long _id;
    private long milins;
    private final String name;
    private final String description;
    private String subscriptionPointId;
    private final int countPhaze;
    private final int phaze;
    private final String numberElectricMeter;
    private final String numberSubscriptionPoint;
    public SubscriptionPointModel(long _id,String name, String description, long milins,int countPhaze, int phaze, String numberElectricMeter, String numberSubscriptionPoint) {
        this._id = _id;
        this.name = name;
        this.description = description;
        this.milins = milins;
        this.countPhaze = countPhaze;
        this.phaze = phaze;
        this.numberElectricMeter = numberElectricMeter;
        this.numberSubscriptionPoint = numberSubscriptionPoint;
    }

    public SubscriptionPointModel(String name, String description, long milins,int countPhaze, int phaze, String numberElectricMeter, String numberSubscriptionPoint) {
        this.name = name;
        this.description = description;
        this.milins = milins;
        this.countPhaze = countPhaze;
        this.phaze = phaze;
        this.numberElectricMeter = numberElectricMeter;
        this.numberSubscriptionPoint = numberSubscriptionPoint;
    }

    public long getId() {
        return _id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getSubscriptionPointId() {
        return subscriptionPointId;
    }

    public int getCountPhaze() {
        return countPhaze;
    }

    public int getPhaze() {
        return phaze;
    }

    public String getNumberElectricMeter() {
        return numberElectricMeter;
    }

    public String getNumberSubscriptionPoint() {
        return numberSubscriptionPoint;
    }

    public long getMilins() {
        return milins;
    }


    /**
     * vrátí identifikační číslo odběrného místa v milisekundách (bez písmenného prefixu)
     * @return String číslo rozslišující jednotlivá odběrná místa v databázi
     */
    public String getIdMilins(){
        return String.valueOf(milins);
    }
    public String getTableO(){
        return "O"+milins;
    }
    public String getTableHDO() {
        return "HDO"+milins;
    }
    public String getTableFAK() {
        return "FAK"+milins;
    }
    public String getTableTED() {
        return "TED"+milins;
    }
    public String getTablePLATBY(){
        return "PLATBY"+milins;
    }
    public void setMilins(long milins){
        this.milins = milins;
    }


}
