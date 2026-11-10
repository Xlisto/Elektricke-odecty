package cz.xlisto.elektrodroid.modules.pricelist

import android.annotation.SuppressLint
import android.content.Context
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.checkbox.MaterialCheckBox
import cz.xlisto.elektrodroid.R
import cz.xlisto.elektrodroid.databaze.DataPriceListSource
import cz.xlisto.elektrodroid.databaze.DataSubscriptionPointSource
import cz.xlisto.elektrodroid.dialogs.PriceListDeleteBlockedDialogFragment
import cz.xlisto.elektrodroid.dialogs.YesNoDialogFragment
import cz.xlisto.elektrodroid.format.DecimalFormatHelper
import cz.xlisto.elektrodroid.models.PriceListModel
import cz.xlisto.elektrodroid.models.PriceListRegulBuilder
import cz.xlisto.elektrodroid.models.SubscriptionPointModel
import cz.xlisto.elektrodroid.ownview.ViewHelper
import cz.xlisto.elektrodroid.shp.ShPFilter
import cz.xlisto.elektrodroid.shp.ShPPriceList
import cz.xlisto.elektrodroid.utils.Calculation
import cz.xlisto.elektrodroid.utils.FragmentChange
import cz.xlisto.elektrodroid.utils.Round

/**
 * Adaptér RecyclerView pro zobrazení a správu položek ceníků podle pravidel Material Design 3.
 *
 * Zajišťuje kompaktní trojřádkové rozvržení položek, dynamické skrývání údajů určených aktivními filtry,
 * vícenásobný výběr položek pomocí checkboxů a správnou manipulaci s regulovanými cenami.
 */
