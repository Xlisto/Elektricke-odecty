package cz.xlisto.elektrodroid.models;


import androidx.annotation.NonNull;

import java.util.Arrays;


/**
 * Třída představující model popisu změn verze.
 */
public record VersionModel(int version, String date, String[] changes) {

    /**
     * Vrací verzi.
     *
     * @return verze
     */
    @Override
    public int version() {
        return version;
    }


    /**
     * Vrací datum.
     *
     * @return datum
     */
    @Override
    public String date() {
        return date;
    }


    /**
     * Vrací změny.
     *
     * @return změny
     */
    @Override
    public String[] changes() {
        return changes;
    }


    /**
     * Konstruktor třídy VersionModel.
     *
     * @param version verze
     * @param date    datum
     * @param changes změny
     */
    public VersionModel {
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
