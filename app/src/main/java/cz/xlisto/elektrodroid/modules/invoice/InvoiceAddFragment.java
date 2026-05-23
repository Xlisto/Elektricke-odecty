package cz.xlisto.elektrodroid.modules.invoice;


import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


/**
 * Fragment pro přidání nového záznamu ve faktuře
 * Xlisto 04.02.2023 11:43
 */
public class InvoiceAddFragment extends InvoiceAddEditAbstractFragment {


    /**
     * Vytvoří novou instanci fragmentu pro přidání položky faktury.
     *
     * @param table  název tabulky, do které se bude ukládat
     * @param id_fak ID faktury
     * @return nová instance InvoiceAddFragment
     */
    public static InvoiceAddFragment newInstance(String table, long id_fak) {
        InvoiceAddFragment invoiceAddFragment = new InvoiceAddFragment();
        Bundle bundle = new Bundle();
        bundle.putString(TABLE, table);
        bundle.putLong(ID_FAK, id_fak);
        invoiceAddFragment.setArguments(bundle);
        return invoiceAddFragment;
    }


    /**
     * Inicializuje view a nastaví akci tlačítka pro uložení nového záznamu.
     *
     * @param view               kořenový view fragmentu
     * @param savedInstanceState uložený stav instance (může být null)
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btnSave.setOnClickListener(v -> saveData(TypeSave.ADD));
    }

}