class PriceListAdapter(
    private var items: ArrayList<PriceListModel>,
    private val subscriptionPoint: SubscriptionPointModel?,
    private var isSelectionMode: Boolean,
    private var idSelectedPriceList: Long,
    private val onClickItemListener: OnClickItemListener,
    private val recyclerView: RecyclerView,
    private val onSelectionChangedListener: OnSelectionChangedListener? = null
) : RecyclerView.Adapter<PriceListAdapter.MyViewHolder>() {

    companion object {
        const val TAG = "PriceListAdapter"

        const val FLAG_DIALOG_FRAGMENT_DELETE_PRICE_LIST = "dialogFragmentDeletePriceList"
    }

    /**
     * Rozhraní pro posluchače událostí klepnutí na položku ceníku.
     */
    fun interface OnClickItemListener {
        fun setClickPriceListListener(priceList: PriceListModel?)
    }

    /**
     * Rozhraní pro posluchače změny vícenásobného výběru ceníků.
     */
    fun interface OnSelectionChangedListener {
        fun onSelectionChanged(selectedIds: Set<Long>)
    }

    /**
     * ViewHolder pro položku ceníku.
     */
    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.cardPriceListItem)
        val cbSelectItem: MaterialCheckBox = itemView.findViewById(R.id.cbPriceList)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvSubtitle: TextView = itemView.findViewById(R.id.tvSubtitle)
        val containerNT: View = itemView.findViewById(R.id.containerNT)
        val tvPriceVT: TextView = itemView.findViewById(R.id.tvPriceVT)
        val tvPriceNT: TextView = itemView.findViewById(R.id.tvPriceNT)
        val tvPriceMonth: TextView = itemView.findViewById(R.id.tvPriceMonth)
        val tvPriceVTRegul: TextView = itemView.findViewById(R.id.tvPriceVTRegul)
        val tvPriceNTRegul: TextView = itemView.findViewById(R.id.tvPriceNTRegul)
        val tvPriceMonthRegul: TextView = itemView.findViewById(R.id.tvPriceMonthRegul)
        val tvNote: TextView = itemView.findViewById(R.id.tvNote)
        val lnButtons: LinearLayout = itemView.findViewById(R.id.lnButtonsPriceListItem)
        val btnEdit: Button = itemView.findViewById(R.id.btnEditPriceListItem)
        val btnDelete: Button = itemView.findViewById(R.id.btnDeletePriceListItem)
        val btnDetail: Button = itemView.findViewById(R.id.btnDetailPriceListItem)
    }

    private val context: Context = recyclerView.context
    private val shPPriceList = ShPPriceList(context)
    private val shPFilter = ShPFilter(context)

    /** Množina vybraných ID ceníků udržená napříč filtrací. */
    val selectedIds: MutableSet<Long> = HashSet()

    private var showButtons = -1
    private var selectedItemId = -1L
    private var selectedPosition = -1

    init {
        setHasStableIds(true)
        if (idSelectedPriceList > 0) {
            selectedIds.add(idSelectedPriceList)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_price_list, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: MyViewHolder,
        @SuppressLint("RecyclerView") position: Int
    ) {
        val priceList = items[position]
        showButtons = shPPriceList.get(ShPPriceList.SHOW_BUTTONS_PRICE_LIST, -1)

        // 1. Zobrazení názvu a dynamických popisků podle filtrů (Řádky 1 a 2)
        bindDynamicLabels(holder, priceList)

        // 2. Výpočet a zobrazení cenových údajů (Řádek 3)
        bindPrices(holder, priceList)

        // 3. Režim výběru nebo běžný režim s tlačítkami
        if (isSelectionMode) {
            holder.cbSelectItem.visibility = View.VISIBLE
            holder.cbSelectItem.isClickable = false
            holder.cbSelectItem.isFocusable = false
            holder.cbSelectItem.isChecked = selectedIds.contains(priceList.id)
            holder.lnButtons.visibility = View.GONE

            holder.cardView.setOnClickListener {
                if (selectedIds.contains(priceList.id)) {
                    selectedIds.remove(priceList.id)
                    holder.cbSelectItem.isChecked = false
                } else {
                    selectedIds.add(priceList.id)
                    holder.cbSelectItem.isChecked = true
                }
                idSelectedPriceList = priceList.id
                onClickItemListener.setClickPriceListListener(priceList)
                onSelectionChangedListener?.onSelectionChanged(selectedIds)
            }
        } else {
            holder.cbSelectItem.visibility = View.GONE

            holder.cardView.setOnClickListener {
                TransitionManager.beginDelayedTransition(recyclerView)

                if (showButtons == position) {
                    showButtons = -1
                    onClickItemListener.setClickPriceListListener(null)
                    setShowIdItem(-1)
                } else {
                    showButtons = position
                    onClickItemListener.setClickPriceListListener(priceList)
                    setShowIdItem(priceList.id)
                }

                setShowPositionItem(position)
                updateButtonsVisibility(holder, position)
            }

            holder.btnDetail.setOnClickListener {
                val priceListDetailFragment = PriceListDetailFragment.newInstance(priceList.id)
                FragmentChange.replace(
                    context as FragmentActivity,
                    priceListDetailFragment,
                    FragmentChange.Transaction.MOVE,
                    true
                )
            }

            holder.btnEdit.setOnClickListener {
                val priceListEditFragment = PriceListEditFragment.newInstance(priceList.id)
                FragmentChange.replace(
                    context as FragmentActivity,
                    priceListEditFragment,
                    FragmentChange.Transaction.MOVE,
                    true
                )
            }

            holder.btnDelete.setOnClickListener {
                selectedItemId = priceList.id
                selectedPosition = position
                val detailInfo =
                    "${priceList.rada} ${priceList.produkt}, \n${priceList.sazba}, \n" +
                            "${ViewHelper.convertLongToDate(priceList.platnostOD)} - ${
                                ViewHelper.convertLongToDate(
                                    priceList.platnostDO
                                )
                            }"

                val yesNoDialogFragment = YesNoDialogFragment.newInstance(
                    context.getString(R.string.confirm_delete_pricelist),
                    FLAG_DIALOG_FRAGMENT_DELETE_PRICE_LIST,
                    detailInfo
                )
                yesNoDialogFragment.show(
                    (context as FragmentActivity).supportFragmentManager,
                    "yesNoDialogFragment"
                )
            }

            updateButtonsVisibility(holder, position)
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemId(position: Int): Long {
        return items.getOrNull(position)?.id ?: RecyclerView.NO_ID
    }

    /**
     * Nastaví dynamický název (Řádek 1) a doplňující parametry platnosti a filtrů (Řádek 2).
     */
    private fun bindDynamicLabels(holder: MyViewHolder, priceList: PriceListModel) {
        val radaFilter = shPFilter.get(ShPFilter.RADA, ShPFilter.DEFAULT)
        val produktFilter = shPFilter.get(ShPFilter.PRODUKT, ShPFilter.DEFAULT)
        val sazbaFilter = shPFilter.get(ShPFilter.SAZBA, ShPFilter.DEFAULT)
        val companyFilter = shPFilter.get(ShPFilter.COMPANY, ShPFilter.DEFAULT)
        val areaFilter = shPFilter.get(ShPFilter.AREA, ShPFilter.DEFAULT)

        val isRadaFiltered =
            radaFilter != "%" && radaFilter != DataPriceListSource.VSE && radaFilter.isNotBlank()
        val isProduktFiltered =
            produktFilter != "%" && produktFilter != DataPriceListSource.VSE && produktFilter.isNotBlank()
        val isSazbaFiltered =
            sazbaFilter != "%" && sazbaFilter != DataPriceListSource.VSE && sazbaFilter.isNotBlank()
        val isCompanyFiltered =
            companyFilter != "%" && companyFilter != DataPriceListSource.VSE && companyFilter.isNotBlank()
        val isAreaFiltered =
            areaFilter != "%" && areaFilter != DataPriceListSource.VSE && areaFilter.isNotBlank()

        // První řádek: Název ceníku (Řada - Produkt)
        val titleText = buildString {
            if (!isRadaFiltered && priceList.rada.isNotBlank()) {
                append(priceList.rada)
            }
            if (!isProduktFiltered && priceList.produkt.isNotBlank()) {
                if (isNotEmpty()) append(" - ")
                append(priceList.produkt)
            }
            if (isEmpty()) {
                append(priceList.name.ifBlank { priceList.produkt })
            }
        }
        holder.tvTitle.text = titleText

        // Druhý řádek: Období platnosti + nefiltrované doplňující údaje
        val dateText = context.getString(
            R.string.validity_price_list_date,
            ViewHelper.convertLongToDate(priceList.platnostOD),
            ViewHelper.convertLongToDate(priceList.platnostDO)
        )

        val extraDetailsList = mutableListOf<String>()

        if (!isCompanyFiltered && !priceList.firma.isNullOrBlank()) {
            extraDetailsList.add(priceList.firma)
        }
        if (!isAreaFiltered && !priceList.distribuce.isNullOrBlank()) {
            extraDetailsList.add(priceList.distribuce)
        }
        if (!isSazbaFiltered && !priceList.sazba.isNullOrBlank()) {
            extraDetailsList.add(priceList.sazba)
        }

        // Pojistka proti vizuální nerozlišitelnosti: pokud byly všechny parametry odfiltrovány, přidáme sazbu a dodavatele
        if (extraDetailsList.isEmpty()) {
            if (!priceList.sazba.isNullOrBlank()) extraDetailsList.add(priceList.sazba)
            if (!priceList.firma.isNullOrBlank()) extraDetailsList.add(priceList.firma)
        }

        val extraDetailsStr = extraDetailsList.joinToString(" • ")
        holder.tvSubtitle.text =
            if (extraDetailsStr.isEmpty()) dateText else "$dateText • $extraDetailsStr"
    }

    /**
     * Zjistí, zda se jedná o jednosazbový ceník (např. D01d, D02d, C01d, C02d).
     */
    private fun isSingleTariff(priceList: PriceListModel): Boolean {
        val sazba = priceList.sazba.lowercase().replace(" ", "")
        return sazba.contains("d01") || sazba.contains("d02") ||
                sazba.contains("c01") || sazba.contains("c02") ||
                (priceList.cenaNT == 0.0 && priceList.distNT == 0.0)
    }

    /**
     * Vypočítá a zformátuje cenové údaje (VT, NT, Měsíc) i případné regulované ceny.
     */
    private fun bindPrices(holder: MyViewHolder, priceList: PriceListModel) {
        val ceny = Calculation.calculatePriceForPriceListDPH(priceList, subscriptionPoint)
        val cenaVT = DecimalFormatHelper.df2.format(Round.round(ceny[0])) + " Kč/kWh"
        val cenaNT = DecimalFormatHelper.df2.format(Round.round(ceny[1])) + " Kč/kWh"
        val cenaMesic = DecimalFormatHelper.df2.format(Round.round(ceny[2])) + " Kč s DPH"

        val year = ViewHelper.yearIntOfLong(priceList.platnostOD)
        val priceListBuilder = PriceListRegulBuilder(priceList, year)
        val priceListRegul = priceListBuilder.regulPriceList
        val cenyRegul = Calculation.calculatePriceForPriceListDPH(priceListRegul, subscriptionPoint)

        val cenaVTRegul = DecimalFormatHelper.df2.format(Round.round(cenyRegul[0])) + " Kč/kWh"
        val cenaNTRegul = DecimalFormatHelper.df2.format(Round.round(cenyRegul[1])) + " Kč/kWh"
        val cenaMesicRegul = DecimalFormatHelper.df2.format(Round.round(cenyRegul[2])) + " Kč s DPH"

        holder.tvPriceVT.text = cenaVT
        holder.tvPriceMonth.text = cenaMesic

        val singleTariff = isSingleTariff(priceList)
        if (singleTariff) {
            holder.containerNT.visibility = View.GONE
        } else {
            holder.containerNT.visibility = View.VISIBLE
            holder.tvPriceNT.text = cenaNT
        }

        if (priceListBuilder.isRegulPrice) {
            holder.tvPriceVTRegul.text = context.getString(R.string.plus_value, cenaVTRegul)
            holder.tvPriceVTRegul.visibility = View.VISIBLE
            if (!singleTariff) {
                holder.tvPriceNTRegul.text = context.getString(R.string.plus_value, cenaNTRegul)
                holder.tvPriceNTRegul.visibility = View.VISIBLE
            } else {
                holder.tvPriceNTRegul.visibility = View.GONE
            }
            holder.tvPriceMonthRegul.text = context.getString(R.string.plus_value, cenaMesicRegul)
            holder.tvPriceMonthRegul.visibility = View.VISIBLE

            val notes = priceListBuilder.getNotes(context)
            if (!notes.isNullOrEmpty()) {
                holder.tvNote.text = notes
                holder.tvNote.visibility = View.VISIBLE
            } else {
                holder.tvNote.visibility = View.GONE
            }
        } else {
            holder.tvPriceVTRegul.visibility = View.GONE
            holder.tvPriceNTRegul.visibility = View.GONE
            holder.tvPriceMonthRegul.visibility = View.GONE
            holder.tvNote.visibility = View.GONE
        }
    }

    /**
     * Řídí viditelnost akčních tlačítek na položce.
     */
    private fun updateButtonsVisibility(holder: MyViewHolder, position: Int) {
        setShowPositionItem(showButtons)
        if (position == showButtons) {
            holder.lnButtons.visibility = View.VISIBLE
        } else {
            holder.lnButtons.visibility = View.GONE
        }
    }

    /**
     * Smaže vybraný ceník.
     */
    fun deleteItemPrice() {
        if (selectedItemId != -1L && selectedPosition != -1) {
            deleteItemPrice(selectedItemId, selectedPosition)
            setShowPositionItem(-1)
            setShowIdItem(-1)
        }
    }

    /**
     * Skryje všechna zobrazená akční tlačítka.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun setHideButtons() {
        showButtons = -1
        setShowPositionItem(-1)
        setShowIdItem(-1)
        notifyDataSetChanged()
    }

    /**
     * Vyprázdní vícenásobný výběr položek.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun clearSelection() {
        selectedIds.clear()
        onSelectionChangedListener?.onSelectionChanged(selectedIds)
        notifyDataSetChanged()
    }

    /**
     * Přepne režim výběru.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun setSelectionMode(enabled: Boolean) {
        this.isSelectionMode = enabled
        notifyDataSetChanged()
    }

    private fun setShowPositionItem(position: Int) {
        shPPriceList.set(ShPPriceList.SHOW_BUTTONS_PRICE_LIST, position)
    }

    private fun setShowIdItem(id: Long) {
        shPPriceList.set(ShPPriceList.SHOW_ID_ITEM_PRICE_LIST, id)
    }

    /**
     * Provede odstranění ceníku podle ID, po kontrole jeho případného využití.
     */
    private fun deleteItemPrice(itemId: Long, position: Int) {
        val dataSubscriptionPointSource = DataSubscriptionPointSource(context)
        dataSubscriptionPointSource.open()
        val subscriptionPointModels = dataSubscriptionPointSource.loadSubscriptionPoints()
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
        dataSubscriptionPointSource.close()

        if (pricesTED > 0 || pricesFAK > 0 || pricesMON > 0) {
            showWarningDialog(pricesTED, pricesFAK, pricesMON, context)
            return
        }

        val dataPriceListSource = DataPriceListSource(context)
        dataPriceListSource.open()
        dataPriceListSource.deletePriceList(itemId)
        dataPriceListSource.close()

        showButtons = -1
        if (position >= 0 && position < items.size) {
            items.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, itemCount)
        }
    }

    private fun showWarningDialog(ted: Int, fak: Int, mon: Int, context: Context) {
        if (context is FragmentActivity) {
            PriceListDeleteBlockedDialogFragment.newInstance(ted, fak, mon)
                .show(context.supportFragmentManager, "priceListDeleteBlockedDialog")
        }
    }
}
