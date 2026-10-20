package dev.kesatria.gemmatch

import dev.kesatria.gemmatch.core.FinishedRun
import dev.kesatria.gemmatch.core.Lifetime

/**
 * Finished runs, kept apart from the game in progress. The files are the only implementation today; the interface is
 * here so the backend can change without touching the session or the screens.
 */
interface RunHistory {
    /** Totals and bests over every run ever recorded, including runs that have dropped off [runs]. */
    fun lifetime(): Lifetime

    /** The newest runs, newest first. */
    fun runs(): List<FinishedRun>

    /** Adds [run] to the totals and the list, at most once. False if it was already recorded. */
    fun record(run: FinishedRun): Boolean
}
