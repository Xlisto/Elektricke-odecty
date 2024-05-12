package cz.xlisto.elektrodroid.models;

import androidx.annotation.NonNull;

public interface ConsuptionModel {


    Double getConsuptionVT();


    Double getConsuptionNT();


    String getYearAsString();


    int getYearAsInt();


    String getDateMonth();


    void setDates(Long dates);


    void setFirstReader(int i);


    void addConsuptionVT(Double spotrebaVT);


    void addConsuptionNT(Double spotrebaVT);


    Integer getConsuptionVTAnimace();


    Integer getConsuptionNTAnimace();


    void setAnimate(int anim, int max);


    @NonNull
    String toString();


    String getDateMonthAsStringShort();


    String getDateMonthAsStringLong();
}
