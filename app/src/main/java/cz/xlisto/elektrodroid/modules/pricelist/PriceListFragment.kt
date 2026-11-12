package cz.xlisto.elektrodroid.modules.pricelist

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.BundleCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import cz.xlisto.elektrodroid.R
import cz.xlisto.elektrodroid.databaze.DataPriceListSource
import cz.xlisto.elektrodroid.databaze.DataSubscriptionPointSource
import cz.xlisto.elektrodroid.dialogs.PriceListDeleteBlockedDialogFragment
import cz.xlisto.elektrodroid.dialogs.PriceListFilterDialogFragment
import cz.xlisto.elektrodroid.dialogs.YesNoDialogFragment
import cz.xlisto.elektrodroid.models.PriceListModel
import cz.xlisto.elektrodroid.models.SubscriptionPointModel
import cz.xlisto.elektrodroid.modules.pricelist.PriceListDetailFragment.Companion.PRICE_LIST_DETAIL_FRAGMENT
import cz.xlisto.elektrodroid.modules.pricelist.PriceListDetailFragment.Companion.PRICE_LIST_ID
import cz.xlisto.elektrodroid.modules.pricelist.download.DownloadPriceListFragment
import cz.xlisto.elektrodroid.modules.pricelist.pdf.PdfImportSelectionFragment
import cz.xlisto.elektrodroid.modules.pricelist.pdf.PdfParserFactory
import cz.xlisto.elektrodroid.modules.pricelist.pdf.PdfParserUtils
import cz.xlisto.elektrodroid.modules.settings.SettingsFragment
import cz.xlisto.elektrodroid.ownview.ViewHelper
import cz.xlisto.elektrodroid.shp.ShPAddEditInvoice
import cz.xlisto.elektrodroid.shp.ShPFilter
import cz.xlisto.elektrodroid.utils.DetectScreenMode
import cz.xlisto.elektrodroid.utils.FragmentChange
import cz.xlisto.elektrodroid.utils.SubscriptionPoint
import cz.xlisto.elektrodroid.utils.UIHelper
import java.util.Calendar
import java.util.concurrent.Executors

/**
 * Fragment pro zobrazení, filtrování, import a výběr ceníků elektřiny.
 *
 * Umožňuje procházet uložené ceníky, aplikovat filtry, importovat ceníky z PDF dokumentů
 * dodavatelů, mazat nevyužité či vybrané ceníky a vybírat ceníky pro porovnání nebo přiřazení k fakturám.
 */
class PriceListFragment : Fragment() {

    /**
     * Strana zobrazení v porovnávacím nebo dvoupanelovém režimu (např. v rozvržení na šířku).
     */
    enum class Side {
        LEFT, RIGHT
    }

