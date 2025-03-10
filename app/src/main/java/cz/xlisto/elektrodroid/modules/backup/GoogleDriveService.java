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
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
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
import java.security.GeneralSecurityException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import cz.xlisto.elektrodroid.R;


/**
 * Třída GoogleDriveService poskytuje metody pro interakci s Google Drive API.
 */
public class GoogleDriveService {

    private static final String TAG = "GoogleDriveService";
    private Drive drive;
    private String currentFolderId;
    private final Context context;
    private OnFilesLoadedListener onFilesLoadedListener;
    private OnDriverServiceListener onDriveServiceListener;


    /**
     * Konstruktor třídy GoogleDriveService.
     *
     * @param context     Kontext aplikace.
     * @param accountName Název účtu Google, který se má použít.
     * @param folderId    ID složky na Google Drive, ze které se mají načíst soubory.
     */
    public GoogleDriveService(Context context, String accountName, String folderId) {
        this.context = context;
        new Thread(() -> {
            try {
                NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
                JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

                GoogleAccountCredential googleAccountCredential = GoogleAccountCredential.usingOAuth2(context, Collections.singletonList(DriveScopes.DRIVE_FILE));
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

                    isTokenValid(token);
                    // Check if the token is valid and has the required permission
                    drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, request -> request.getHeaders().setAuthorization("Bearer " + token)).setApplicationName(context.getString(R.string.app_name)).build();

                    if (onFilesLoadedListener != null)
                        onFilesLoadedListener.onFilesLoaded(listFilesInFolder(folderId));
                    if (onDriveServiceListener != null)
                        onDriveServiceListener.onDriveServiceReady();

                    Log.d(TAG, "Token is valid and has the required permission.");
                } catch (UserRecoverableAuthException e) {
                    Log.w(TAG, "UserRecoverableAuthException: " + e.getMessage());
                    Intent consentIntent = e.getIntent();
                    ((Activity) context).startActivityForResult(consentIntent, 111);
                } catch (GoogleAuthException e) {
                    Log.e(TAG, "GoogleAuthException: " + e.getMessage(), e);
                } catch (IOException e) {
                    Log.e(TAG, "IOException: " + e.getMessage(), e);
                }

            } catch (GeneralSecurityException | IOException e) {
                Log.e(TAG, "Chyba při vytváření Google Drive Service: " + e.getMessage(), e);
            }

        }).start();
    }


    /**
     * Nastaví posluchače událostí načtení souborů z Google Drive.
     *
     * @param onFilesLoadedListener Rozhraní pro naslouchání událostem načtení souborů z Google Drive.
     */
    public void setOnFilesLoadedListener(OnFilesLoadedListener onFilesLoadedListener) {
        this.onFilesLoadedListener = onFilesLoadedListener;
    }


    /**
     * Nastaví posluchače událostí pro službu Google Drive.
     *
     * @param onDriveServiceListener Rozhraní pro naslouchání událostem služby Google Drive.
     */
    public void setOnDriveServiceListener(OnDriverServiceListener onDriveServiceListener) {
        this.onDriveServiceListener = onDriveServiceListener;
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
     * @param folderId ID složky, do které se má soubor nahrát.
     * @return Nahraný soubor.
     * @throws IOException Pokud dojde k chybě při nahrávání souboru.
     */
    public boolean uploadFile(DocumentFile documentFile, String folderId) {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(documentFile.getUri());

            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream != null ? inputStream.read(buffer) : 0) != -1) {
                byteArrayOutputStream.write(buffer, 0, len);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading file content: " + e.getMessage(), e);
            return false;
        }

        byte[] fileContent = byteArrayOutputStream.toByteArray();
        long creationTime = documentFile.lastModified();
        File fileMetadata = new File();
        fileMetadata.setParents(Collections.singletonList(folderId));
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
     * @return true, pokud byl soubor úspěšně smazán, jinak false.
     */
    public ResultAction deleteFile(String fileId) {
        int[] counts = countFilesAndFolders(fileId);
        int folderCount = counts[0];
        int fileCount = counts[1];

        if (folderCount > 0 || fileCount > 0) {
            return new ResultAction(ResultAction.RESULT_ERROR, context.getString(R.string.not_deleted_folder));
        }

        try {
            drive.files().delete(fileId).execute();
            return new ResultAction(ResultAction.RESULT_OK, context.getString(R.string.deleted_file));
        } catch (IOException e) {
            e.printStackTrace();
            return new ResultAction(ResultAction.RESULT_ERROR, context.getString(R.string.not_deleted_file) + e.getMessage());
        }
    }


    /**
     * Vytvoří novou složku na Google Drive.
     *
     * @param folderName Název složky, která se má vytvořit.
     * @return true, pokud byla složka úspěšně vytvořena, jinak false.
     */
    public boolean createFolder(String folderName) {
        File fileMetadata = new File();
        fileMetadata.setName(folderName);
        fileMetadata.setMimeType("application/vnd.google-apps.folder");
        fileMetadata.setParents(Collections.singletonList(currentFolderId));
        try {
            drive.files().create(fileMetadata)
                    .setFields("id, name, mimeType, parents")
                    .execute();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error creating folder: " + e.getMessage(), e);
            return false;
        }
    }


    /**
     * Přejmenuje složku na Google Drive.
     *
     * @param folderId ID složky, která se má přejmenovat.
     * @param newName  Nový název složky.
     * @return true, pokud byla složka úspěšně přejmenována, jinak false.
     */
    public boolean renameFolder(String folderId, String newName) {
        try {
            File fileMetadata = new File();
            fileMetadata.setName(newName);

            drive.files().update(folderId, fileMetadata)
                    .setFields("id, name")
                    .execute();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error renaming folder: " + e.getMessage(), e);
            return false;
        }
    }


    /**
     * Načte seznam souborů ve složce na Google Drive.
     *
     * @param folderId ID složky, ze které se mají načíst soubory.
     * @return Seznam souborů ve složce.
     */
    public List<File> listFilesInFolder(String folderId) {
        List<File> allFiles = new ArrayList<>();
        String pageToken = null;

        do {
            try {
                FileList result = drive.files().list()
                        .setQ("'" + folderId + "' in parents and trashed = false")
                        .setPageSize(50)
                        .setSpaces("drive")
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

        // Přidání položky pro navigaci na nadřazenou složku, pokud není kořenová
        String parentFolderId = getParentFolderId(folderId);
        currentFolderId = folderId;
        if (parentFolderId != null) {
            File parentFolderItem = new File();
            parentFolderItem.setName("...");
            parentFolderItem.setMimeType("application/vnd.google-apps.folder");
            parentFolderItem.setId(parentFolderId);
            allFiles.add(0, parentFolderItem);
        }

        // Fitrace souborů
        allFiles.removeIf(file -> {
            String name = file.getName();
            String type = file.getMimeType();
            return !(name.endsWith("El odecet.zip") || name.endsWith("ElektroDroid.zip") || type.equals("application/vnd.google-apps.folder"));
        });

        // Třídění souborů
        Collator collator = Collator.getInstance(new Locale("cs", "CZ"));
        Collections.sort(allFiles, (f1, f2) -> {
            boolean isFolder1 = "application/vnd.google-apps.folder".equals(f1.getMimeType());
            boolean isFolder2 = "application/vnd.google-apps.folder".equals(f2.getMimeType());

            if (isFolder1 && !isFolder2) {
                return -1;
            } else if (!isFolder1 && isFolder2) {
                return 1;
            } else if (isFolder1) {
                return collator.compare(f1.getName(), f2.getName());
            } else {
                DateTime date1 = f1.getModifiedTime() != null ? f1.getModifiedTime() : f1.getCreatedTime();
                DateTime date2 = f2.getModifiedTime() != null ? f2.getModifiedTime() : f2.getCreatedTime();
                return Long.compare(date2.getValue(), date1.getValue());
            }
        });

        return allFiles;
    }


    /**
     * Získá ID nadřazené složky pro dané ID složky.
     *
     * @param folderId ID složky, pro kterou se má získat ID nadřazené složky.
     * @return ID nadřazené složky nebo null, pokud se nepodaří získat ID.
     */
    private String getParentFolderId(String folderId) {
        try {
            File file = drive.files().get(folderId).setFields("parents").execute();
            if (file.getParents() != null && !file.getParents().isEmpty()) {
                return file.getParents().get(0);
            }
        } catch (IOException e) {
            Log.e(TAG, "An error occurred while getting parent folder ID: " + e);
        }
        return null;
    }


    /**
     * Vrátí instanci služby Google Drive.
     *
     * @return instance služby Google Drive.
     */
    public Drive getDrive() {
        return drive;
    }


    /**
     * Zjistí počet souborů a složek v dané složce.
     *
     * @param folderId ID složky, ve které se mají spočítat soubory a složky.
     * @return Pole dvou čísel, kde první číslo je počet složek a druhé číslo je počet souborů.
     */
    public int[] countFilesAndFolders(String folderId) {
        int folderCount = 0;
        int fileCount = 0;
        String pageToken = null;

        do {
            try {
                FileList result = drive.files().list()
                        .setQ("'" + folderId + "' in parents and trashed = false")
                        .setPageSize(50)
                        .setSpaces("drive")
                        .setFields("nextPageToken, files(id, name, mimeType)")
                        .setPageToken(pageToken)
                        .execute();
                for (File file : result.getFiles()) {
                    if ("application/vnd.google-apps.folder".equals(file.getMimeType())) {
                        folderCount++;
                    } else {
                        fileCount++;
                    }
                }
                pageToken = result.getNextPageToken();
            } catch (IOException e) {
                Log.e(TAG, "An error occurred: " + e);
                break;
            }
        } while (pageToken != null);

        return new int[]{folderCount, fileCount};
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

    }


    /**
     * Třída ResultAction představuje výsledek akce s příslušnou zprávou.
     * <p>
     * Tato třída obsahuje konstanty pro různé výsledky akce a zprávu popisující výsledek.
     */
    static class ResultAction {

        public static final int RESULT_OK = 1;
        public static final int RESULT_CANCEL = 2;
        public static final int RESULT_ERROR = 3;
        public String message;
        public int result;


        public ResultAction(int result, String message) {
            this.message = message;
            this.result = result;
        }

    }

}
