package cz.xlisto.odecty.modules.backup;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.odecty.BuildConfig;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataMonthlyReadingSource;
import cz.xlisto.odecty.dialogs.YesNoDialogFragment;
import cz.xlisto.odecty.ownview.ViewHelper;
import cz.xlisto.odecty.shp.ShPBackup;

/**
 * Xlisto 07.03.2023 12:36
 */
public class BackupFragment extends Fragment {
    private static final String TAG = "BackupFragment";
    private static final String DEF_URI = "content://com.android.externalstorage.documents/document/primary%3A";
    private static final String DEF_TREE_URI = "/tree/primary%3A";
    private Button btnBackup;
    private RecyclerView recyclerView;
    private ArrayList<DocumentFile> documentFiles = new ArrayList<>(); //seznam souborů
    private final String[] filtersFileName = {".cenik", ".odecet", "ElektroDroid.zip", "El odecet.zip"};
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    Handler handler = new Handler(Looper.getMainLooper());

    private static final int REQUEST_WRITE_STORAGE = 0;//může být jakékoliv číslo typu int, slouží pro oddělení jednotlivých oprávnění
    private Uri uri;
    private ShPBackup shPBackup;
    private LinearLayout lnProgressBar;
    private BackupAdapter backupAdapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_backup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        shPBackup = new ShPBackup(getContext());
        btnBackup = view.findViewById(R.id.btnZalohuj);
        Button btnSelectDir = view.findViewById(R.id.btnVynerSlozku);
        recyclerView = view.findViewById(R.id.recyclerViewBackup);
        btnBackup.setOnClickListener(v -> save());
        btnSelectDir.setOnClickListener((v) -> openTree(false));
        lnProgressBar = view.findViewById(R.id.lnProgressBar);

