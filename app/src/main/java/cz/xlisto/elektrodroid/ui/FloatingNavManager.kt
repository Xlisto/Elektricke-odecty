package cz.xlisto.elektrodroid.ui

import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigationrail.NavigationRailView
import cz.xlisto.elektrodroid.MainActivity

/**
 * Správce plovoucího navigačního panelu v [MainActivity].
 *
 * Zajišťuje rozměry, vnitřní odsazení, animaci přepínání mezi plným a kompaktním režimem
 * spodní navigace ([BottomNavigationView]), konfiguraci postranního panelu ([NavigationRailView])
 * a výpočet spodního odsazení pro obsah fragmentů a plovoucí akční tlačítka (FAB).
 *
 * @property activity Hlavní aktivita poskytující rozměry systémových lišt a stav rozvržení
 * @property bottomNav Instance [BottomNavigationView] nebo [NavigationRailView] uvnitř plovoucí karty
 * @property floatingNavCard Obalující karta plovoucího navigačního panelu
 */
class FloatingNavManager(
    private val activity: MainActivity,
    private val bottomNav: View?,
    private val floatingNavCard: View?,
) {
    /**
     * Určuje, zda je spodní navigační panel aktuálně zmenšen do kompaktního režimu (bez popisků).
     */
    var isCompact: Boolean = false
        private set

    private var initialCardHeightPx: Int = 0
    private var labelAlphaAnimator: ValueAnimator? = null

    init {
        floatingNavCard?.post {
            if (floatingNavCard.height > 0) {
                initialCardHeightPx = floatingNavCard.height
            }
            updateCenteredBottomMargin()
            configureCompactNavRail()
        }
    }

    /**
     * Vypočítá celkové spodní odsazení (v pixelech) od spodní hrany obrazovky nad plovoucí kartu
     * pro zadanou výšku karty.
     *
     * V režimu postranního panelu ([MainActivity.isNavRail]) vrací pouze součet spodního systémového
     * insetu a základního odsazení `16 dp`.
     *
     * @param cardHeightPx Výška karty spodní navigace v pixelech
     * @return Celkové spodní odsazení v pixelech
     */
    private fun calculatePanelMarginForHeight(cardHeightPx: Int): Int {
        val density = activity.resources.displayMetrics.density
        val baseMarginBottomPx = (16 * density).toInt()

        val isNavRail = activity.isNavRail
        val insetBottom = activity.effectiveNavBarInsetBottom
        if (isNavRail) {
            return insetBottom + baseMarginBottomPx
        }

        val gapPx = (8 * density).toInt()
        return insetBottom + baseMarginBottomPx + cardHeightPx + gapPx
    }

    /**
     * Vrátí aktuální spodní odsazení (v pixelech) potřebné pro umístění prvků nad plovoucím
     * spodním navigačním panelem.
     *
     * Pokud karta ještě není změřená, použije výchozí výšku `56 dp` pro kompaktní režim
     * nebo `80 dp` pro plný režim.
     *
     * @return Spodní odsazení v pixelech
     */
    fun getBottomPanelMarginPx(): Int {
        val density = activity.resources.displayMetrics.density
        val cardHeightPx = if ((floatingNavCard != null) && (floatingNavCard.height > 0)) {
            floatingNavCard.height
        } else {
            ((if (isCompact) 56 else 80) * density).toInt()
        }
        return calculatePanelMarginForHeight(cardHeightPx)
    }

    /**
     * Vynuluje vnitřní odsazení, horní/spodní marginy a minimální výšku [BottomNavigationView]
     * i jeho vnořených kontejnerů a položek.
     *
     * Tím zabrání posunu obsahu směrem nahoru při zmenšení karty pod výchozích `80 dp` z Material 3.
     */
    fun updateCenteredBottomMargin() {
        val nav = (bottomNav as? BottomNavigationView) ?: return
        nav.setPadding(0, 0, 0, 0)
        nav.minimumHeight = 0

        val lp = nav.layoutParams as? ViewGroup.MarginLayoutParams
        if ((lp != null) && ((lp.topMargin != 0) || (lp.bottomMargin != 0))) {
            lp.topMargin = 0
            lp.bottomMargin = 0
            nav.layoutParams = lp
        }

        for (i in 0 until nav.childCount) {
            val child = nav.getChildAt(i)
            child.setPadding(0, 0, 0, 0)
            child.minimumHeight = 0
            val childGroup = (child as? ViewGroup) ?: continue
            for (j in 0 until childGroup.childCount) {
                val item = childGroup.getChildAt(j)
                item.minimumHeight = 0
                if (item.paddingBottom > 0) {
                    item.setPadding(item.paddingLeft, item.paddingTop, item.paddingRight, 0)
                }
            }
        }
    }

    /**
     * Upraví šířku a zobrazení popisků postranního navigačního panelu ([NavigationRailView])
     * podle typu zařízení (tablet vs. telefon v režimu na šířku).
     */
    fun configureCompactNavRail() {
        val isNavRail = activity.isNavRail
        if (!isNavRail) return

        val nav = (bottomNav as? NavigationRailView) ?: return
        val card = (floatingNavCard as? com.google.android.material.card.MaterialCardView) ?: return
        val density = activity.resources.displayMetrics.density

        if (activity.isTablet) {
            val cardLp = card.layoutParams
            if (cardLp != null) {
                cardLp.width = ViewGroup.LayoutParams.WRAP_CONTENT
                cardLp.height = ViewGroup.LayoutParams.WRAP_CONTENT
                card.layoutParams = cardLp
            }
            nav.labelVisibilityMode = NavigationRailView.LABEL_VISIBILITY_LABELED
            nav.minimumWidth = (80 * density).toInt()
        } else {
            val targetWidthPx = (64 * density).toInt()
            val cardLp = card.layoutParams
            if ((cardLp != null) && (cardLp.width != targetWidthPx)) {
                cardLp.width = targetWidthPx
                card.layoutParams = cardLp
            }
            nav.setPadding(0, 0, 0, 0)
            nav.minimumWidth = 0
        }
    }

    /**
     * Plynule přepne spodní plovoucí navigační panel mezi plným a kompaktním zobrazením.
     *
     * V kompaktním režimu skryje textové popisky položek, zmenší výšku karty tak, aby ikona
     * měla shodné horní i spodní odsazení, a v každém kroku animace přepočítá pozici FAB v aktivitě.
     *
     * @param compact `true` pro zmenšení do kompaktního režimu, `false` pro návrat do plné výšky
     */
    fun setCompact(compact: Boolean) {
        val nav = (bottomNav as? BottomNavigationView) ?: return
        val card = floatingNavCard ?: return
        if (isCompact == compact) return
        isCompact = compact

        updateCenteredBottomMargin()
        labelAlphaAnimator?.cancel()
        val density = activity.resources.displayMetrics.density

        if ((card.height > 0) && (initialCardHeightPx == 0)) {
            initialCardHeightPx = card.height
        }

        val contentContainer =
            nav.findViewById<View>(com.google.android.material.R.id.navigation_bar_item_content_container)
        val iconContainer =
            nav.findViewById<View>(com.google.android.material.R.id.navigation_bar_item_icon_container)

        val expandedHeightPx =
            if (initialCardHeightPx > 0) initialCardHeightPx else (80 * density).toInt()
        val compactHeightPx =
            if ((contentContainer != null) && (iconContainer != null) && (iconContainer.height > 0)) {
                val contentTopMargin =
                    (contentContainer.layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin
                        ?: contentContainer.top
                val topSpacingPx = contentTopMargin + iconContainer.top
                ((topSpacingPx * 2) + iconContainer.height).coerceAtLeast((48 * density).toInt())
            } else {
                (56 * density).toInt()
            }

        val startHeightPx =
            if (card.height > 0) card.height else if (compact) expandedHeightPx else compactHeightPx
        val targetHeightPx = if (compact) compactHeightPx else expandedHeightPx

        val startAlpha = if (compact) 1f else 0f
        val targetAlpha = if (compact) 0f else 1f

        card.scaleY = 1.0f
        card.translationY = 0f

        val anim = ValueAnimator.ofInt(startHeightPx, targetHeightPx)
        anim.duration = 200
        anim.interpolator = FastOutSlowInInterpolator()
        anim.addUpdateListener { animator ->
            val currentHeightPx = animator.animatedValue as Int
            val fraction = animator.animatedFraction
            val currentAlpha = startAlpha + ((targetAlpha - startAlpha) * fraction)

            // 1. Změna reálné výšky karty (layoutParams.height)
            val cardLp = card.layoutParams
            cardLp.height = currentHeightPx
            card.layoutParams = cardLp

            // 2. Přepočet marginu v přesném reálném čase 1:1!
            val currentPanelMarginPx = calculatePanelMarginForHeight(currentHeightPx)
            activity.updateBottomPanelMarginValue(currentPanelMarginPx)

            // 3. Průhlednost popisků
            fun updateNavElements(vg: ViewGroup) {
                for (i in 0 until vg.childCount) {
                    when (val child = vg.getChildAt(i)) {
                        is TextView -> child.alpha = currentAlpha
                        is ViewGroup -> updateNavElements(child)
                    }
                }
            }
            updateNavElements(nav)
        }
        labelAlphaAnimator = anim
        anim.start()
    }

    /**
     * Skryje plovoucí navigační kartu posunem dolů a zprůhledněním, případně okamžitě bez animace.
     *
     * @param animated `true` pro animované skrytí, `false` pro okamžité skrytí
     */
    fun hide(animated: Boolean) {
        val card = floatingNavCard ?: bottomNav ?: return
        if ((!card.isVisible) && (card.translationY > 0f)) return
        if (animated) {
            card.animate()
                .translationY(card.height.toFloat() + (32 * activity.resources.displayMetrics.density))
                .alpha(0f)
                .setDuration(250)
                .withEndAction { card.visibility = View.GONE }
                .start()
        } else {
            card.visibility = View.GONE
            card.alpha = 0f
        }
    }

    /**
     * Zobrazí plovoucí navigační kartu návratem na původní svislou pozici a plnou neprůhlednost.
     *
     * @param animated `true` pro animované zobrazení, `false` pro okamžité zobrazení
     */
    fun show(animated: Boolean) {
        val card = floatingNavCard ?: bottomNav ?: return
        card.animate().cancel()
        card.visibility = View.VISIBLE
        if (animated) {
            card.animate().translationY(0f).alpha(1f).setDuration(250).start()
        } else {
            card.translationY = 0f
            card.alpha = 1f
        }
    }
}