    companion object {
        private const val TAG = "PriceListFragment"

        /** Klíč výsledku s vybraným modelem ceníku předávaným volajícímu fragmentu. */
        const val FLAG_RESULT_PRICE_LIST_FRAGMENT = "flagResultPriceListFragment"

        /** Klíč výsledkového balíčku (Bundle) při vrácení vybraného ceníku. */
        const val FLAG_PRICE_LIST_FRAGMENT = "flagPriceListFragment"

        /** Klíč pro určení strany zobrazení (vlevo/vpravo). */
        const val FLAG_SIDE = "flagSide"

        /** Klíč pro předání pole vybraných ID ceníků. */
        const val FLAG_SELECTED_IDS = "flagSelectedIds"

        /** Klíč příznaku režimu porovnávání ceníků. */
        const val FLAG_IS_COMPARE_MODE = "flagIsCompareMode"

        /** Klíč výsledku s potvrzením smazání vybraných ceníků. */
        const val FLAG_RESULT_DELETE_SELECTED_PRICELISTS = "flagResultDeleteSelectedPriceLists"

        private const val SHOW_SELECT_ITEM = "param1"
        private const val ID_SELECTED_PRICE_LIST = "param2"
        private const val ARG_ID_FRAGMENT = "idFragment"
        private const val BTN_BACK_TEXT = "btnBackText"
        private const val KEY_SELECTED_IDS = "keySelectedIds"
        private const val KEY_SELECTION_MODE_ACTIVE = "keySelectionModeActive"

        private var selectedPrice: PriceListModel? = null

        /**
         * Vytvoří novou instanci fragmentu pro výběr jednoho ceníku.
         *
         * @param selectedItem Zda se má zobrazit režim výběru položky
         * @param idSelectedPriceList ID aktuálně vybraného ceníku
         * @return Nová instance [PriceListFragment]
         */
        @JvmStatic
        fun newInstance(selectedItem: Boolean, idSelectedPriceList: Long): PriceListFragment {
            return newInstance(selectedItem, idSelectedPriceList, null, null, false)
        }

        /**
         * Vytvoří novou instanci fragmentu s určením strany zobrazení.
         *
         * @param selectedItem Zda se má zobrazit režim výběru položky
         * @param idSelectedPriceList ID aktuálně vybraného ceníku
         * @param side Strana zobrazení ([Side.LEFT] nebo [Side.RIGHT])
         * @return Nová instance [PriceListFragment]
         */
        @JvmStatic
        fun newInstance(
            selectedItem: Boolean,
            idSelectedPriceList: Long,
            side: Side?
        ): PriceListFragment {
            return newInstance(selectedItem, idSelectedPriceList, null, side, false)
        }

        /**
         * Vytvoří novou instanci fragmentu s kompletní konfigurací výběru a porovnávání.
         *
         * @param selectedItem Zda se má zobrazit režim výběru položky
         * @param idSelectedPriceList ID aktuálně vybraného ceníku
         * @param initialSelectedIds Výchozí pole ID vybraných ceníků
         * @param side Strana zobrazení
         * @param isCompareMode Zda se má fragment otevřít v režimu srovnání ceníků
         * @return Nová instance [PriceListFragment]
         */
        @JvmStatic
        fun newInstance(
            selectedItem: Boolean,
            idSelectedPriceList: Long,
            initialSelectedIds: LongArray?,
            side: Side?,
            isCompareMode: Boolean = false
        ): PriceListFragment {
            val fragment = PriceListFragment()
            val args = Bundle()
            args.putBoolean(SHOW_SELECT_ITEM, selectedItem)
            args.putLong(ID_SELECTED_PRICE_LIST, idSelectedPriceList)
            if (initialSelectedIds != null) {
                args.putLongArray(FLAG_SELECTED_IDS, initialSelectedIds)
            }
            args.putSerializable(FLAG_SIDE, side)
            args.putBoolean(FLAG_IS_COMPARE_MODE, isCompareMode)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var shpFilter: ShPFilter
    private var showSelectItem = false
    private var isSelectionModeActive = false
    private var isCompareMode = false
    private var idSelectedPriceList: Long = -1L
    private var side: Side? = null
    private var idFragment = 0

    private lateinit var rv: RecyclerView
    private lateinit var tvCountPriceItem: TextView
    private lateinit var tvSelectionCount: TextView
    private lateinit var containerSelectionActions: View
    private lateinit var btnClearSelection: Button
    private lateinit var btnCompareSelected: Button
    private lateinit var btnBack: Button
    private lateinit var fab: FloatingActionButton
    private var addMenuItem: MenuItem? = null
    private var addPdfMenuItem: MenuItem? = null

    private var subscriptionPoint: SubscriptionPointModel? = null
    private var priceListAdapter: PriceListAdapter? = null
    private var priceListModels: ArrayList<PriceListModel> = ArrayList()
    private val selectedIds: MutableSet<Long> = HashSet()
    private var isFilterOnlySelected = false
    private var isResultReturned = false

    /** Launcher pro výběr PDF souboru ceníku ze zařízení. */
    private val pdfPickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { processSelectedPdf(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shpFilter = ShPFilter(requireContext())

        arguments?.let {
            showSelectItem = it.getBoolean(SHOW_SELECT_ITEM)
            idSelectedPriceList = it.getLong(ID_SELECTED_PRICE_LIST)
            side = BundleCompat.getSerializable(it, FLAG_SIDE, Side::class.java)
            isCompareMode = it.getBoolean(FLAG_IS_COMPARE_MODE, false)
            val initialIds = it.getLongArray(FLAG_SELECTED_IDS)
            if (initialIds != null) {
                selectedIds.clear()
                initialIds.forEach { id -> selectedIds.add(id) }
            }
        }

        isSelectionModeActive = showSelectItem

        savedInstanceState?.let {
            showSelectItem = it.getBoolean(SHOW_SELECT_ITEM)
            isSelectionModeActive = it.getBoolean(KEY_SELECTION_MODE_ACTIVE, showSelectItem)
            idSelectedPriceList = it.getLong(ID_SELECTED_PRICE_LIST)
            idFragment = it.getInt(ARG_ID_FRAGMENT)
            side = BundleCompat.getSerializable(it, FLAG_SIDE, Side::class.java)
            val savedIds = it.getLongArray(KEY_SELECTED_IDS)
            if (savedIds != null) {
                selectedIds.clear()
                savedIds.forEach { id -> selectedIds.add(id) }
            }
        }

        if (idSelectedPriceList > 0 && isSelectionModeActive) {
            selectedIds.add(idSelectedPriceList)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_price_list, menu)
                addMenuItem = menu.findItem(R.id.menu_action_add_pricelist)
                addPdfMenuItem = menu.findItem(R.id.menu_action_add_pdf_pricelist)

                val isPickerMode = showSelectItem || isCompareMode

                if (isPickerMode) {
                    menu.findItem(R.id.menu_select_mode_pricelist)?.isVisible = false
                    menu.findItem(R.id.menu_delete_unused_pricelist)?.isVisible = false
                    menu.findItem(R.id.menu_compare_selected_pricelists)?.isVisible = false
                    menu.findItem(R.id.menu_delete_selected_pricelists)?.isVisible = false
                } else {
                    menu.findItem(R.id.menu_select_mode_pricelist)?.isVisible = true
                    menu.findItem(R.id.menu_delete_unused_pricelist)?.isVisible = true
                    menu.findItem(R.id.menu_compare_selected_pricelists)?.isVisible =
                        isSelectionModeActive
                    menu.findItem(R.id.menu_delete_selected_pricelists)?.isVisible =
                        isSelectionModeActive
                }

                UIHelper.showButtons(
                    null,
                    fab,
                    addMenuItem,
                    requireActivity(),
                    rv,
                    false
                )
                if (addMenuItem != null && addPdfMenuItem != null) {
                    addPdfMenuItem?.isVisible = addMenuItem?.isVisible == true
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    android.R.id.home -> {
                        confirmAndReturnResult()
                        true
                    }

                    R.id.menu_action_add_pricelist -> {
                        addNewPriceList()
                        true
                    }

                    R.id.menu_action_add_pdf_pricelist -> {
                        pdfPickerLauncher.launch("application/pdf")
                        true
                    }

                    R.id.menu_filter_pricelist -> {
                        PriceListFilterDialogFragment.newInstance()
                            .show(requireActivity().supportFragmentManager, TAG)
                        true
                    }

                    R.id.menu_select_mode_pricelist -> {
                        toggleSelectionMode()
                        true
                    }

                    R.id.menu_compare_selected_pricelists -> {
                        startPriceListComparison()
                        true
                    }

                    R.id.menu_delete_selected_pricelists -> {
                        confirmDeleteSelectedPriceLists()
                        true
                    }

                    R.id.menu_delete_unused_pricelist -> {
                        val yesNoDialogFragment = YesNoDialogFragment.newInstance(
                            getString(R.string.delete_unused_pricelist),
                            YesNoDialogFragment.FLAG_RESULT_DIALOG_FRAGMENT,
                            getString(R.string.delete_unused_pricelist_content)
                        )
                        yesNoDialogFragment.show(
                            requireActivity().supportFragmentManager,
                            YesNoDialogFragment.TAG
                        )
                        true
                    }

                    R.id.menu_action_download_pricelists -> {
                        val downloadFragment = DownloadPriceListFragment.newInstance()
                        FragmentChange.replace(
                            requireActivity(),
                            downloadFragment,
                            FragmentChange.Transaction.MOVE,
                            true
                        )
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        return inflater.inflate(R.layout.fragment_price_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().invalidateOptionsMenu()

        fab = view.findViewById(R.id.fab)
        rv = view.findViewById(R.id.rv_price_list)
        tvCountPriceItem = view.findViewById(R.id.tvPocetMist)
        tvSelectionCount = view.findViewById(R.id.tvSelectionCount)
        containerSelectionActions = view.findViewById(R.id.containerSelectionActions)
        btnClearSelection = view.findViewById(R.id.btnClearSelection)
        btnCompareSelected = view.findViewById(R.id.btnCompareSelected)
        btnBack = view.findViewById(R.id.btnBack)

        btnClearSelection.setOnClickListener {
            selectedIds.clear()
            priceListAdapter?.clearSelection()
            if (isFilterOnlySelected) {
                loadOnlySelectedPriceLists()
            } else {
                updateSelectionUiState()
            }
        }

        btnCompareSelected.setOnClickListener {
            isFilterOnlySelected = !isFilterOnlySelected
            if (isFilterOnlySelected) {
                loadOnlySelectedPriceLists()
            } else {
                onLoadData()
                setAdapter()
            }
        }

        btnBack.setOnClickListener {
            confirmAndReturnResult()
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    confirmAndReturnResult()
                }
            })

        // Posluchač při potvrzení filtru ceníku
        requireActivity().supportFragmentManager.setFragmentResultListener(
            PriceListFilterDialogFragment.FLAG_RESULT_FILTER_DIALOG_FRAGMENT, this
        ) { _, result ->
            if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                onLoadData()
                setAdapter()
                priceListAdapter?.setHideButtons()
                showDetailPriceFragment(false)
            }
        }

        // Posluchač při potvrzení smazání jednoho ceníku
        requireActivity().supportFragmentManager.setFragmentResultListener(
            PriceListAdapter.FLAG_DIALOG_FRAGMENT_DELETE_PRICE_LIST, this
        ) { _, result ->
            if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                priceListAdapter?.deleteItemPrice()
                onLoadData()
                priceListAdapter?.setHideButtons()
                idSelectedPriceList = -1L
                showDetailPriceFragment(false)
            }
        }

        // Posluchač při potvrzení smazání nevyužitých ceníků
        requireActivity().supportFragmentManager.setFragmentResultListener(
            YesNoDialogFragment.FLAG_RESULT_DIALOG_FRAGMENT, this
        ) { _, result ->
            if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                val dataPriceListSource = DataPriceListSource(requireActivity())
                dataPriceListSource.open()
                dataPriceListSource.deleteUnusedPriceList()
                dataPriceListSource.close()
                onLoadData()
                setAdapter()
                priceListAdapter?.setHideButtons()
                idSelectedPriceList = -1L
                showDetailPriceFragment(false)
            }
        }

        // Posluchač při potvrzení smazání vybraných ceníků
        requireActivity().supportFragmentManager.setFragmentResultListener(
            FLAG_RESULT_DELETE_SELECTED_PRICELISTS, this
        ) { _, result ->
            if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                performDeleteSelectedPriceLists()
            }
        }

        // Posluchač při zavření dialogového okna nastavení
        requireActivity().supportFragmentManager.setFragmentResultListener(
            SettingsFragment.FLAG_UPDATE_SETTINGS_FOR_FRAGMENT, this
        ) { _, _ ->
            UIHelper.showButtons(null, fab, addMenuItem, requireActivity(), rv, false)
            requireActivity().invalidateOptionsMenu()
            btnShowBack()
        }

        onLoadData()
        setAdapter()
        showDetailPriceFragment(true)

        if (savedInstanceState != null) {
            btnBack.text = savedInstanceState.getString(BTN_BACK_TEXT, getString(R.string.zpet))
        }
    }

