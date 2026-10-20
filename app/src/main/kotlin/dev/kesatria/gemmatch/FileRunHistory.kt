package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.FinishedRun
import dev.kesatria.gemmatch.core.Lifetime
import dev.kesatria.gemmatch.core.decodeLifetime
import dev.kesatria.gemmatch.core.decodeRun
import dev.kesatria.gemmatch.core.encodeLifetime
import dev.kesatria.gemmatch.core.encodeRun
import dev.kesatria.gemmatch.core.keepNewest

/**
 * The run history as two text files next to the saved game: `runs.txt`, the newest runs one per line, and
 * `lifetime.txt`, one line for every run ever. Both are written with the store's atomic write. A damaged line costs
 * only that run, a damaged or oversize file reads as empty.
 */
class FileRunHistory(
    private val store: GameStore,
) : RunHistory {
    /** With no readable file yet, only the best score from the old `best.txt` carries over. */
    override fun lifetime(): Lifetime =
        store.read(LIFETIME_FILE, MAX_LIFETIME_BYTES)?.let { decodeLifetime(it.trim()) }
            ?: Lifetime.EMPTY.copy(bestScore = store.loadBest())

    override fun runs(): List<FinishedRun> =
        store
            .read(RUNS_FILE, MAX_RUNS_BYTES)
            ?.lines()
            ?.mapNotNull { decodeRun(it.trim()) }
            .orEmpty()

    /**
     * Writes the lifetime first, then the list, and waits. A kill between the two loses that run from the list, and
     * [Lifetime.last] makes a second try a no-op, so a run is never counted twice.
     */
    override fun record(run: FinishedRun): Boolean =
        store.onWriter {
            val before = lifetime()
            val alreadyRecorded = before.runs > 0 && before.last == run.startedMs
            if (!alreadyRecorded) {
                store.write(LIFETIME_FILE, encodeLifetime(before + run))
                store.write(RUNS_FILE, keepNewest(runs(), run).joinToString("\n") { encodeRun(it) })
            }
            !alreadyRecorded
        }

    private companion object {
        const val RUNS_FILE = "runs.txt"
        const val LIFETIME_FILE = "lifetime.txt"

        /** One lifetime line is about 700 bytes at its largest. */
        const val MAX_LIFETIME_BYTES = 2048L

        /** 100 run lines of about 300 bytes each is about 30 KiB. */
        const val MAX_RUNS_BYTES = 64 * 1024L
    }
}
