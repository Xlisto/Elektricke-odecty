package cz.xlisto.elektrodroid.modules.pricelist.download;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cz.xlisto.elektrodroid.R;


/**
 * Fragment zobrazující seznam dodavatelů a odkazů na jejich oficiální stránky s ceníky ke stažení.
 */
public class DownloadPriceListFragment extends Fragment {

    public static final String TAG = "DownloadPriceListFragment";


    public DownloadPriceListFragment() {
        // Vyžadovaný prázdný konstruktor
    }


    /**
     * Vytvoří novou instanci fragmentu pro stažení ceníků.
     *
     * @return Nová instance {@link DownloadPriceListFragment}
     */
    public static DownloadPriceListFragment newInstance() {
        return new DownloadPriceListFragment();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_download_price_list, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rv = view.findViewById(R.id.rvSuppliers);
        Button btnBack = view.findViewById(R.id.btnBackDownload);

        List<SupplierDownloadModel> suppliers = getSupportedSuppliers();

        SupplierDownloadAdapter adapter = new SupplierDownloadAdapter(suppliers, supplier -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(supplier.getUrl()));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }


    /**
     * Vrátí seznam podporovaných dodavatelů s parsery a jejich oficiálními URL adresami ke stažení ceníků.
     *
     * @return Seznam objektů {@link SupplierDownloadModel}
     */
    private List<SupplierDownloadModel> getSupportedSuppliers() {
        List<SupplierDownloadModel> list = new ArrayList<>();
        list.add(new SupplierDownloadModel("ČEZ Prodej, a.s.", "https://www.cez.cz/nextcez/cs/ceniky"));
        list.add(new SupplierDownloadModel("E.ON Energie, a.s. / EG.D", "https://www.eon.cz/domacnosti/zakaznicka-pece/ceniky/"));
        list.add(new SupplierDownloadModel("Pražská energetika, a. s. (PRE)", "https://www.pre.cz/cs/domacnosti/elektrina/prehled-produktu/"));
        list.add(new SupplierDownloadModel("CENTROPOL ENERGY, a. s.", "https://www.centropol.cz/ceniky/"));
        list.add(new SupplierDownloadModel("Česká Regionální Energetika a.s.", "https://www.cr-energetika.cz/ke-stazeni/"));
        list.add(new SupplierDownloadModel("innogy Energie, s.r.o.", "https://www.innogy.cz/ceniky/"));
        list.add(new SupplierDownloadModel("MND Energie a.s.", "https://www.mnd.cz/elektrina-domacnosti"));
        return list;
    }

}