    override fun onResume() {
        super.onResume()
        UIHelper.showButtons(null, fab, addMenuItem, requireActivity(), rv, false)
        btnShowBack()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(ID_SELECTED_PRICE_LIST, idSelectedPriceList)
        outState.putBoolean(SHOW_SELECT_ITEM, showSelectItem)
        outState.putBoolean(KEY_SELECTION_MODE_ACTIVE, isSelectionModeActive)
        outState.putInt(ARG_ID_FRAGMENT, idFragment)
        outState.putSerializable(FLAG_SIDE, side)
        outState.putLongArray(KEY_SELECTED_IDS, selectedIds.toLongArray())
        outState.putString(BTN_BACK_TEXT, btnBack.text.toString())
    }

    /**
     * Načte seznam ceníků z databáze podle aplikovaných filtrů a aktualizuje počítadlo ceníků.
     */
    fun onLoadData() {
        btnBack.text = getString(R.string.zpet)
        subscriptionPoint = SubscriptionPoint.load(requireActivity())

        val rada = shpFilter.get(ShPFilter.RADA, ShPFilter.DEFAULT)
        val produkt = shpFilter.get(ShPFilter.PRODUKT, ShPFilter.DEFAULT)
        val sazba = shpFilter.get(ShPFilter.SAZBA, ShPFilter.DEFAULT)
        val company = shpFilter.get(ShPFilter.COMPANY, ShPFilter.DEFAULT)
        val area = shpFilter.get(ShPFilter.AREA, ShPFilter.DEFAULT)
        var dateStart = "%"
        var dateEnd = "%"

        val filterStart = shpFilter.get(ShPFilter.DATE_START, ShPFilter.DEFAULT)
        if (filterStart != "%" && filterStart.isNotEmpty()) {
            val calStart = ViewHelper.parseCalendarFromString(filterStart)
            calStart.set(Calendar.HOUR_OF_DAY, 0)
            calStart.set(Calendar.MINUTE, 0)
            calStart.set(Calendar.SECOND, 0)
            calStart.set(Calendar.MILLISECOND, 0)
            dateStart = calStart.timeInMillis.toString()
        }

        val filterEnd = shpFilter.get(ShPFilter.DATE_END, ShPFilter.DEFAULT)
        if (filterEnd != "%" && filterEnd.isNotEmpty()) {
            val calEnd = ViewHelper.parseCalendarFromString(filterEnd)
            calEnd.set(Calendar.HOUR_OF_DAY, 23)
            calEnd.set(Calendar.MINUTE, 59)
            calEnd.set(Calendar.SECOND, 59)
            calEnd.set(Calendar.MILLISECOND, 999)
            dateEnd = calEnd.timeInMillis.toString()
        }

        val dataPriceListSource = DataPriceListSource(requireContext())
        dataPriceListSource.open()
        priceListModels = dataPriceListSource.readPriceList(
            rada,
            produkt,
            sazba,
            company,
            area,
            dateStart,
            dateEnd
        )
        val totalCountPriceList = dataPriceListSource.countPriceItems()
        dataPriceListSource.close()

        tvCountPriceItem.text =
            getString(R.string.zobrazeno_z_ceniku, priceListModels.size, totalCountPriceList)
        updateSelectionUiState()
    }

