package cz.xlisto.elektrodroid.models;


import androidx.annotation.NonNull;

import java.util.Arrays;


/**
 * Třída představující model popisu změn verze aplikace.
 *
 * @param version číslo verze aplikace
 * @param date    datum vydání verze
 * @param changes seznam provedených změn
 */
public record VersionModel(int version, String date, String[] changes) {

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
