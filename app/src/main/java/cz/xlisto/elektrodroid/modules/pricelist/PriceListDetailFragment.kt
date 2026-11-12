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
import cz.xlisto.elektrodroid.ownview.LabelPriceDetail
import cz.xlisto.elektrodroid.ownview.ViewHelper
import cz.xlisto.elektrodroid.utils.DetectScreenMode
import java.util.Calendar

/**
 * Fragment pro zobrazení detailů ceníku elektřiny.
 */
class PriceListDetailFragment : Fragment() {

    companion object {
        const val PRICE_LIST_DETAIL_FRAGMENT = "price_list_detail_fragment"
        const val PRICE_LIST_ID = "price_list_id"
        private const val PRICE_ID = "price_list_id"
        private const val SHOW_IN_FRAGMENT = "show_in_fragment"

        @JvmStatic
        fun newInstance(priceListId: Long): PriceListDetailFragment {
            return newInstance(priceListId, showInFragment = false)
        }

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

    private lateinit var ldnDatum: LabelPriceDetail
    private lateinit var ldnRada: LabelPriceDetail
    private lateinit var ldnProdukt: LabelPriceDetail
    private lateinit var ldnSazba: LabelPriceDetail
    private lateinit var ldnDodavatel: LabelPriceDetail
    private lateinit var ldnUzemi: LabelPriceDetail
    private lateinit var ldnVTDodavka: LabelPriceDetail
    private lateinit var ldnNTDodavka: LabelPriceDetail
    private lateinit var ldnMesPlat: LabelPriceDetail
    private lateinit var ldnVTDistribuce: LabelPriceDetail
    private lateinit var ldnNTDistribuce: LabelPriceDetail
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
    private lateinit var ldnSystemSluzby: LabelPriceDetail
    private lateinit var ldnOTE: LabelPriceDetail
    private lateinit var ldnPOZE1: LabelPriceDetail
    private lateinit var ldnPOZE2: LabelPriceDetail
    private lateinit var ldnDan: LabelPriceDetail
    private lateinit var ldnDPH: LabelPriceDetail

    private lateinit var tvPoznamka: TextView
    private lateinit var tvPoznamkaVT: TextView
    private lateinit var tvPoznamkaNT: TextView
    private lateinit var tvPoznamkaPlat: TextView
    private lateinit var tvPoznamkaPOZE1: TextView
    private lateinit var tvPoznamkaPOZE2: TextView
    private lateinit var tvPoznamkaOTE: TextView

    private var priceId: Long = -1L
    private var showInFragment = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            priceId = it.getLong(PRICE_ID)
            showInFragment = it.getBoolean(SHOW_IN_FRAGMENT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        var v = inflater.inflate(R.layout.fragment_price_list_detail, container, false)
        if (showInFragment && DetectScreenMode.isLandscape(requireActivity())) {
            v = inflater.inflate(R.layout.fragment_price_list_detail_infragment, container, false)
        }

        ldnDatum = v.findViewById(R.id.ldnPlatnost)
        ldnRada = v.findViewById(R.id.ldnRada)
        ldnProdukt = v.findViewById(R.id.ldnProdukt)
        ldnSazba = v.findViewById(R.id.ldnSazba)
        ldnDodavatel = v.findViewById(R.id.ldnDodavatel)
        ldnUzemi = v.findViewById(R.id.ldnUzemi)
        tvPoznamka = v.findViewById(R.id.tvPoznamkaDetail)

        ldnVTDodavka = v.findViewById(R.id.ldn_VT_dodavka)
        ldnNTDodavka = v.findViewById(R.id.ldn_NT_dodavka)
        ldnMesPlat = v.findViewById(R.id.ldn_staly_plat)
        tvPoznamkaVT = v.findViewById(R.id.tvPoznamkaVT)
        tvPoznamkaNT = v.findViewById(R.id.tvPoznamkaNT)
        tvPoznamkaPlat = v.findViewById(R.id.tvPoznamkaPlat)
        tvPoznamkaPOZE1 = v.findViewById(R.id.tvPoznamkaPOZE1)
        tvPoznamkaPOZE2 = v.findViewById(R.id.tvPoznamkaPOZE2)
        tvPoznamkaOTE = v.findViewById(R.id.tvPoznamkaOTE)

        ldnVTDistribuce = v.findViewById(R.id.ldn_VT_distribuce)
        ldnNTDistribuce = v.findViewById(R.id.ldn_NT_distribuce)

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

        ldnSystemSluzby = v.findViewById(R.id.ldn_systemove_sluzby)
        ldnOTE = v.findViewById(R.id.ldn_OTE)
        ldnPOZE1 = v.findViewById(R.id.ldn_POZE1)
        ldnPOZE2 = v.findViewById(R.id.ldn_POZE2)

        ldnDan = v.findViewById(R.id.ldn_dan)
        ldnDPH = v.findViewById(R.id.ldn_DPH)

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
     */
    fun loadPrice(priceId: Long) {
        if (priceId <= 0) return
        this.priceId = priceId

        val dataPriceListSource = DataPriceListSource(requireActivity())
        dataPriceListSource.open()
        val priceList = dataPriceListSource.readPrice(priceId)
        dataPriceListSource.close()

        if (priceList == null) return

        val dateStart = priceList.platnostOD
        val dateEnd = priceList.platnostDO

        ldnDatum.price =
            "${ViewHelper.convertLongToDate(dateStart)} - ${
                ViewHelper.convertLongToDate(
                    dateEnd
                )
            }"

        ldnRada.price = priceList.rada
        ldnProdukt.price = priceList.produkt
        ldnSazba.price = priceList.sazba
        ldnDodavatel.price = priceList.firma
        ldnUzemi.price = priceList.distribuce

        ldnVTDodavka.price = df2.format(priceList.cenaVT)
        ldnNTDodavka.price = df2.format(priceList.cenaNT)
        ldnMesPlat.price = df2.format(priceList.mesicniPlat)

        ldnVTDistribuce.price = df2.format(priceList.distVT)
        ldnNTDistribuce.price = df2.format(priceList.distNT)

        ldnJ0.price = df2.format(priceList.j0)
        ldnJ1.price = df2.format(priceList.j1)
        ldnJ2.price = df2.format(priceList.j2)
        ldnJ3.price = df2.format(priceList.j3)
        ldnJ4.price = df2.format(priceList.j4)
        ldnJ5.price = df2.format(priceList.j5)
        ldnJ6.price = df2.format(priceList.j6)
        ldnJ7.price = df2.format(priceList.j7)
        ldnJ8.price = df2.format(priceList.j8)
        ldnJ9.price = df2.format(priceList.j9)
        ldnJ10.price = df2.format(priceList.j10)
        ldnJ11.price = df2.format(priceList.j11)
        ldnJ12.price = df2.format(priceList.j12)
        ldnJ13.price = df2.format(priceList.j13)
        ldnJ14.price = df2.format(priceList.j14)

        ldnSystemSluzby.price = df2.format(priceList.systemSluzby)
        ldnOTE.price = df2.format(priceList.cinnost)
        ldnPOZE1.price = df2.format(priceList.poze1)
        ldnPOZE2.price = df2.format(priceList.poze2)

        ldnDan.price = df2.format(priceList.dan)
        ldnDPH.price = df2.format(priceList.dph)

        // Skrytí NT u jednotarifního ceníku
        if (priceList.distNT == 0.0) {
            ldnNTDistribuce.visibility = View.GONE
            ldnNTDodavka.visibility = View.GONE
        } else {
            ldnNTDistribuce.visibility = View.VISIBLE
            ldnNTDodavka.visibility = View.VISIBLE
        }

        if (priceList.j10 == 0.0) {
            // Méně hodnot jističů
            ldnJ10.visibility = View.GONE
            ldnJ11.visibility = View.GONE
            ldnJ12.visibility = View.GONE
            ldnJ13.visibility = View.GONE
            ldnJ14.visibility = View.GONE
            ldnJ8.visibility = View.VISIBLE
        } else {
            // Více hodnot jističů
            ldnJ10.visibility = View.VISIBLE
            ldnJ11.visibility = View.VISIBLE
            ldnJ12.visibility = View.VISIBLE
            ldnJ13.visibility = View.VISIBLE
            ldnJ14.visibility = View.VISIBLE
            ldnJ8.visibility = View.GONE
        }

        val calendar = ViewHelper.parseCalendarFromString(ldnDatum.price)
        val year = calendar[Calendar.YEAR]
        if (year < 2016) {
            ldnOTE.price = df2.format(priceList.ote)
            ldnOTE.label = getString(R.string.cinnost_ote)
            ldnOTE.setItem(getString(R.string.kc_MWh))
            ldnPOZE2.price = df2.format(priceList.oze)
            ldnPOZE2.label = getString(R.string.podpora_vykupu_el_z_oze_kvet_a_dz)
            ldnPOZE1.visibility = View.GONE
        }

        // Přenastavení ceníku pro rok 2024 (1719784800000 = 1.7.2024)
        if (dateStart >= 1719784800000L) {
            ldnOTE.label = getString(R.string.provoz_nesitove_infrastruktury)
            ldnOTE.setItem(getString(R.string.kc_om))
        }

        val transition2024 = 1719784800000L // 1.7.2024
        val transition2025 = 1756677600000L // 1.9.2025
        when (year) {
            2024 -> {
                if (dateStart < transition2024 && dateEnd >= transition2024) {
                    tvPoznamkaOTE.text = getText(R.string.provoz_nesitove_infrastruktury_poznamka)
                    tvPoznamkaOTE.visibility = View.VISIBLE
                } else {
                    tvPoznamkaOTE.visibility = View.GONE
                }
            }

            2025 -> {
                if (dateStart < transition2025 && dateEnd >= transition2025) {
                    tvPoznamkaOTE.text = getText(R.string.nova_cena_infrastruktua_poznamka)
                    tvPoznamkaOTE.visibility = View.VISIBLE
                } else {
                    tvPoznamkaOTE.visibility = View.GONE
                }
            }

            else -> {
                tvPoznamkaOTE.visibility = View.GONE
            }
        }

        // Poznámky a cenové stropy
        val priceListRegulBuilder = PriceListRegulBuilder(priceList, year)
        val poznamka = priceListRegulBuilder.getNotes(requireActivity())
        val maxVT = priceListRegulBuilder.maxVT
        val maxNT = priceListRegulBuilder.maxNT
        val maxPlat = priceListRegulBuilder.maxPlat
        val maxPOZE1 = priceListRegulBuilder.maxPOZE

        if (priceListRegulBuilder.isRegulPrice) {
            tvPoznamka.text = poznamka
            tvPoznamka.visibility = View.VISIBLE

            // Zobrazení stropů přímo pod příslušným sloupcem (VT, NT, Měsíční plat)
            setPoznamka(tvPoznamkaVT, maxVT)
            setPoznamka(tvPoznamkaNT, maxNT)
            if (priceList.distNT == 0.0) {
                tvPoznamkaNT.visibility = View.GONE
            }
            setPoznamka(tvPoznamkaPlat, maxPlat)

            // Zrušení POZE se zobrazuje pouze jednou pod sekcí Ostatní služby
            setPoznamka(tvPoznamkaPOZE1, maxPOZE1)
            tvPoznamkaPOZE2.visibility = View.GONE
        } else {
            tvPoznamka.visibility = View.GONE
            tvPoznamkaVT.visibility = View.GONE
            tvPoznamkaNT.visibility = View.GONE
            tvPoznamkaPlat.visibility = View.GONE
            tvPoznamkaPOZE1.visibility = View.GONE
            tvPoznamkaPOZE2.visibility = View.GONE
        }
    }

    private fun setPoznamka(tv: TextView, s: String?) {
        if (!s.isNullOrEmpty()) {
            tv.visibility = View.VISIBLE
            tv.text = s
        } else {
            tv.visibility = View.GONE
        }
    }
}
