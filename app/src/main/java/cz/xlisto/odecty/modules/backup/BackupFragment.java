package cz.xlisto.odecty.modules.backup;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;

import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.dialogs.YesNoDialogFragment;
import cz.xlisto.odecty.shp.ShPBackup;
import cz.xlisto.odecty.shp.ShPSubscriptionPoint;

/**
 * Xlisto 07.03.2023 12:36
 */
public class BackupFragment extends Fragment {
    private static final String TAG = "BackupFragment";
    private static final String DEF_URI = "content://com.android.externalstorage.documents/document/primary%3A";
    private static final String DEF_TREE_URI = "/tree/primary%3A";
    private View view;
    private Button btnBackup, btnSelectDir;
    private RecyclerView recyclerView;
    private ArrayList<DocumentFile> documentFiles = new ArrayList<>(); //seznam souborů
    private final String[] filtersFileName = {".cenik", ".odecet", "ElektroDroid.zip"};
    static DocumentFile selectedFile;
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    Handler handler = new Handler(Looper.getMainLooper());

    private static final int REQUEST_WRITE_STORAGE = 0;//může být jakékoliv číslo typu int, slouží pro oddělení jednotlivých oprávnění, klidně můžu používat rovniu čísla int, ale takto je to přehlednější
    private Uri uri;
    private final SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy  HH.mm.ss");//dvojtečka u hodin je zákazáný znak pro unix systémy
    private ShPBackup shPBackup;
    private ShPSubscriptionPoint shPSubscriptionPoint;
    private LinearLayout lnProgressBar;
    private BackupAdapter backupAdapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_backup, container, false);

        /*listView.setOnItemClickListener((parent, view, position, id) -> {
            Bundle arg = new Bundle();
            selectedFile = documentFiles.get(position);
            ObnovDialogFragment obnovDialogFragment = new ObnovDialogFragment();
            obnovDialogFragment.setArguments(arg);
            obnovDialogFragment.show(getActivity().getSupportFragmentManager(), "Obnov");
        });*/

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        shPBackup = new ShPBackup(getContext());
        shPSubscriptionPoint = new ShPSubscriptionPoint(getContext());
        btnBackup = view.findViewById(R.id.btnZalohuj);
        btnSelectDir = view.findViewById(R.id.btnVynerSlozku);
        recyclerView = view.findViewById(R.id.recyclerViewBackup);
        //btnBackup.setOnClickListener(v -> save());
        btnSelectDir.setOnClickListener((v) -> openTree(false));
        lnProgressBar = view.findViewById(R.id.lnProgressBar);

        requireActivity().getSupportFragmentManager().setFragmentResultListener(BackupAdapter.FLAG_DIALOG_FRAGMENT_BACKUP,this, (requestKey, result) -> {


                if(result.getBoolean(YesNoDialogFragment.RESULT)){
                    backupAdapter.recoverDatabaseFromZip();
                }

        });
        requireActivity().getSupportFragmentManager().setFragmentResultListener(BackupAdapter.FLAG_DIALOG_FRAGMENT_DELETE,this, (requestKey, result) -> {

                if(result.getBoolean(YesNoDialogFragment.RESULT)){
                    backupAdapter.deleteFile();
                }

        });
    }

    @Override
    public void onResume() {
        super.onResume();
        showLnProgressBar(true);

        uri = Uri.parse(shPBackup.get(ShPBackup.FOLDER_BACKUP, DEF_URI));
        Log.w(TAG, "Permice proced" + android.os.Process.myPid());
        Log.w(TAG, "Permice uid  " + getActivity().getApplicationInfo().uid);
        Log.w(TAG, "Permice perm" + getActivity().checkUriPermission(uri, android.os.Process.myPid(), getActivity().getApplicationInfo().uid, Intent.FLAG_GRANT_READ_URI_PERMISSION));
        Log.w(TAG, "Permice flag" + getActivity().checkUriPermission(uri, android.os.Process.myPid(), getActivity().getApplicationInfo().uid, Intent.FLAG_GRANT_WRITE_URI_PERMISSION));

        documentFiles.clear();
        recyclerView.setAdapter(new BackupAdapter(getActivity(), documentFiles, recyclerView));//nastavení prázdného adaptéru kvůli warning: No adapter attached; skipping layout
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        Log.w(TAG, "Perm " + permissions());
        btnBackup.setEnabled(permissions());
        if (permissions()) {
            executorService.execute(() -> {
                DocumentFile pickedDir = DocumentFile.fromTreeUri(getActivity(), uri);
                if (pickedDir != null) {
                    for (DocumentFile file : pickedDir.listFiles()) {

                        try {
                            if (!file.isDirectory()) {
                                if (new FilterNameFile(filtersFileName).accept(file)) {
                                    documentFiles.add(file);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    if (documentFiles != null) {
                        SortFile.quickSortDate(documentFiles);
                        backupAdapter = new BackupAdapter(getActivity(), documentFiles, recyclerView);
                        //backupAdapter.setOnListenerFile(file -> restore(file));
                        handler.postDelayed(() -> {
                            recyclerView.setAdapter(backupAdapter);
                            backupAdapter.notifyDataSetChanged();
                            recyclerView.scheduleLayoutAnimation();
                            showLnProgressBar(false);

                        }, 500);
                    }
                }
            });
        } else {
            Snackbar.make(view, getActivity().getResources().getString(R.string.add_permissions), Snackbar.LENGTH_LONG)
                    .setAction(getActivity().getResources().getString(R.string.select), v -> openTree(true))
                    .show();
        }

    }

    /**
     * Zeptá se na povolení oprávnění složky
     *
     * @return
     */
    private boolean permissions() {
        return Permissions.getInstance().isPermissionStorage(getActivity(), uri);
    }

    /**
     * Otevření aktivity pro výběr složky
     *
     * @param showRoot
     */
    private void openTree(boolean showRoot) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

        Uri uri = Uri.parse("content://com.android.externalstorage.documents/document/primary:");
        Log.w(TAG, "URI " + uri);


        //intent.putExtra("android.provider.extra.INITIAL_URI", uri);
        if (showRoot)
            intent.putExtra("android.provider.extra.INITIAL_URI", uri);
        else {       //content://com.android.externalstorage.documents/document/primary:
            Uri uriTemp = Uri.parse(shPBackup.get(ShPBackup.FOLDER_BACKUP, "content://com.android.externalstorage.documents/document/primary:"));
            intent.putExtra("android.provider.extra.INITIAL_URI", uriTemp);
        }

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        //intent.addCategory(Intent.CATEGORY_OPENABLE);
        //intent.setType("*/*");
        //intent.setType("application/zip");
        resultTree.launch(intent);
    }


    private void save() {
        //zápis souborů
        Date date = new Date();
        saveToZip(date);
        onResume();
    }

    private void saveToZip(Date date) {
        //zápis textového souboru s posledními záznamy
        //String s = readDataFromDatabase();
        /*try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(getActivity().openFileOutput("info.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(s);
            outputStreamWriter.close();
        } catch (Exception e) {
            Log.e(TAG, J + "Zápis souboru info.txt se nepovedl");
            e.printStackTrace();
        }

        Uri treeUri = new ShP(getActivity()).getUriBackup(TREE);
        DocumentFile pickedDir = DocumentFile.fromTreeUri(getActivity(), treeUri);*/

        //uložení zálohovaných souborů do ZIPu
        //DocumentFile f = pickedDir.createFile("plain/text", generateNameFile(date.getTime()) + " ElektroDroid.zip");

        //File f1 = new File(Environment.getDataDirectory(), "//data//cz.xlisto.elektrodroid//databases//odecty_a_mista");
        //File f2 = new File(Environment.getDataDirectory(), "//data//cz.xlisto.elektrodroid//databases//databaze_cenik");

        //boolean isDirectory = false;
        //for (DocumentFile file : pickedDir.listFiles()) {
        //  isDirectory = file.getParentFile().isDirectory();

        //}
        //musí tady být kontrola na výběr a oprávnění složky. Pokud není, vrací chybu: requires android.permission.MANAGE_DOCUMENTS or android.permission.MANAGE_DOCUMENTS
        //složka try mi toto nepodchytává
        /*if (pickedDir.getName() != null && isDirectory == true) {
            File f3;
            byte[] buffer = new byte[1024];
            try {
                f3 = new File(getActivity().getFilesDir() + "//info.txt");
                OutputStream fos = getActivity().getContentResolver().openOutputStream(f.getUri());
                ZipOutputStream zos = new ZipOutputStream(fos);
                ZipEntry zeOdecet = new ZipEntry("odecet.db");
                ZipEntry zeCenik = new ZipEntry("cenik.db");
                ZipEntry zeInfo = new ZipEntry("info.txt");
                FileInputStream in = null;
                int length;

                if (f1.exists()) {
                    zos.putNextEntry(zeOdecet);
                    in = new FileInputStream(f1);

                    while ((length = in.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                }

                if (f2.exists()) {
                    zos.putNextEntry(zeCenik);
                    in = new FileInputStream(f2);
                    while ((length = in.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                }

                if (f3.exists()) {
                    zos.putNextEntry(zeInfo);
                    in = new FileInputStream(f3);
                    while ((length = in.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                }

                if (in != null)
                    in.close();
                zos.closeEntry();
                zos.close();
                Toast.makeText(getActivity(), getResources().getString(R.string.nastavena_slozka), Toast.LENGTH_SHORT).show();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {

            File dir = new File(Environment.getExternalStorageDirectory().getPath() + "/ElektroDroid_zalohy");
            if (!dir.exists())
                dir.mkdir(); //kontrola existence složky, pokud neexistuje, vytvoří se nová

            Toast.makeText(getActivity(), getResources().getString(R.string.neni_slozka), Toast.LENGTH_LONG).show();
        }*/

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_WRITE_STORAGE: {
                // If request is cancelled, the fakturyArrayList arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Snackbar.make(getView(), "Přístup povolen", Snackbar.LENGTH_LONG).show();
                } else {
                    Snackbar.make(getView(), "Přístup zamítnut", Snackbar.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    private String generateNameFile(long l) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(l);
        Date date = new Date(calendar.getTimeInMillis());
        return format.format(date);
    }


    /**
     * Callback z výběru složky
     */
    private ActivityResultLauncher<Intent> resultTree = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    activityResult(result.getData());
                }
            }
    );

    /**
     * Zpracování vybrané složky
     *
     * @param data
     */
    private void activityResult(Intent data) {
        boolean isFolder;
        documentFiles = new ArrayList<>();//inicializace seznamu záložních souborů
        uri = data.getData();
        shPBackup.set(ShPBackup.FOLDER_BACKUP, uri.toString());

        //Nastavení trvalého oprávnění po restartu aplikace
        getActivity().grantUriPermission(getActivity().getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        getActivity().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        Log.w(TAG, "Permision " + data.getFlags());
        Log.w(TAG, "Uri: " + uri);
        DocumentFile pickedDir = DocumentFile.fromTreeUri(getActivity(), uri);
        Log.w(TAG, "piskeddir " + pickedDir.listFiles().length);
        // Seznam všech existujících souborů a složek
        String path = "";
        isFolder = pickedDir.isDirectory();
        for (DocumentFile file : pickedDir.listFiles()) {
            //Log.d(E, "Found file: " + file.getName() + " with size " + file.length());
            //Log.w(E, "Found folder: " + file.getParentFile().isDirectory());
            //Log.w(E, "Found folder:_" + file.getParentFile().getUri().getPathSegments().get(1));
            //Log.w(E, "Found folder: " + pickedDir.getUri().getPath());
            path = file.getParentFile().getUri().getPathSegments().get(1);
            Log.w(TAG, "Path: " + path);
            Log.w(TAG, "Path: " + file.getUri());
            isFolder = file.getParentFile().isDirectory();
            documentFiles.add(file);
        }
        //následující řádky smazat
        //Log.w(E, J + "Uri: " + treeUri.getPath());
        if (!isFolder) {
            //Toast.makeText(getActivity(), getResources().getString(R.string.vyberte_slozku), Toast.LENGTH_SHORT).show();
            //Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            //intent.addCategory(Intent.CATEGORY_OPENABLE);
            //intent.setType("*/*");
            //intent.setType("application/zip");
            //intent.setType("image/jpeg");
            //startActivityForResult(intent, 42);
        } else {
            //new ShP(getActivity()).setCestaLokalniZalohy(path);
        }

    }


    /**
     * Načítá poslední záznamy ze všech odběrných míst
     *
     * @return poslední záznam
     */
    /*private String readDataFromDatabase() {
        return new Database(getActivity(), 1).getPosledniOdectyAsText();
    }*/
    /*private void restore(DocumentFile f) {
        if (f.getName().contains(".zip")) {
            //zip archiv, který se rozbalí do povolený složky
            ArrayList<DocumentFile> files = unzip(f);
            //unzip(f);
            for (int i = 0; i < files.size(); i++) {
                Log.w(TAG, "Rozbalené soubory: " + files.get(i).getName());
                if (files.get(i).getName().equals("odecet.db") || files.get(i).getName().equals("cenik.db")) {
                    Log.w(TAG, "Seznam souborů " + files.get(i).getName());
                    obnovDatabaziZip(files.get(i));
                }
            }
            //úklid složky temp
            deleteDirectory(files);
        } else {
            //původní záložní soubory
            obnovDatabazi(f);
        }
    }*/

    /**
     * Rozbalení ZIP archivu a uložení do stávající složky
     *
     * @param documentFile
     * @return
     */
   /* private ArrayList<DocumentFile> unzip(DocumentFile documentFile) {
        //documentFile - předaný soubor k rozbalení
        Uri treeUri = Uri.parse(shPBackup.get(ShPBackup.FOLDER_BACKUP, DEF_URI));
        DocumentFile pickedDir = DocumentFile.fromTreeUri(getActivity(), treeUri);

        //DocumentFile dc = pickedDir.createFile("plain/text", " ZZZZZZ.txt");

        //vytvoření dočasné složky
        //File folder = new File(Environment.getExternalStorageDirectory() + File.separator + "Zalohy");
        //if (!folder.exists())
        //    folder.mkdir();

        //Log.w(E, J + "Dir: " + folder);
        //Log.w(E, J + "Documentfile: " + documentFile.getName());
        //File f = new File(folder + File.separator + documentFile.getName());
        //DocumentFile documentFile = pickedDir.createFile("plain/text", documentFile.getName());
        *//*try {
            InputStream in = getActivity().getContentResolver().openInputStream(documentFile.getUri());//načítaný záložní soubor
            OutputStream out = null;//výstupní soubor
            //out = new FileOutputStream(folder + File.separator + documentFile.getUri());
            out = getActivity().getContentResolver().openOutputStream(dc.getUri());

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }*//*


        ArrayList<DocumentFile> files = new ArrayList<>();
        byte[] buffer = new byte[1024];
        try {
            //vytvoření dočasné složky
            *//*folder = new File(Environment.getExternalStorageDirectory().getPath() + "/Zalohy");
            if (!folder.exists())
                folder.mkdir();*//*

            //vložení zip souboru
            ZipInputStream zis = new ZipInputStream(getActivity().getContentResolver().openInputStream(documentFile.getUri()));
            //seznam souborů v zipu
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                String fileName = ze.getName();
                //File newFile = new File(folder + File.separator + fileName);
                DocumentFile newFiled = pickedDir.findFile(fileName);
                if (newFiled == null) {
                    newFiled = pickedDir.createFile("bin", fileName);
                }

                //Log.w(E, J + "Rozbalený soubor: " + newFile.getAbsoluteFile().toString());
                //vyztvoření všech složek
                //nebo chyba ????
                //new File(newFile.getParent()).mkdirs();

                OutputStream fos = getActivity().getContentResolver().openOutputStream(newFiled.getUri());

                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
                files.add(newFiled);
                ze = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();
            Log.w(TAG, "Hotovo");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return files;
    }*/

    /*private void obnovDatabaziZip(DocumentFile f) {
        if (f == null) {
            return;
        }
        //kontrola databáze, zdali existuje, pokud ne vytvoří se a pak se následně přepíše obnovaným souborem
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getActivity());
        DataPriceListSource dataPriceListSource = new DataPriceListSource(getActivity());
        dataSubscriptionPointSource.open();
        dataPriceListSource.open();
        dataSubscriptionPointSource.close();
        dataPriceListSource.close();
        ;
        //SQLiteDatabase dbOdberneMisto = (new OvladacDatabazeOdberneMisto(getActivity(), 1).getReadableDatabase());
        //SQLiteDatabase dbCeniky = new OvladacDatabazeCeniku(getActivity(), 1).getReadableDatabase();

        try {
            File data = Environment.getDataDirectory();
            //File file = new File(f.toString());
            //Log.w(E, J + "File: " + f.toString());
            Log.w(TAG, "File extension: " + f.getName().contains(".zip"));//vrátí tru pokud je to zip
            Log.w(TAG, "Data Directory: " + Environment.getDataDirectory().toString());
            Log.w(TAG, "Name space: " + getContext().getPackageName());

            //if(data.canWrite()) {
            boolean cenik = f.getName().contains("cenik");
            String currentDBPath = "";
            if (cenik) {
                Log.w(TAG, " soubor přípona: .cenik");
                currentDBPath = "//data//" + getContext().getPackageName() + "//databases//databaze_cenik";
            }
            boolean odecet = f.getName().contains("odecet");
            if (odecet) {
                Log.w(TAG, " soubor přípona: .odecet");
                currentDBPath = "//data//" + getContext().getPackageName() + "//databases//odecty_a_mista";
            }

            //String backupDBPath = f.getPath();//mAdapter.getItem(position).toString();

            File currentDB = new File(data, currentDBPath);//vnitřní databáze
            //File backupDB = new File("", backupDBPath);

            //Log.w(E, J + "currentDB: " + currentDB.toString());//cesta k vnitřní databázi
            //Log.w(E, J + " backupDB: " + backupDB.toString());

            OutputStream src = new FileOutputStream(currentDB);
            //FileChannel dst = new FileInputStream(backupDB).getChannel();
            InputStream dst = getActivity().getContentResolver().openInputStream(f.getUri());

            int c;
            byte[] buffer = new byte[1024];
            while ((c = dst.read(buffer)) != -1) {
                src.write(buffer, 0, c);
            }
            src.close();
            src.flush();
            dst.close();

            //dbOdberneMisto.close();
            //dbCeniky.close();
            //nastavení barev VT a NT
            //TODO: doplnit nastavení aplikace
            //new Databaze(getActivity()).getColorApp();//načtení barev z databáze a nastavení do sharedPreferences, otevírá si a zavírá databázy
            shPSubscriptionPoint.set(ShPSubscriptionPoint.ID_SUBSCRIPTION_POINT, -1L);
            //new ShP(getActivity()).setIDMista(-1L);
            //new ShP(getActivity()).setIDMistaPozice(-1);
            Toast.makeText(getActivity(), "Úspěšně obnoveno. Zvolte aktuální odběrné místo.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "Bohužel se něco nepovedlo při obnově: " + e.toString(), Toast.LENGTH_LONG).show();
        }
    }*/

    /**
     * Smazání dočasných souborů z rozbaleného ZIPu
     *
     * @return
     */
    /*public static void deleteDirectory(ArrayList<DocumentFile> documentFiles) {
        for (int i = 0; i < documentFiles.size(); i++) {
            documentFiles.get(i).delete();
        }
    }*/

    /*private void obnovDatabazi(DocumentFile f) {
        if (f == null) {
            return;
        }
        Log.w(TAG, "File: " + f.getName());
        //kontrola databáze, zdali existuje, pokud ne vytvoří se a pak se následně přepíše obnovaným souborem
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getActivity());
        DataPriceListSource dataPriceListSource = new DataPriceListSource(getActivity());
        dataSubscriptionPointSource.open();
        dataPriceListSource.open();
        dataSubscriptionPointSource.close();
        dataPriceListSource.close();
        ;

        try {
            File data = Environment.getDataDirectory();
            //File file = new File(f.toString());
            Log.w(TAG, "File: " + f.toString());
            Log.w(TAG, "File extension: " + f.getName().contains(".zip"));//vrátí tru pokud je to zip
            Log.w(TAG, "Data Directory: " + Environment.getDataDirectory().toString());

            //if(data.canWrite()) {
            boolean cenik = f.getName().contains("cenik");
            String currentDBPath = "";
            if (cenik) {
                Log.w(TAG, " soubor přípona: .cenik");
                currentDBPath = "//data//" + "cz.xlisto.elektrodroid" + "//databases//databaze_cenik";
            }
            boolean odecet = f.getName().contains("odecet");
            if (odecet) {
                Log.w(TAG, " soubor přípona: .odecet");
                currentDBPath = "//data//" + "cz.xlisto.elektrodroid" + "//databases//odecty_a_mista";
            }

            String backupDBPath = f.getUri().getPath();//mAdapter.getItem(position).toString();

            File currentDB = new File(data, currentDBPath);//vnitřní dtabáze
            File backupDB = new File("", backupDBPath);
            Log.w(TAG, "currentDB: " + currentDB.toString());//cesta k vnitřní databázi
            Log.w(TAG, " backupDB: " + backupDB.toString());

            *//*FileChannel src = new FileOutputStream(currentDB).getChannel();
            FileChannel dst = new FileInputStream(backupDB).getChannel();
            src.transferFrom(dst, 0, dst.size());
            src.close();
            dst.close();*//*

            InputStream in = getActivity().getContentResolver().openInputStream(f.getUri());//načítaný záložní soubor
            //File currentDB_ = new File(currentDBPath);//soubor aplikace s databází
            OutputStream out = new FileOutputStream(currentDB);//výstupní soubor
            //out.write("A long time ago...".getBytes());
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.flush();
            out.close();

            //dbOdberneMisto.close();
            //dbCeniky.close();
            //nastavení barev VT a NT
            //TODO: doplnit nastavení aplikace
            //new Databaze(getActivity()).getColorApp();//načtení barev z databáze a nastavení do sharedPreferences, otevírá si a zavírá databázy
            shPSubscriptionPoint.set(ShPSubscriptionPoint.ID_SUBSCRIPTION_POINT, -1L);
            //new ShP(getActivity()).setIDMista(-1L);
            //new ShP(getActivity()).setIDMistaPozice(-1);
            Toast.makeText(getActivity(), "Úspěšně obnoveno. Zvolte aktuální odběrné místo.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "Bohužel se něco nepovedlo při obnově: " + e.toString(), Toast.LENGTH_LONG).show();
        }
    }*/

    /**
     * Zobrazí nebo skryje LinearLayout s ProgressBar
     */
    private void showLnProgressBar(boolean b) {
        if (b) {
            lnProgressBar.setAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
            lnProgressBar.setVisibility(View.VISIBLE);
        } else {
            lnProgressBar.setAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
            lnProgressBar.setVisibility(View.GONE);
        }
    }


}
