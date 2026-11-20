package cz.xlisto.elektrodroid.ui

import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

class FloatingNavScrollListener(
    private val onScrollDirectionChanged: (compact: Boolean) -> Unit
) : RecyclerView.OnScrollListener() {

    private var accumulatedDy = 0
    private var isCompact = false

    companion object {
        private const val SCROLL_THRESHOLD = 32
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        // Pokud jsme na samém začátku seznamu, vždy vrátit plný rozbalený stav
        if (!recyclerView.canScrollVertically(-1)) {
            if (isCompact) {
                isCompact = false
                accumulatedDy = 0
                onScrollDirectionChanged(false)
            }
            return
        }

        // Akumulujeme vertikální posun stejným směrem
        if ((dy > 0 && accumulatedDy < 0) || (dy < 0 && accumulatedDy > 0)) {
            accumulatedDy = 0
        }
        accumulatedDy += dy

        if (abs(accumulatedDy) >= SCROLL_THRESHOLD) {
            if (accumulatedDy > 0 && !isCompact) {
                // Posun dolů v seznamu (prst jde nahoru) -> kompaktní stav (skrýt textové popisky)
                isCompact = true
                onScrollDirectionChanged(true)
                accumulatedDy = 0
            } else if (accumulatedDy < 0 && isCompact) {
                // Posun nahoru v seznamu (prst jde dolů) -> rozšířený stav (zobrazit textové popisky)
                isCompact = false
                onScrollDirectionChanged(false)
                accumulatedDy = 0
            }
        }
    }
}
