package cz.xlisto.elektrodroid.modules.backup;


import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.shp.ShPGoogleDrive;


/**
 * Servisní vrstva pro práci s Google Drive (appData prostor aplikace).
 * Třída řeší inicializaci klienta Drive API, ověření/obnovu autorizace, načítání seznamu
 * záložních souborů, nahrávání nových záloh a mazání existujících souborů.
 * Komunikace s UI probíhá přes callback rozhraní.
 */
public class GoogleDriveService {

    private static final String TAG = "GoogleDriveService";
    private static final String APP_DATA_SPACE = ShPGoogleDrive.APP_DATA_FOLDER;
    private Drive drive;
    private final Context context;
    private final String accountName;
    private OnFilesLoadedListener onFilesLoadedListener;
    private OnDriverServiceListener onDriveServiceListener;
    private boolean initializationStarted = false;


    /**
     * Konstruktor třídy GoogleDriveService.
     *
     * @param context     Kontext aplikace.
     * @param accountName Název účtu Google, který se má použít.
     */
    public GoogleDriveService(Context context, String accountName) {
        this.context = context;
        this.accountName = accountName;
    }


    /**
     * Nastaví posluchače událostí načtení souborů z Google Drive.
     *
     * @param onFilesLoadedListener Rozhraní pro naslouchání událostem načtení souborů z Google Drive.
     */
    public void setOnFilesLoadedListener(OnFilesLoadedListener onFilesLoadedListener) {
        this.onFilesLoadedListener = onFilesLoadedListener;
        startInitializationIfNeeded();
    }


    /**
     * Nastaví posluchače událostí pro službu Google Drive.
     *
     * @param onDriveServiceListener Rozhraní pro naslouchání událostem služby Google Drive.
     */
    public void setOnDriveServiceListener(OnDriverServiceListener onDriveServiceListener) {
        this.onDriveServiceListener = onDriveServiceListener;
        startInitializationIfNeeded();
    }