    /**
     * Přepne režim výběru více položek a aktualizuje stav akční lišty a ovládacích prvků.
     */
    private fun toggleSelectionMode() {
        isSelectionModeActive = !isSelectionModeActive
        priceListAdapter?.setSelectionMode(isSelectionModeActive)
        requireActivity().invalidateOptionsMenu()
        updateSelectionUiState()
    }

    /**
     * Nastaví [PriceListAdapter] pro [RecyclerView].
     */
    private fun setAdapter() {
        priceListAdapter = PriceListAdapter(
            items = priceListModels,
            subscriptionPoint = subscriptionPoint,
            isSelectionMode = isSelectionModeActive,
            idSelectedPriceList = idSelectedPriceList,
            onClickItemListener = { priceList ->
                selectedPrice = priceList
                if (priceList != null) {
                    btnBack.text = getString(R.string.vybrat)
                    idSelectedPriceList = priceList.id
                    if (!isSelectionModeActive) {
                        val bundle = Bundle()
                        bundle.putLong(PRICE_LIST_ID, idSelectedPriceList)
                        parentFragmentManager.setFragmentResult(PRICE_LIST_DETAIL_FRAGMENT, bundle)
                        showDetailPriceFragment(false)
                    }
                }
            },
            recyclerView = rv,
            onSelectionChangedListener = { ids ->
                selectedIds.clear()
                selectedIds.addAll(ids)
                updateSelectionUiState()
            },
            isCompareMode = isCompareMode
        )

        // Sync selectedIds with adapter
        priceListAdapter?.selectedIds?.clear()
        priceListAdapter?.selectedIds?.addAll(selectedIds)

        rv.adapter = priceListAdapter
        rv.layoutManager = LinearLayoutManager(requireContext())
        updateSelectionUiState()
    }

