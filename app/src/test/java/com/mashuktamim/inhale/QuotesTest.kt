package com.mashuktamim.inhale

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuotesTest {

    @Test
    fun testGeneralQuotesNotEmpty() {
        val general = Quotes.getForType(Prefs.QuoteType.GENERAL)
        assertTrue(general.isNotEmpty())
        assertEquals(Quotes.GENERAL.size, general.size)
    }

    @Test
    fun testIslamicQuotesNotEmpty() {
        val islamic = Quotes.getForType(Prefs.QuoteType.ISLAMIC)
        assertTrue(islamic.isNotEmpty())
        assertEquals(Quotes.ISLAMIC.size, islamic.size)
    }

    @Test
    fun testBothQuotesCombinesCollections() {
        val both = Quotes.getForType(Prefs.QuoteType.BOTH)
        assertEquals(Quotes.GENERAL.size + Quotes.ISLAMIC.size, both.size)
    }

    @Test
    fun testGetRandomReturnsValidQuote() {
        val quote = Quotes.getRandom(Prefs.QuoteType.GENERAL)
        assertTrue(quote.first.isNotBlank())
        assertTrue(quote.second.isNotBlank())
    }
}
