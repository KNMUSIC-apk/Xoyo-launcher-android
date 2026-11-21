package com.movtery.zalithlauncher.game.playhistory

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayHistoryTest {

    @Test
    fun sessionStartedCreatesRecordAndCountsSession() {
        val result = emptyMap<String, PlayRecord>().withSessionStarted("1.21", startedAt = 1_000L)

        val record = result.getValue("1.21")
        assertEquals(1_000L, record.lastPlayedAt)
        assertEquals(1, record.sessions)
        assertEquals(0L, record.totalPlayMs)
    }

    @Test
    fun sessionStartedUpdatesLastPlayedAndKeepsTotals() {
        val before = mapOf(
            "1.21" to PlayRecord("1.21", lastPlayedAt = 500L, totalPlayMs = 9_000L, sessions = 3)
        )

        val record = before.withSessionStarted("1.21", startedAt = 2_000L).getValue("1.21")

        assertEquals(2_000L, record.lastPlayedAt)
        assertEquals(9_000L, record.totalPlayMs)
        assertEquals(4, record.sessions)
    }

    @Test
    fun sessionEndedAddsElapsedTime() {
        val started = emptyMap<String, PlayRecord>().withSessionStarted("1.20.1", startedAt = 1_000L)

        val record = started.withSessionEnded("1.20.1", startedAt = 1_000L, endedAt = 61_000L).getValue("1.20.1")

        assertEquals(60_000L, record.totalPlayMs)
    }

    @Test
    fun negativeElapsedTimeIsIgnored() {
        val record = emptyMap<String, PlayRecord>()
            .withSessionEnded("1.20.1", startedAt = 10_000L, endedAt = 5_000L)
            .getValue("1.20.1")

        assertEquals(0L, record.totalPlayMs)
    }

    @Test
    fun totalsAccumulateAcrossSessions() {
        var map = emptyMap<String, PlayRecord>()
        map = map.withSessionStarted("a", 0L).withSessionEnded("a", 0L, 1_000L)
        map = map.withSessionStarted("a", 5_000L).withSessionEnded("a", 5_000L, 8_000L)

        val record = map.getValue("a")
        assertEquals(4_000L, record.totalPlayMs)
        assertEquals(2, record.sessions)
    }
}