    /**
     * Spustí jednorázovou inicializaci Drive klienta, pokud již neproběhla.
     * Inicializace běží na background vlákně.
     */
    private synchronized void startInitializationIfNeeded() {
        if (initializationStarted)
            return;

        initializationStarted = true;
        new Thread(() -> {
            try {
                NetHttpTransport HTTP_TRANSPORT = new NetHttpTransport();
                JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

                GoogleAccountCredential googleAccountCredential = GoogleAccountCredential.usingOAuth2(context, Collections.singletonList(DriveScopes.DRIVE_APPDATA));
                googleAccountCredential.setSelectedAccount(new Account(accountName, "com.google"));

                try {
                    String token = googleAccountCredential.getToken();

                    try {
                        //vymazání tokenu
                        googleAccountCredential.getGoogleAccountManager().invalidateAuthToken(token);
                        Log.w(TAG, "Token has been invalidated.");
                    } catch (Exception e) {
                        Log.e(TAG, "Error invalidating token: " + e.getMessage(), e);
                    }

                    if (isTokenValid(token)) {
                        Log.d(TAG, "Token is valid and has the required permission.");
                    }
                    // Check if the token is valid and has the required permission
                    drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, request -> request.getHeaders().setAuthorization("Bearer " + token)).setApplicationName(context.getString(R.string.app_name)).build();

                    if (onFilesLoadedListener != null)
                        onFilesLoadedListener.onFilesLoaded(listFilesInFolder());
                    if (onDriveServiceListener != null)
                        onDriveServiceListener.onDriveServiceReady();

                    Log.d(TAG, "Token is valid and has the required permission.");
                } catch (UserRecoverableAuthException e) {
                    Log.w(TAG, "UserRecoverableAuthException: " + e.getMessage());
                    Intent consentIntent = e.getIntent();
                    ((Activity) context).startActivityForResult(consentIntent, 111);
                    notifyDriveServiceError(context.getString(R.string.google_drive_service_init_failed));
                } catch (GoogleAuthException e) {
                    Log.e(TAG, "GoogleAuthException: " + e.getMessage(), e);
                    notifyDriveServiceError(context.getString(R.string.google_drive_service_init_failed));
                } catch (IOException e) {
                    Log.e(TAG, "IOException: " + e.getMessage(), e);
                    notifyDriveServiceError(context.getString(R.string.google_drive_service_init_failed));
                }

            } catch (Exception e) {
                Log.e(TAG, "Chyba při vytváření Google Drive Service: " + e.getMessage(), e);
                notifyDriveServiceError(context.getString(R.string.google_drive_service_init_failed));
            }

        }).start();
    }


    /**
     * Propaguje chybu inicializace služby do registrovaného posluchače.
     *
     * @param message uživatelsky čitelná zpráva chyby
     */
    private void notifyDriveServiceError(String message) {
        if (onDriveServiceListener != null)
            onDriveServiceListener.onDriveServiceError(message);
    }


    /**
     * Ověří, zda je daný přístupový token platný.
     *
     * @param accessToken Přístupový token, který se má ověřit.
     * @return true, pokud je token platný, jinak false.
     */
    public boolean isTokenValid(String accessToken) {
        try {
            String tokenInfoUrl = "https://oauth2.googleapis.com/tokeninfo?access_token=" + accessToken;
            URL url = new URL(tokenInfoUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            String responseMessage = connection.getResponseMessage();
            Log.w(TAG, "isTokenValid: " + responseCode + " " + responseMessage);
            if (responseCode == 200) {
                // Token je platný
                return true;
            } else {
                // Token není platný
                Log.w(TAG, "neplatný token ");
                return false;
            }
        } catch (Exception e) {
            Log.e("TokenValidation", "Chyba při ověřování tokenu", e);
            return false;
        }
    }


    /**
     * Nahraje soubor na Google Drive.
     *
     * @param documentFile soubor vybraný přes SAF, který bude nahrán
     * @return {@code true}, pokud se nahrání podařilo, jinak {@code false}
     */
    public boolean uploadFile(DocumentFile documentFile) {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            try (InputStream inputStream = context.getContentResolver().openInputStream(documentFile.getUri())) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputStream != null ? inputStream.read(buffer) : 0) != -1) {
                    byteArrayOutputStream.write(buffer, 0, len);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading file content: " + e.getMessage(), e);
            return false;
        }

        byte[] fileContent = byteArrayOutputStream.toByteArray();
        long creationTime = documentFile.lastModified();
        File fileMetadata = new File();
        fileMetadata.setParents(Collections.singletonList(APP_DATA_SPACE));
        fileMetadata.setName(documentFile.getName());
        fileMetadata.setCreatedTime(new DateTime(creationTime));
        fileMetadata.setModifiedTime(new DateTime(creationTime));
        ByteArrayContent mediaContent = new ByteArrayContent("application/octet-stream", fileContent);

        try {
            drive.files().create(fileMetadata, mediaContent)
                    .setFields("id, name, createdTime, modifiedTime")
                    .execute();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error uploading file: " + e.getMessage(), e);
            return false;
        }
    }


    /**
     * Smaže soubor na Google Drive.
     *
     * @param fileId ID souboru, který se má smazat.
     * @return Výsledek akce včetně zprávy pro UI.
     */
    public ResultAction deleteFile(String fileId) {
        if (drive == null)
            return new ResultAction(ResultAction.RESULT_ERROR, context.getString(R.string.not_deleted_file));

        if (fileId == null || fileId.trim().isEmpty())
            return new ResultAction(ResultAction.RESULT_ERROR, context.getString(R.string.not_deleted_file));

        try {
            drive.files().delete(fileId).execute();
            return new ResultAction(ResultAction.RESULT_OK, context.getString(R.string.deleted_file));
        } catch (IOException e) {
            Log.e(TAG, "Error deleting file: " + e.getMessage(), e);
            return new ResultAction(ResultAction.RESULT_ERROR, context.getString(R.string.not_deleted_file));
        }
    }


    /**
     * Načte seznam souborů ve složce na Google Drive.
     *
     * @return Seznam souborů ve složce.
     */
    public List<File> listFilesInFolder() {
        List<File> allFiles = new ArrayList<>();
        String pageToken = null;

        do {
            try {
                FileList result = drive.files().list()
                        .setSpaces(APP_DATA_SPACE)
                        .setQ("trashed = false")
                        .setPageSize(50)
                        .setFields("nextPageToken, files(id, name, mimeType, parents, createdTime, modifiedTime)")
                        .setPageToken(pageToken)
                        .execute();
                allFiles.addAll(result.getFiles());
                pageToken = result.getNextPageToken();
            } catch (IOException e) {
                Log.e(TAG, "An error occurred: " + e);
                break;
            }
        } while (pageToken != null);

        // Filtrace souborů
        allFiles.removeIf(file -> {
            String name = file.getName();
            return name == null || !(name.endsWith("El odecet.zip") || name.endsWith("ElektroDroid.zip"));
        });

        // Třídění souborů
        Collator collator = Collator.getInstance(new Locale("cs", "CZ"));
        Collections.sort(allFiles, (f1, f2) -> {
            DateTime date1 = f1.getModifiedTime() != null ? f1.getModifiedTime() : f1.getCreatedTime();
            DateTime date2 = f2.getModifiedTime() != null ? f2.getModifiedTime() : f2.getCreatedTime();
            if (date1 == null && date2 == null) {
                return collator.compare(f1.getName(), f2.getName());
            }
            if (date1 == null) {
                return 1;
            }
            if (date2 == null) {
                return -1;
            }
            return Long.compare(date2.getValue(), date1.getValue());
        });

        return allFiles;
    }

    /**
     * Vrátí instanci služby Google Drive.
     *
     * @return inicializovaná instance služby Google Drive, nebo {@code null}, pokud
     * inicializace ještě neproběhla
     */
    public Drive getDrive() {
        return drive;
    }


    /**
     * Rozhraní pro naslouchání událostem načtení souborů z Google Drive.
     */
    public interface OnFilesLoadedListener {

        /**
         * Metoda volaná při načtení souborů z Google Drive.
         *
         * @param files Seznam souborů načtených z Google Drive.
         */
        void onFilesLoaded(List<File> files);

    }


    /**
     * Rozhraní pro naslouchání událostem služby Google Drive.
     */
    public interface OnDriverServiceListener {

        /**
         * Metoda volaná při připravenosti služby Google Drive.
         */
        void onDriveServiceReady();

        /**
         * Výchozí callback pro chybu inicializace.
         *
         * @param errorMessage text chyby
         */
        default void onDriveServiceError(String errorMessage) {
        }

    }


    /**
     * Třída ResultAction představuje výsledek akce s příslušnou zprávou.
     * <p>
     * Tato třída obsahuje konstanty pro různé výsledky akce a zprávu popisující výsledek.
     */
    public static class ResultAction {

        public static final int RESULT_OK = 1;
        public static final int RESULT_ERROR = 3;
        public String message;
        public int result;


        /**
         * Vytvoří objekt výsledku akce.
         *
         * @param result  stav operace (např. {@link #RESULT_OK}, {@link #RESULT_ERROR})
         * @param message textová zpráva pro UI
         */
        public ResultAction(int result, String message) {
            this.message = message;
            this.result = result;
        }

    }

}
