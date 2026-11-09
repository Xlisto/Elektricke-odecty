package cz.xlisto.elektrodroid.pricelist

import cz.xlisto.elektrodroid.models.PriceListModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Jednotkové testy pro ověření výběru ceníků a dynamické manipulace s vybranými ID.
 */
class PriceListSelectionTest {

    @Test
    fun testSelectionTracking() {
        val selectedIds = mutableSetOf<Long>()
        val item1 = PriceListModel("Rada A").apply { id = 101L }
        val item2 = PriceListModel("Rada B").apply { id = 102L }
        val item3 = PriceListModel("Rada C").apply { id = 103L }

        selectedIds.add(item1.id)
        selectedIds.add(item2.id)

        assertTrue(selectedIds.contains(item1.id))
        assertTrue(selectedIds.contains(item2.id))
        assertFalse(selectedIds.contains(item3.id))

        assertEquals(2, selectedIds.size)

        // Odstranění jednoho ceníku
        selectedIds.remove(item1.id)
        assertFalse(selectedIds.contains(item1.id))
        assertEquals(1, selectedIds.size)
    }

    @Test
    fun testVisibleAndHiddenSelectionCounts() {
        val allSelectedIds = setOf(101L, 102L, 103L)

        // Simulovaný seznam ceníků viditelných po filtraci (jen 101L a 102L)
        val visibleItems = listOf(
            PriceListModel("Rada A").apply { id = 101L },
            PriceListModel("Rada B").apply { id = 102L }
        )

        val totalSelected = allSelectedIds.size
        val visibleSelected = visibleItems.count { allSelectedIds.contains(it.id) }
        val hiddenSelected = totalSelected - visibleSelected

        assertEquals(3, totalSelected)
        assertEquals(2, visibleSelected)
        assertEquals(1, hiddenSelected)
    }
}