    /**
     * Načte a zobrazí v seznamu pouze ceníky odpovídající vybraným ID.
     */
    private fun loadOnlySelectedPriceLists() {
        val dataPriceListSource = DataPriceListSource(requireContext())
        dataPriceListSource.open()
        val selectedModels = ArrayList<PriceListModel>()
        selectedIds.forEach { id ->
            val pl = dataPriceListSource.readPrice(id)
            if (pl != null && pl.id > 0) {
                selectedModels.add(pl)
            }
        }
        dataPriceListSource.close()

        priceListModels = selectedModels
        tvCountPriceItem.text = getString(R.string.zobrazeno_vybranych_ceniku, selectedModels.size)
        setAdapter()
    }

    /**
     * Aktualizuje stav informační lišty vybraných ceníků, textu počítadla a tlačítka akce.
     */
    private fun updateSelectionUiState() {
        val totalSelected = selectedIds.size
        val visibleSelected = priceListModels.count { selectedIds.contains(it.id) }
        val hiddenSelected = totalSelected - visibleSelected

        if (isSelectionModeActive) {
            containerSelectionActions.visibility = View.VISIBLE
            btnCompareSelected.isEnabled = totalSelected > 0 || isFilterOnlySelected

            if (isFilterOnlySelected) {
                btnCompareSelected.text = getString(R.string.zobrazit_vse)
            } else {
                btnCompareSelected.text = getString(R.string.zobrazit_vybrane)
            }

            if (hiddenSelected > 0 && !isFilterOnlySelected) {
                tvSelectionCount.text =
                    getString(R.string.vybrano_ceniku_count_skryte, totalSelected, hiddenSelected)
            } else {
                tvSelectionCount.text = getString(R.string.vybrano_ceniku_count, totalSelected)
            }
        } else {
            containerSelectionActions.visibility = View.GONE
        }
    }

