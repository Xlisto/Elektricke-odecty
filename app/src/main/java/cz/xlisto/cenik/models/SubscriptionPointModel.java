package cz.xlisto.cenik.models;

/**
 * Model odběrného místa
 * Created by xlisto on 18.04.2023
 */
public class SubscriptionPointModel {
    private long _id;
    private long milins;
    private String name;
    private String description;
    private String subscriptionPointId;
    private int countPhaze;
    private int phaze;
    private String numberElectricMeter;
    private String numberSubscriptionPoint;
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

    public long get_id() {
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
