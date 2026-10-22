package ru.muz.yandex

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibrarySnapshotStoreTest {
    private val empty = LibrarySnapshot(
        uid = "42",
        syncedAtMs = 1_000_000L,
        liked = emptyList(),
        albums = emptyList(),
        playlists = emptyList(),
        wave = emptyList(),
        chart = emptyList(),
    )

    @Test
    fun `fresh snapshot stays local until interval expires`() {
        assertTrue(LibrarySnapshotStore.isFresh(empty, intervalMinutes = 60, nowMs = 1_000_000L + 30 * 60_000L))
        assertFalse(LibrarySnapshotStore.isFresh(empty, intervalMinutes = 60, nowMs = 1_000_000L + 60 * 60_000L))
    }

    @Test
    fun `zero interval means manual sync only`() {
        assertFalse(LibrarySnapshotStore.isFresh(empty, intervalMinutes = 0, nowMs = 1_000_001L))
        assertFalse(LibrarySnapshotStore.isFresh(null, intervalMinutes = 60, nowMs = 1_000_001L))
    }
}