    /**
     * Odešle balíček s výsledkem výběru ceníku do [parentFragmentManager] a uloží ID do preferencí.
     */
    private fun sendResultBundle() {
        if (isResultReturned) return
        isResultReturned = true

        val shPAddEditInvoice = ShPAddEditInvoice(context)
        shPAddEditInvoice.set(ShPAddEditInvoice.SELECTED_ID_PRICE, idSelectedPriceList)

        val bundle = Bundle()
        bundle.putSerializable(FLAG_RESULT_PRICE_LIST_FRAGMENT, selectedPrice)
        bundle.putSerializable(FLAG_SIDE, side)
        bundle.putLongArray(FLAG_SELECTED_IDS, selectedIds.toLongArray())

        parentFragmentManager.setFragmentResult(FLAG_PRICE_LIST_FRAGMENT, bundle)
    }

    /**
     * Potvrdí výběr ceníků a předá výsledek volajícímu fragmentu nebo aktivitě.
     */
    private fun confirmAndReturnResult() {
        sendResultBundle()
        parentFragmentManager.popBackStack()
    }

    override fun onDestroyView() {
        if (!isResultReturned && (showSelectItem || isCompareMode)) {
            sendResultBundle()
        }
        super.onDestroyView()
    }

    /**
     * Otevírá fragment pro srovnání vybraných ceníků podle [selectedIds].
     */
    private fun startPriceListComparison() {
        if (selectedIds.isEmpty()) {
            Toast.makeText(
                requireContext(),
                R.string.no_pricelists_selected_for_compare,
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val compareBoxFragment = PriceListCompareBoxFragment.newInstance(selectedIds.toLongArray())
        FragmentChange.replace(
            requireActivity(),
            compareBoxFragment,
            FragmentChange.Transaction.MOVE,
            true
        )
    }

    /**
     * Zobrazí dialog [YesNoDialogFragment] s dotazem na smazání vybraných ceníků.
     */
    private fun confirmDeleteSelectedPriceLists() {
        if (selectedIds.isEmpty()) {
            Toast.makeText(
                requireContext(),
                R.string.no_pricelists_selected_for_delete,
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val yesNoDialogFragment = YesNoDialogFragment.newInstance(
            getString(R.string.delete_selected_pricelists_title),
            FLAG_RESULT_DELETE_SELECTED_PRICELISTS,
            getString(R.string.delete_selected_pricelists_message, selectedIds.size)
        )
        yesNoDialogFragment.show(
            requireActivity().supportFragmentManager,
            YesNoDialogFragment.TAG
        )
    }

    /**
     * Provede smazání vybraných ceníků s kontrolou na jejich vazbu v odečtech a fakturách.
     *
     * Pokud jsou ceníky používány, zobrazí se varovný dialog [PriceListDeleteBlockedDialogFragment].
     */
    private fun performDeleteSelectedPriceLists() {
        if (selectedIds.isEmpty()) return

        val dataSubscriptionPointSource = DataSubscriptionPointSource(requireContext())
        dataSubscriptionPointSource.open()
        val subscriptionPointModels: ArrayList<SubscriptionPointModel> =
            dataSubscriptionPointSource.loadSubscriptionPoints()

        val dataPriceListSource = DataPriceListSource(requireContext())
        dataPriceListSource.open()

        var deletedCount = 0
        var blockedCount = 0
        var totalTed = 0
        var totalFak = 0
        var totalMon = 0

        val idsToDelete = ArrayList(selectedIds)

        for (itemId in idsToDelete) {
            var pricesTED = 0
            var pricesFAK = 0
            var pricesMON = 0

            for (subscriptionPointModel in subscriptionPointModels) {
                pricesTED += dataSubscriptionPointSource.countPriceItems(
                    subscriptionPointModel.tableTED,
                    itemId
                )
                pricesFAK += dataSubscriptionPointSource.countPriceItems(
                    subscriptionPointModel.tableFAK,
                    itemId
                )
                pricesMON += dataSubscriptionPointSource.countPriceItems(
                    subscriptionPointModel.tableO,
                    itemId
                )
            }

            if (pricesTED > 0 || pricesFAK > 0 || pricesMON > 0) {
                blockedCount++
                totalTed += pricesTED
                totalFak += pricesFAK
                totalMon += pricesMON
            } else {
                dataPriceListSource.deletePriceList(itemId)
                selectedIds.remove(itemId)
                deletedCount++
            }
        }

        dataPriceListSource.close()
        dataSubscriptionPointSource.close()

        onLoadData()
        setAdapter()

        if (blockedCount > 0) {
            PriceListDeleteBlockedDialogFragment.newInstance(totalTed, totalFak, totalMon)
                .show(requireActivity().supportFragmentManager, "priceListDeleteBlockedDialog")
            Toast.makeText(
                requireContext(),
                getString(R.string.deleted_some_blocked_some, deletedCount, blockedCount),
                Toast.LENGTH_LONG
            ).show()
        } else if (deletedCount > 0) {
            Toast.makeText(
                requireContext(),
                getString(R.string.deleted_pricelists_success, deletedCount),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Zobrazí nebo aktualizuje detail ceníku [PriceListDetailFragment] v rozvržení pro režim na šířku (landscape).
     *
     * @param showAfterRotation Zda se má provést obnova detailu po otočení obrazovky
     */
    private fun showDetailPriceFragment(showAfterRotation: Boolean) {
        var fragment: PriceListDetailFragment?
        if (showAfterRotation && idFragment != 0) {
            fragment =
                requireActivity().supportFragmentManager.findFragmentById(idFragment) as? PriceListDetailFragment
            if (fragment != null) {
                requireActivity().supportFragmentManager.beginTransaction().remove(fragment)
                    .commit()
            }
            idFragment = 0
        }
        if (DetectScreenMode.isLandscape(requireActivity())) {
            if (idSelectedPriceList != -1L) {
                if (idFragment == 0) {
                    fragment = PriceListDetailFragment.newInstance(idSelectedPriceList, true)
                    childFragmentManager.beginTransaction()
                        .replace(R.id.fl_price_list_right, fragment)
                        .commit()
                    idFragment = fragment.id
                } else {
                    fragment =
                        childFragmentManager.findFragmentById(idFragment) as? PriceListDetailFragment
                    fragment?.loadPrice(idSelectedPriceList)
                }
            } else {
                if (idFragment != 0) {
                    fragment =
                        requireActivity().supportFragmentManager.findFragmentById(idFragment) as? PriceListDetailFragment
                    if (fragment != null) {
                        childFragmentManager.beginTransaction().remove(fragment).commit()
                        idFragment = 0
                    }
                }
            }
        }
    }

    /**
     * Řídí viditelnost tlačítka Zpět a plovoucího tlačítka FAB podle režimu výběru.
     */
    private fun btnShowBack() {
        btnBack.visibility = View.GONE
        if (showSelectItem) {
            fab.visibility = View.GONE
        }
    }

    /**
     * Otevře fragment [PriceListAddFragment] pro ruční zadání nového ceníku.
     */
    private fun addNewPriceList() {
        val priceListAddFragment = PriceListAddFragment.newInstance()
        FragmentChange.replace(
            requireActivity(),
            priceListAddFragment,
            FragmentChange.Transaction.MOVE,
            true
        )
    }

    /**
     * Načte a rozparsuje vybraný PDF soubor na pozadí a při úspěchu otevře [PdfImportSelectionFragment].
     *
     * @param uri URI vybraného PDF dokumentu
     */
    private fun processSelectedPdf(uri: Uri) {
        Executors.newSingleThreadExecutor().execute {
            try {
                val pdfText = PdfParserUtils.extractTextFromPdf(requireContext(), uri)
                val priceLists = PdfParserFactory.parsePdf(requireContext(), pdfText)

                requireActivity().runOnUiThread {
                    if (priceLists.isNotEmpty()) {
                        val selectionFragment = PdfImportSelectionFragment.newInstance(priceLists)
                        FragmentChange.replace(
                            requireActivity(),
                            selectionFragment,
                            FragmentChange.Transaction.MOVE,
                            true
                        )
                    } else {
                        Toast.makeText(
                            requireContext(),
                            R.string.pdf_import_unsupported_supplier,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.pdf_import_error, e.message ?: ""),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
