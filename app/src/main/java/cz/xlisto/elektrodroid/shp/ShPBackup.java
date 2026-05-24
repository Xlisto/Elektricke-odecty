package cz.xlisto.elektrodroid.shp;

import android.content.Context;

/**
 * Xlisto 07.03.2023 13:16
 */
public class ShPBackup extends ShP {
    public static final String FOLDER_BACKUP = "folderBackup";
    public static final String PENDING_WIFI_UPLOAD_USER_NAME = "pendingWifiUploadUserName";
    public static final String PENDING_WIFI_UPLOAD_FILE_NAMES = "pendingWifiUploadFileNames";


    public ShPBackup(Context context) {
        this.context = context;
    }
}
