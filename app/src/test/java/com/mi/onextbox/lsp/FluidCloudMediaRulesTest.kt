package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class FluidCloudMediaRulesTest {
    private data class Player(val pkg: String?, val user: Int = 0)

    @Test fun emptyBlacklistKeepsTheOriginalListWithoutReadingRows() {
        val players = listOf(Player("com.example.music"))
        assertSame(players, FluidCloudMediaRules.filter(players, emptySet()) { error("Unexpected read") })
    }

    @Test fun onlyExactPackagesAreBlocked() {
        val allowed = Player("com.example.music.extra")
        val unknown = Player(null)
        val players = listOf(Player("com.example.music"), allowed, unknown, Player("com.example.Music"))
        assertEquals(players.drop(1), FluidCloudMediaRules.filter(players, setOf("com.example.music")) { it.pkg })
    }

    @Test fun allPlayersOfTheSelectedPackageAreBlockedIncludingClones() {
        val players = listOf(Player("com.example.music", 0), Player("com.example.music", 999))
        assertTrue(FluidCloudMediaRules.filter(players, setOf("com.example.music")) { it.pkg }.isEmpty())
        assertEquals(2, players.size)
    }

    @Test fun otherPlayersRetainTheirOrderAndIdentity() {
        val first = Player("com.other.music")
        val last = Player("com.third.video")
        val players = arrayListOf(first, Player("com.example.music"), last, first)
        val result = FluidCloudMediaRules.filter(players, setOf("com.example.music")) { it.pkg }
        assertEquals(listOf(first, last, first), result)
        assertSame(first, result.first())
        assertEquals(4, players.size)
    }

    @Test fun unmatchedBlacklistKeepsTheOriginalList() {
        val players = listOf(Player("com.other.music"))
        assertSame(players, FluidCloudMediaRules.filter(players, setOf("com.example.music")) { it.pkg })
    }

    @Test fun disablingRestoresAllRowsFromTheUnmodifiedNativeList() {
        val native = arrayListOf(Player("com.example.music"), Player("com.other.music"))
        assertEquals(1, FluidCloudMediaRules.filter(native, setOf("com.example.music")) { it.pkg }.size)
        assertSame(native, FluidCloudMediaRules.filter(native, emptySet()) { it.pkg })
        assertEquals(2, native.size)
    }

    @Test fun normalizationRejectsPathsWildcardsAndInvalidNamesWithoutChangingPackageCase() {
        assertEquals(setOf("com.Example.music", "com.example.video"), FluidCloudMediaRules.normalize(setOf(
            " com.Example.music ", "com.example.video", "", "com.*", "../com.example", "com..example", "android", "com.1bad",
        )))
    }

    @Test fun validPackagesMayContainDigitsAndUnderscoresButNotSpaces() {
        assertTrue(FluidCloudMediaRules.isPackageName("com.example_1.player2"))
        assertFalse(FluidCloudMediaRules.isPackageName("com.example player"))
    }

    @Test fun blacklistDoesNotDependOnTheSystemWhitelistOrPlaybackState() {
        val players = listOf(Player("com.spotify.music"), Player("com.netease.cloudmusic"))
        assertTrue(FluidCloudMediaRules.filter(players, setOf("com.spotify.music", "com.netease.cloudmusic")) { it.pkg }.isEmpty())
    }
}
