package cz.xlisto.elektrodroid.modules.pricelist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import cz.xlisto.elektrodroid.R
import cz.xlisto.elektrodroid.databaze.DataPriceListSource
import cz.xlisto.elektrodroid.format.DecimalFormatHelper.df2
import cz.xlisto.elektrodroid.models.PriceListRegulBuilder
import cz.xlisto.elektrodroid.modules.pricelist.pdf.PdfParserUtils
import cz.xlisto.elektrodroid.ownview.LabelPriceDetail
import cz.xlisto.elektrodroid.ownview.ViewHelper
import cz.xlisto.elektrodroid.utils.DetectScreenMode
import java.util.Calendar

/**
 * Fragment pro zobrazení detailů ceníku elektřiny.
 *
 * Přehledně zobrazuje všechny složky ceníku včetně obchodní dodávky (VT, NT, stálý plat),
 * distribuční sazby a jističů, regulovaných poplatků (systémové služby, OTE, POZE),
 * daní (daň z elektřiny, DPH) a aplikovatelných cenových stropů a poznámek.
 */
class PriceListDetailFragment : Fragment() {

    companion object {
        /** Klíč příznaku výsledku pro mezigrafickou komunikaci mezi fragmenty. */
        const val PRICE_LIST_DETAIL_FRAGMENT = "price_list_detail_fragment"

        /** Klíč předávaného ID ceníku ve výsledkovém balíčku. */
        const val PRICE_LIST_ID = "price_list_id"

        private const val PRICE_ID = "price_list_id"
        private const val SHOW_IN_FRAGMENT = "show_in_fragment"

        /**
         * Vytvoří novou instanci fragmentu pro zobrazení detailu ceníku.
         *
         * @param priceListId ID ceníku v databázi
         * @return Nová instance [PriceListDetailFragment]
         */
        @JvmStatic
        fun newInstance(priceListId: Long): PriceListDetailFragment {
            return newInstance(priceListId, showInFragment = false)
        }

        /**
         * Vytvoří novou instanci fragmentu s možností přizpůsobení rozvržení pro vnoření do jiného fragmentu.
         *
         * @param priceListId ID ceníku v databázi
         * @param showInFragment `true` pokud se fragment zobrazuje vnořený (např. v režimu na šířku), jinak `false`
         * @return Nová instance [PriceListDetailFragment]
         */
        @JvmStatic
        fun newInstance(priceListId: Long, showInFragment: Boolean): PriceListDetailFragment {
            val fragment = PriceListDetailFragment()
            val args = Bundle()
            args.putLong(PRICE_ID, priceListId)
            args.putBoolean(SHOW_IN_FRAGMENT, showInFragment)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var ldnDate: LabelPriceDetail
    private lateinit var ldnSeries: LabelPriceDetail
    private lateinit var ldnProduct: LabelPriceDetail
    private lateinit var ldnRate: LabelPriceDetail
    private lateinit var ldnCompany: LabelPriceDetail
    private lateinit var ldnArea: LabelPriceDetail
    private lateinit var ldnVtSupply: LabelPriceDetail
    private lateinit var ldnNtSupply: LabelPriceDetail
    private lateinit var ldnMonthlyPayment: LabelPriceDetail
    private lateinit var ldnVtDistribution: LabelPriceDetail
    private lateinit var ldnNtDistribution: LabelPriceDetail
    private lateinit var ldnJ0: LabelPriceDetail
    private lateinit var ldnJ1: LabelPriceDetail
    private lateinit var ldnJ2: LabelPriceDetail
    private lateinit var ldnJ3: LabelPriceDetail
    private lateinit var ldnJ4: LabelPriceDetail
    private lateinit var ldnJ5: LabelPriceDetail
    private lateinit var ldnJ6: LabelPriceDetail
    private lateinit var ldnJ7: LabelPriceDetail
    private lateinit var ldnJ8: LabelPriceDetail
    private lateinit var ldnJ9: LabelPriceDetail
    private lateinit var ldnJ10: LabelPriceDetail
    private lateinit var ldnJ11: LabelPriceDetail
    private lateinit var ldnJ12: LabelPriceDetail
    private lateinit var ldnJ13: LabelPriceDetail
    private lateinit var ldnJ14: LabelPriceDetail
    private lateinit var ldnSystemServices: LabelPriceDetail
    private lateinit var ldnOte: LabelPriceDetail
    private lateinit var ldnPoze1: LabelPriceDetail
    private lateinit var ldnPoze2: LabelPriceDetail
    private lateinit var ldnTax: LabelPriceDetail
    private lateinit var ldnVat: LabelPriceDetail

    private lateinit var tvNote: TextView
    private lateinit var tvNoteVt: TextView
    private lateinit var tvNoteNt: TextView
    private lateinit var tvNotePayment: TextView
    private lateinit var tvNotePoze1: TextView
    private lateinit var tvNotePoze2: TextView
    private lateinit var tvNoteOte: TextView
    private lateinit var containerBreakers: android.widget.LinearLayout

    private var priceId: Long = -1L
    private var showInFragment = false

    /**
     * Načte předané argumenty fragmentu (ID ceníku a příznak vnořeného zobrazení).
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            priceId = it.getLong(PRICE_ID)
            showInFragment = it.getBoolean(SHOW_IN_FRAGMENT)
        }
    }

    /**
     * Vytvoří a vrátí strom pohledů fragmentu (v závislosti na orientaci obrazovky a vnoření),
     * inicializuje komponenty UI a zaregistruje posluchač výsledků pro zobrazení zvoleného ceníku.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        var v = inflater.inflate(R.layout.fragment_price_list_detail, container, false)
        if (showInFragment && DetectScreenMode.isLandscape(requireActivity())) {
            v = inflater.inflate(R.layout.fragment_price_list_detail_infragment, container, false)
        }

        ldnDate = v.findViewById(R.id.ldnPlatnost)
        ldnSeries = v.findViewById(R.id.ldnRada)
        ldnProduct = v.findViewById(R.id.ldnProdukt)
        ldnRate = v.findViewById(R.id.ldnSazba)
        ldnCompany = v.findViewById(R.id.ldnDodavatel)
        ldnArea = v.findViewById(R.id.ldnUzemi)
        tvNote = v.findViewById(R.id.tvPoznamkaDetail)

        ldnVtSupply = v.findViewById(R.id.ldn_VT_dodavka)
        ldnNtSupply = v.findViewById(R.id.ldn_NT_dodavka)
        ldnMonthlyPayment = v.findViewById(R.id.ldn_staly_plat)
        tvNoteVt = v.findViewById(R.id.tvPoznamkaVT)
        tvNoteNt = v.findViewById(R.id.tvPoznamkaNT)
        tvNotePayment = v.findViewById(R.id.tvPoznamkaPlat)
        tvNotePoze1 = v.findViewById(R.id.tvPoznamkaPOZE1)
        tvNotePoze2 = v.findViewById(R.id.tvPoznamkaPOZE2)
        tvNoteOte = v.findViewById(R.id.tvPoznamkaOTE)

        ldnVtDistribution = v.findViewById(R.id.ldn_VT_distribuce)
        ldnNtDistribution = v.findViewById(R.id.ldn_NT_distribuce)

        ldnJ0 = v.findViewById(R.id.ldn_J0)
        ldnJ1 = v.findViewById(R.id.ldn_J1)
        ldnJ2 = v.findViewById(R.id.ldn_J2)
        ldnJ3 = v.findViewById(R.id.ldn_J3)
        ldnJ4 = v.findViewById(R.id.ldn_J4)
        ldnJ5 = v.findViewById(R.id.ldn_J5)
        ldnJ6 = v.findViewById(R.id.ldn_J6)
        ldnJ7 = v.findViewById(R.id.ldn_J7)
        ldnJ8 = v.findViewById(R.id.ldn_J8)
        ldnJ9 = v.findViewById(R.id.ldn_J9)
        ldnJ10 = v.findViewById(R.id.ldn_J10)
        ldnJ11 = v.findViewById(R.id.ldn_J11)
        ldnJ12 = v.findViewById(R.id.ldn_J12)
        ldnJ13 = v.findViewById(R.id.ldn_J13)
        ldnJ14 = v.findViewById(R.id.ldn_J14)

        containerBreakers = v.findViewById(R.id.containerJistice)

        ldnSystemServices = v.findViewById(R.id.ldn_systemove_sluzby)
        ldnOte = v.findViewById(R.id.ldn_OTE)
        ldnPoze1 = v.findViewById(R.id.ldn_POZE1)
        ldnPoze2 = v.findViewById(R.id.ldn_POZE2)

        ldnTax = v.findViewById(R.id.ldn_dan)
        ldnVat = v.findViewById(R.id.ldn_DPH)

        loadPrice(priceId)

        parentFragmentManager.setFragmentResultListener(
            PRICE_LIST_DETAIL_FRAGMENT,
            this
        ) { _, result ->
            val newPriceId = result.getLong(PRICE_LIST_ID)
            loadPrice(newPriceId)
        }

        return v
    }

    /**
     * Načte a zobrazí detaily ceníku na základě zadaného ID ceníku.
     *
     * @param priceId ID ceníku k načtení z databáze
     */
    fun loadPrice(priceId: Long) {
        if (priceId <= 0) return
        this.priceId = priceId

        val dataPriceListSource = DataPriceListSource(requireActivity())
        dataPriceListSource.open()
        val priceList = dataPriceListSource.readPrice(priceId)
        dataPriceListSource.close()

        if (priceList == null) return

        val dateStart = priceList.validFrom
        val dateEnd = priceList.validTo

        ldnDate.price =
            "${ViewHelper.convertLongToDate(dateStart)} - ${
                ViewHelper.convertLongToDate(
                    dateEnd
                )
            }"

        ldnSeries.price = priceList.series
        ldnProduct.price = priceList.product
        ldnRate.price = priceList.rate
        ldnCompany.price = priceList.company
        ldnArea.price = PdfParserUtils.getFullDistribuceName(priceList.distribution)

        ldnVtSupply.price = df2.format(priceList.priceVT)
        ldnNtSupply.price = df2.format(priceList.priceNT)
        ldnMonthlyPayment.price = df2.format(priceList.monthlyPayment)

        ldnVtDistribution.price = df2.format(priceList.distVT)
        ldnNtDistribution.price = df2.format(priceList.distNT)

        val allBreakers = listOf(
            ldnJ0 to priceList.j0,
            ldnJ1 to priceList.j1,
            ldnJ2 to priceList.j2,
            ldnJ3 to priceList.j3,
            ldnJ4 to priceList.j4,
            ldnJ5 to priceList.j5,
            ldnJ6 to priceList.j6,
            ldnJ7 to priceList.j7,
            ldnJ10 to priceList.j10,
            ldnJ11 to priceList.j11,
            ldnJ12 to priceList.j12,
            ldnJ13 to priceList.j13,
            ldnJ14 to priceList.j14,
            ldnJ8 to priceList.j8,
            ldnJ9 to priceList.j9
        )

        val activeViews = allBreakers
            .filter { it.second != 0.0 }
            .map { (view, value) ->
                view.price = df2.format(value)
                view
            }

        setupBreakersGrid(containerBreakers, activeViews)

        ldnSystemServices.price = df2.format(priceList.systemServices)
        ldnOte.price = df2.format(priceList.activity)
        ldnPoze1.price = df2.format(priceList.poze1)
        ldnPoze2.price = df2.format(priceList.poze2)

        ldnTax.price = df2.format(priceList.tax)
        ldnVat.price = df2.format(priceList.vat)

        val calendar = ViewHelper.parseCalendarFromString(ldnDate.price)
        val year = calendar[Calendar.YEAR]
        if (year < 2016) {
            ldnOte.price = df2.format(priceList.ote)
            ldnOte.label = getString(R.string.cinnost_ote)
            ldnOte.setItem(getString(R.string.kc_MWh))
            ldnPoze2.price = df2.format(priceList.oze)
            ldnPoze2.label = getString(R.string.podpora_vykupu_el_z_oze_kvet_a_dz)
            ldnPoze1.visibility = View.GONE
        }

        // Přenastavení ceníku pro rok 2024 (1719784800000 = 1.7.2024)
        if (dateStart >= 1719784800000L) {
            ldnOte.label = getString(R.string.provoz_nesitove_infrastruktury)
            ldnOte.setItem(getString(R.string.kc_om))
        }

        val transition2024 = 1719784800000L // 1.7.2024
        val transition2025 = 1756677600000L // 1.9.2025
        when (year) {
            2024 -> {
                if (dateStart < transition2024 && dateEnd >= transition2024) {
                    tvNoteOte.text = getText(R.string.provoz_nesitove_infrastruktury_poznamka)
                    tvNoteOte.visibility = View.VISIBLE
                } else {
                    tvNoteOte.visibility = View.GONE
                }
            }

            2025 -> {
                if (dateStart < transition2025 && dateEnd >= transition2025) {
                    tvNoteOte.text = getText(R.string.nova_cena_infrastruktua_poznamka)
                    tvNoteOte.visibility = View.VISIBLE
                } else {
                    tvNoteOte.visibility = View.GONE
                }
            }

            else -> {
                tvNoteOte.visibility = View.GONE
            }
        }

        // Poznámky a cenové stropy
        val priceListRegulBuilder = PriceListRegulBuilder(priceList, year)
        val note = priceListRegulBuilder.getNotes(requireActivity())
        val maxVT = priceListRegulBuilder.maxVT
        val maxNT = priceListRegulBuilder.maxNT
        val maxPayment = priceListRegulBuilder.maxPayment
        val maxPOZE1 = priceListRegulBuilder.maxPOZE

        if (priceListRegulBuilder.isRegulPrice) {
            tvNote.text = note
            tvNote.visibility = View.VISIBLE

            // Zobrazení stropů přímo pod příslušným sloupcem (VT, NT, Měsíční plat)
            setNote(tvNoteVt, maxVT)
            setNote(tvNoteNt, maxNT)
            if (priceList.distNT == 0.0) {
                tvNoteNt.visibility = View.GONE
            }
            setNote(tvNotePayment, maxPayment)

            // Zrušení POZE se zobrazuje pouze jednou pod sekcí Ostatní služby
            setNote(tvNotePoze1, maxPOZE1)
            tvNotePoze2.visibility = View.GONE
        } else {
            tvNote.visibility = View.GONE
            tvNoteVt.visibility = View.GONE
            tvNoteNt.visibility = View.GONE
            tvNotePayment.visibility = View.GONE
            tvNotePoze1.visibility = View.GONE
            tvNotePoze2.visibility = View.GONE
        }
    }

    /**
     * Nastaví text poznámky do zadaného [TextView] a upraví jeho viditelnost podle toho, zda je text neprázdný.
     *
     * @param tv Komponenta [TextView] pro zobrazení poznámky
     * @param s Text poznámky k nastavení (nebo `null`/prázdný pro skrytí)
     */
    private fun setNote(tv: TextView, s: String?) {
        if (!s.isNullOrEmpty()) {
            tv.visibility = View.VISIBLE
            tv.text = s
        } else {
            tv.visibility = View.GONE
        }
    }

    /**
     * Dynamicky uspořádá seznam aktivních prvků jističů do dvou sloupců bez prázdných mezer.
     *
     * @param container Kontejnerové rozvržení [android.widget.LinearLayout] pro vložení řádků jističů
     * @param visibleItems Seznam aktivních komponent [LabelPriceDetail] s nenulovou cenou jističe
     */
    private fun setupBreakersGrid(
        container: android.widget.LinearLayout,
        visibleItems: List<LabelPriceDetail>
    ) {
        container.removeAllViews()
        val context = container.context
        val density = context.resources.displayMetrics.density
        val margin2dp = (2 * density).toInt()

        for (i in visibleItems.indices step 2) {
            val rowLayout = android.widget.LinearLayout(context).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = margin2dp
                    bottomMargin = margin2dp
                }
                orientation = android.widget.LinearLayout.HORIZONTAL
            }

            val item1 = visibleItems[i]
            (item1.parent as? ViewGroup)?.removeView(item1)
            item1.layoutParams = android.widget.LinearLayout.LayoutParams(
                0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            item1.visibility = View.VISIBLE
            rowLayout.addView(item1)

            if (i + 1 < visibleItems.size) {
                val item2 = visibleItems[i + 1]
                (item2.parent as? ViewGroup)?.removeView(item2)
                item2.layoutParams = android.widget.LinearLayout.LayoutParams(
                    0,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                item2.visibility = View.VISIBLE
                rowLayout.addView(item2)
            } else {
                val emptyView = View(context).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        0,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                }
                rowLayout.addView(emptyView)
            }

            container.addView(rowLayout)
        }
    }
}
