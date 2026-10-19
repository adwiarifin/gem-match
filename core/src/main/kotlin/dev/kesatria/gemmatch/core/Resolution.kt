package dev.kesatria.gemmatch.core

data class Resolution(
    val board: Board,
    val steps: List<Step>,
    val score: Int,
    /** Gems in the biggest match group of the move, before any effect. 0 if no group formed. */
    val largestMatch: Int = 0,
)

/**
 * Plays a valid swap to a stable board: clear, fall, spawn, repeat until no matches remain.
 * [a] is the cell the player dragged. A special fires when the swap moves it, when it is part of a match at any
 * depth, or when another effect reaches it. Every qualifying match group earns a special, whether or not anything
 * fired in the same clear.
 * Consumes [rng] in place (at least one draw per spawned gem, see [SPAWN_RETRIES]); pass a copy if the caller must keep its own.
 */
fun resolveSwap(
    board: Board,
    a: Pos,
    b: Pos,
    rng: Rng,
    spawnRetries: Int = SPAWN_RETRIES,
): Resolution {
    require(isValidSwap(board, a, b)) { "swap of $a and $b is not valid" }

    val steps = mutableListOf<Step>(Step.Swap(a, b))
    var current = board.swap(a, b)
    var score = 0
    var depth = 1
    var largestMatch = 0

    while (true) {
        val groups = findMatchGroups(current)
        val matched = groups.flatMapTo(mutableSetOf()) { it.cells }
        val swap = if (depth == 1) a to b else null
        val fired = firings(current, matched, swap)
        if (groups.isEmpty() && fired.isEmpty()) break
        largestMatch = maxOf(largestMatch, groups.maxOfOrNull { it.cells.size } ?: 0)

        steps += fired
        val effectCells = fired.flatMapTo(mutableSetOf()) { it.cells }
        val points = scoreGroups(groups, depth) + scoreEffectGems((effectCells - matched).size, depth)
        val cleared = matched + effectCells
        steps += Step.Clear(cleared, points, depth)
        score += points
        current = cleared.fold(current) { remaining, pos -> remaining.with(pos, null) }

        for (create in createdPieces(groups, swap?.toList().orEmpty())) {
            steps += create
            current = current.with(create.pos, create.piece)
        }

        val (afterFall, fall) = applyGravity(current)
        if (fall.moves.isNotEmpty()) steps += fall
        val (afterSpawn, spawn) = refill(afterFall, rng, spawnRetries)
        steps += spawn
        current = afterSpawn
        depth++
    }
    return Resolution(current, steps, score, largestMatch)
}

/** Drops every piece to the lowest free row of its column. Moves are listed column by column, lowest gem first. */
private fun applyGravity(board: Board): Pair<Board, Step.Fall> {
    val moves = mutableListOf<GemMove>()
    var result = board
    for (col in 0 until BOARD_SIZE) {
        var target = BOARD_SIZE - 1
        for (row in BOARD_SIZE - 1 downTo 0) {
            val piece = board[Pos(row, col)] ?: continue
            if (row != target) {
                moves += GemMove(piece, Pos(row, col), Pos(target, col))
                result = result.with(Pos(row, col), null).with(Pos(target, col), piece)
            }
            target--
        }
    }
    return result to Step.Fall(moves)
}

/**
 * How many times a new gem that matches a neighbour is redrawn before it is kept anyway. More retries mean fewer
 * adjacent pairs, so fewer valid moves and a run that ends sooner. Zero draws one gem per cell with no check.
 */
internal const val SPAWN_RETRIES = 1

/**
 * Fills every empty cell with a new gem, column by column, top row first. A gem that equals an orthogonal
 * neighbour already on the board is redrawn up to [retries] times, then kept, so the board always fills.
 */
internal fun refill(
    board: Board,
    rng: Rng,
    retries: Int = SPAWN_RETRIES,
): Pair<Board, Step.Spawn> {
    val placed = mutableListOf<Placed>()
    var result = board
    for (col in 0 until BOARD_SIZE) {
        for (row in 0 until BOARD_SIZE) {
            if (result[Pos(row, col)] == null) {
                val piece = Piece(drawAvoidingNeighbours(result, Pos(row, col), rng, retries))
                placed += Placed(piece, Pos(row, col))
                result = result.with(Pos(row, col), piece)
            }
        }
    }
    return result to Step.Spawn(placed)
}

private fun drawAvoidingNeighbours(
    board: Board,
    pos: Pos,
    rng: Rng,
    retries: Int,
): Gem {
    val neighbours =
        listOf(Pos(pos.row - 1, pos.col), Pos(pos.row + 1, pos.col), Pos(pos.row, pos.col - 1), Pos(pos.row, pos.col + 1))
            .mapNotNull { board[it]?.gem }
    var gem = rng.nextGem()
    var left = retries
    while (left > 0 && gem in neighbours) {
        gem = rng.nextGem()
        left--
    }
    return gem
}
