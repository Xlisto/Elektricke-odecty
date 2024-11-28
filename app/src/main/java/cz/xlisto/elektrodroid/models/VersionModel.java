package cz.xlisto.elektrodroid.models;


import androidx.annotation.NonNull;

import java.util.Arrays;


/**
 * Třída představující model popisu změn verze.
 */
public class VersionModel {

    private final int version;
    private final String date;
    private final String[] changes;


    /**
     * Vrací verzi.
     *
     * @return verze
     */
    public int getVersion() {
        return version;
    }


    /**
     * Vrací datum.
     *
     * @return datum
     */
    public String getDate() {
        return date;
    }


    /**
     * Vrací změny.
     *
     * @return změny
     */
    public String[] getChanges() {
        return changes;
    }


    /**
     * Konstruktor třídy VersionModel.
     *
     * @param version verze
     * @param date    datum
     * @param changes změny
     */
    public VersionModel(int version, String date, String[] changes) {
        this.version = version;
        this.date = date;
        this.changes = changes;
    }


    /**
     * Vrací řetězcovou reprezentaci objektu VersionModel.
     *
     * @return řetězcová reprezentace objektu VersionModel
     */
    @NonNull
    @Override
    public String toString() {
        return "VersionModel{" +
                "version=" + version +
                ", date='" + date + '\'' +
                ", changes=" + Arrays.toString(changes) +
                '}';
    }

}