        requireActivity().getSupportFragmentManager().setFragmentResultListener(BackupAdapter.FLAG_DIALOG_FRAGMENT_BACKUP, this, (requestKey, result) -> {


            if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                backupAdapter.recoverDatabaseFromZip();
            }

        });
        requireActivity().getSupportFragmentManager().setFragmentResultListener(BackupAdapter.FLAG_DIALOG_FRAGMENT_DELETE, this, (requestKey, result) -> {

            if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                backupAdapter.deleteFile();
            }

        });
    }


    @Override
    public void onResume() {
        super.onResume();
        showLnProgressBar(true);

        uri = Uri.parse(shPBackup.get(ShPBackup.FOLDER_BACKUP, DEF_URI));
        documentFiles.clear();
        recyclerView.setAdapter(new BackupAdapter(requireActivity(), documentFiles, recyclerView));//nastavení prázdného adaptéru kvůli warning: No adapter attached; skipping layout
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        btnBackup.setEnabled(permissions());
        if (permissions()) {
            executorService.execute(() -> {
                DocumentFile pickedDir = DocumentFile.fromTreeUri(requireActivity(), uri);
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
                        backupAdapter = new BackupAdapter(requireActivity(), documentFiles, recyclerView);
                        handler.postDelayed(() -> {
                            recyclerView.setAdapter(backupAdapter);
                            //backupAdapter.notifyDataSetChanged();
                            recyclerView.scheduleLayoutAnimation();
                            showLnProgressBar(false);

                        }, 500);
                    }
                }
            });
        } else {
            Snackbar.make(requireView(), requireActivity().getResources().getString(R.string.add_permissions), Snackbar.LENGTH_LONG)
                    .setAction(requireActivity().getResources().getString(R.string.select), v -> openTree(true))
                    .show();
        }

    }


    /**
     * Dotaz se na povolení oprávnění složky
     *
     * @return true pokud je povoleno
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
        resultTree.launch(intent);
    }


    /**
     * Vytvoří zálohu
     */
    private void save() {
        //zápis souborů
        Date date = new Date();
        saveToZip(date);
    }


    /**
     * Uloží databáze do ZIPu
     *
     * @param date datum vytvoření
     */
    private void saveToZip(Date date) {
        //vytvoření a zápis do textového souboru s posledními záznamy
        String s = readDataFromDatabase();
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(requireActivity().openFileOutput("info.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(s);
            outputStreamWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Uri treeUri = Uri.parse(shPBackup.get(ShPBackup.FOLDER_BACKUP, DEF_TREE_URI));
        DocumentFile pickedDir = DocumentFile.fromTreeUri(requireActivity(), treeUri);

        //uložení zálohovaných souborů do ZIPu
        assert pickedDir != null;
        DocumentFile f = pickedDir.createFile("plain/text", generateNameFile(date.getTime()) + " " + filtersFileName[3]);

        String applicationId = BuildConfig.APPLICATION_ID;
        File f1 = new File(Environment.getDataDirectory(), "//data//" + applicationId + "//databases//odecty_a_mista");
        File f2 = new File(Environment.getDataDirectory(), "//data//" + applicationId + "//databases//databaze_cenik");

        boolean isDirectory = false;
        for (DocumentFile file : pickedDir.listFiles()) {
            isDirectory = Objects.requireNonNull(file.getParentFile()).isDirectory();

        }
        //musí tady být kontrola na výběr a oprávnění složky. Pokud není, vrací chybu: requires android.permission.MANAGE_DOCUMENTS or android.permission.MANAGE_DOCUMENTS

        if (pickedDir.getName() != null && isDirectory) {
            try {
                File f3 = new File((requireActivity()).getFilesDir() + "//info.txt");

                assert f != null;
                OutputStream fos = requireActivity().getContentResolver().openOutputStream(f.getUri());
                ZipOutputStream zos = new ZipOutputStream(fos);
                ZipEntry zeOdecet = new ZipEntry("odecet.db");
                ZipEntry zeCenik = new ZipEntry("cenik.db");
                ZipEntry zeInfo = new ZipEntry("info.txt");

                if (f1.exists()) {
                    writeToFile(f1, zeOdecet, zos);
                }

                if (f2.exists()) {
                    writeToFile(f2, zeCenik, zos);
                }

                if (f3.exists()) {
                    writeToFile(f3, zeInfo, zos);
                }

                zos.closeEntry();
                zos.close();
                Toast.makeText(requireActivity(), getResources().getString(R.string.backup_created), Toast.LENGTH_SHORT).show();
                documentFiles.add(f);
                SortFile.quickSortDate(documentFiles);
                backupAdapter.notifyItemInserted(0);
                backupAdapter.notifyItemRangeChanged(0, documentFiles.size());


                // Získání výšky jednoho prvku v RecyclerView (pokud je pevná výška)
                int itemHeight = recyclerView.getChildAt(0).getHeight();

                // Vytvoření ValueAnimator pro animovaný posun
                ValueAnimator animator = ValueAnimator.ofInt(0, itemHeight);
                animator.setDuration(1000); // Dobu trvání

                animator.addUpdateListener(animation -> {
                    int animatedValue = (int) animation.getAnimatedValue();
                    recyclerView.scrollBy(0, -animatedValue); // Záporné znaménko posune o výšku jedné položky nahoru
                });

                // Spuštění animace
                animator.start();

                //smazání dočasného souboru info.txt
                f3.delete();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {

            File dir = new File(Environment.getExternalStorageDirectory().getPath() + "/El_odecty_zalohy");
            if (!dir.exists())
                dir.mkdir(); //kontrola existence složky, pokud neexistuje, vytvoří se nová

            Toast.makeText(requireActivity(), getResources().getString(R.string.no_folder), Toast.LENGTH_LONG).show();
        }

    }


    /**
     * Zápis jednotlivých souborů do souboru ZIP
     *
     * @param file soubor(y) s daty, které se mají uložit do ZIPu
     * @param zipEntry název souboru v ZIPu
     * @param zos ZipOutputStream
     * @throws IOException chyba při zápisu
     */
    private void writeToFile(File file, ZipEntry zipEntry, ZipOutputStream zos) throws IOException {
        byte[] buffer = new byte[1024];
        int length;
        zos.putNextEntry(zipEntry);
        FileInputStream in = new FileInputStream(file);
        while ((length = in.read(buffer)) > 0) {
            zos.write(buffer, 0, length);
        }

        in.close();
    }


    //TODO: DEPRECATED!!!
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_WRITE_STORAGE) {// If request is cancelled, the fakturyArrayList arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(requireView(), "Přístup povolen", Snackbar.LENGTH_LONG).show();
            } else {
                Snackbar.make(requireView(), "Přístup zamítnut", Snackbar.LENGTH_LONG).show();
            }
        }
    }


    /**
     * Vygeneruje název souboru podle data a času vytvoření
     *
     * @param l čas vytvoření
     * @return název souboru
     */
    private String generateNameFile(long l) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(l);
        return ViewHelper.getSimpleDateFormatForFiles().format(new Date(calendar.getTimeInMillis()));
    }


    /**
     * Callback z výběru složky
     */
    private final ActivityResultLauncher<Intent> resultTree = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    assert result.getData() != null;
                    activityResult(result.getData());
                }
            }
    );


    /**
     * Zpracování vybrané složky
     *
     * @param data Uri v objektu intent
     */
    private void activityResult(Intent data) {
        documentFiles = new ArrayList<>();//inicializace seznamu záložních souborů
        uri = data.getData();
        assert uri != null;
        shPBackup.set(ShPBackup.FOLDER_BACKUP, uri.toString());

        //Nastavení trvalého oprávnění po restartu aplikace
        requireActivity().grantUriPermission(requireActivity().getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        requireActivity().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        DocumentFile pickedDir = DocumentFile.fromTreeUri(requireActivity(), uri);
        assert pickedDir != null;
        // Seznam všech existujících souborů a složek
        //String path;
        Collections.addAll(documentFiles, pickedDir.listFiles());
    }


    /**
     * Načítá poslední záznamy ze všech odběrných míst
     *
     * @return poslední záznam
     */
    private String readDataFromDatabase() {
        DataMonthlyReadingSource dataMonthlyReadingSource = new DataMonthlyReadingSource(requireActivity());
        dataMonthlyReadingSource.open();
        String s = dataMonthlyReadingSource.getLastMonthlyReadingAsText();
        dataMonthlyReadingSource.close();
        return s;
    }


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
