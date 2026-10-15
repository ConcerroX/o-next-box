package com.mi.onextbox.ui.screens

import org.junit.Assert.*
import org.junit.Test

class SharedFeatureDefinitionsTest {
    @Test fun migratedDefinitionsHaveExactlyOneConsistentEntryInEveryConsumer() {
        val actions = EnabledFeatureActions()
        for (definition in SharedFeatureDefinitions.all) {
            val catalog = EnabledFeatureCatalog.entries.single { it.title == definition.title }
            val search = FeatureSearchIndex.entries.single { it.title == definition.title }
            assertEquals(definition.page, catalog.page)
            assertEquals(definition.page, search.page)
            assertEquals(definition.summary, search.description)
            assertSame(definition.read, catalog.read)
            assertTrue(actions.supports(definition.title))
        }
    }
}
