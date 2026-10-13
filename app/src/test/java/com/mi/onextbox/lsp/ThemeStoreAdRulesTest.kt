package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class ThemeStoreAdRulesTest {
    @Test fun filtersOnlyExplicitAdTypesWithoutChangingSourceOrderOrContents() {
        val normal = "com.oppo.cdo.card.theme.dto.BannerDto"
        val advertising = "com.oppo.cdo.card.theme.dto.ad.AdCardDto"
        val source = arrayListOf(normal, advertising, "com.oppo.cdo.card.theme.dto.ResourceCardDto")
        assertEquals(listOf(normal, source[2]), ThemeStoreAdRules.filterCards(source, true, ThemeStoreAdRules::isAdType))
        assertEquals(listOf(normal, advertising, "com.oppo.cdo.card.theme.dto.ResourceCardDto"), source)
        assertFalse(ThemeStoreAdRules.isAdType("unrelated.AdCardDto"))
        assertFalse(ThemeStoreAdRules.isAdType("com.oppo.cdo.card.theme.dto.AdSettingsCardDto"))
    }
    @Test fun disabledOrNoAdsPreservesOriginalListIdentity() {
        val ads = listOf("com.oppo.cdo.card.theme.dto.SearchBannerAdCardDto")
        assertSame(ads, ThemeStoreAdRules.filterCards(ads, false, ThemeStoreAdRules::isAdType))
        assertTrue(ThemeStoreAdRules.filterCards(ads, true, ThemeStoreAdRules::isAdType).isEmpty())
        val ordinary = listOf("com.oppo.cdo.card.theme.dto.BannerDto")
        assertSame(ordinary, ThemeStoreAdRules.filterCards(ordinary, true, ThemeStoreAdRules::isAdType))
    }
    @Test fun containerAdaptersUseTheirActualCollectionGetters() {
        assertEquals("getTabs", ThemeStoreAdRules.cardGetters["MultiTabCardDto"])
        assertEquals("getTabs", ThemeStoreAdRules.cardGetters["MultiTabCardDtoV2"])
        assertEquals("getRankCards", ThemeStoreAdRules.cardGetters["RankListCardDto"])
        assertEquals("getTopicList", ThemeStoreAdRules.cardGetters["TopicListCardDto"])
        assertEquals("getTopicList", ThemeStoreAdRules.cardGetters["TopicListV2CardDto"])
        assertEquals("getItems", ThemeStoreAdRules.cardGetters["TabItemCardDto"])
        assertEquals("getItems", ThemeStoreAdRules.cardGetters["TabItemCardDtoV2"])
    }
    @Test fun nullableAndOrdinaryEntriesAreKeptAlongsideAllSupportedAdTypes() {
        val normal = "com.oppo.cdo.card.theme.dto.ResourceCardDto"
        val source = listOf(null, normal,
            "com.oppo.cdo.card.theme.dto.ad.AdCardDto",
            "com.oppo.cdo.card.theme.dto.ad.template.TemplateAdCardDTO",
            "com.oppo.cdo.card.theme.dto.SearchBannerAdCardDto",
            "com.oppo.cdo.card.theme.dto.GoodsAdCardDto")
        assertEquals(listOf(null, normal), ThemeStoreAdRules.filterCards(source, true) {
            it != null && ThemeStoreAdRules.isAdType(it)
        })
        assertEquals(6, source.size)
    }
}
