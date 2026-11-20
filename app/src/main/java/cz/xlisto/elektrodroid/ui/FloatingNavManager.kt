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

class FloatingNavManager(
    private val activity: MainActivity,
    private val bottomNav: View?,
    private val floatingNavCard: View?
) {
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

    fun getBottomPanelMarginPx(): Int {
        val density = activity.resources.displayMetrics.density
        val cardHeightPx = if (floatingNavCard != null && floatingNavCard.height > 0) {
            floatingNavCard.height
        } else {
            ((if (isCompact) 62 else 80) * density).toInt()
        }
        return calculatePanelMarginForHeight(cardHeightPx)
    }

    fun updateCenteredBottomMargin() {
        val nav = bottomNav as? BottomNavigationView ?: return
        nav.setPadding(0, 0, 0, 0)

        val lp = nav.layoutParams as? ViewGroup.MarginLayoutParams
        if (lp != null && (lp.topMargin != 0 || lp.bottomMargin != 0)) {
            lp.topMargin = 0
            lp.bottomMargin = 0
            nav.layoutParams = lp
        }

        for (i in 0 until nav.childCount) {
            val child = nav.getChildAt(i)
            child.setPadding(0, 0, 0, 0)
            if (child is ViewGroup) {
                for (j in 0 until child.childCount) {
                    val item = child.getChildAt(j)
                    if (item.paddingBottom > 0) {
                        item.setPadding(item.paddingLeft, item.paddingTop, item.paddingRight, 0)
                    }
                }
            }
        }
    }

    fun configureCompactNavRail() {
        val isNavRail = activity.isNavRail
        if (!isNavRail) return

        val nav = bottomNav as? NavigationRailView ?: return
        val card = floatingNavCard as? com.google.android.material.card.MaterialCardView ?: return
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
            if (cardLp != null && cardLp.width != targetWidthPx) {
                cardLp.width = targetWidthPx
                card.layoutParams = cardLp
            }
            nav.setPadding(0, 0, 0, 0)
            nav.minimumWidth = 0
        }
    }

    fun setCompact(compact: Boolean) {
        val nav = bottomNav as? BottomNavigationView ?: return
        val card = floatingNavCard ?: return
        if (isCompact == compact) return
        isCompact = compact

        labelAlphaAnimator?.cancel()
        val density = activity.resources.displayMetrics.density

        if (card.height > 0 && initialCardHeightPx == 0) {
            initialCardHeightPx = card.height
        }

        var labelDifferencePx = 18f * density
        fun findItemLabelDifference(vg: ViewGroup) {
            for (i in 0 until vg.childCount) {
                val child = vg.getChildAt(i)
                if (child is TextView && child.height > 0) {
                    val lp = child.layoutParams as? ViewGroup.MarginLayoutParams
                    val topMargin = lp?.topMargin?.toFloat() ?: (0f * density)
                    val bottomMargin = lp?.bottomMargin?.toFloat() ?: 0f
                    labelDifferencePx = child.height.toFloat() + topMargin + bottomMargin
                    return
                } else if (child is ViewGroup) {
                    findItemLabelDifference(child)
                }
            }
        }
        findItemLabelDifference(nav)

        val expandedHeightPx =
            if (initialCardHeightPx > 0) initialCardHeightPx else (80 * density).toInt()
        val compactHeightPx =
            (expandedHeightPx - labelDifferencePx).toInt().coerceAtLeast((48 * density).toInt())

        val startHeightPx = if (compact) expandedHeightPx else compactHeightPx
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
            val currentAlpha = startAlpha + (targetAlpha - startAlpha) * fraction

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
                    val child = vg.getChildAt(i)
                    if (child is TextView) {
                        child.alpha = currentAlpha
                    } else if (child is ViewGroup) {
                        updateNavElements(child)
                    }
                }
            }
            updateNavElements(nav)
        }
        labelAlphaAnimator = anim
        anim.start()
    }

    fun hide(animated: Boolean) {
        val card = floatingNavCard ?: bottomNav ?: return
        if (!card.isVisible && card.translationY > 0f) return
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
