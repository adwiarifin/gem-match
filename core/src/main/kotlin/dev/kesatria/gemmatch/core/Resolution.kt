package dev.kesatria.gemmatch.core

data class Resolution(
    val board: Board,
    val steps: List<Step>,
    val score: Int,
)

/**
 * Plays a valid swap to a stable board: clear, fall, spawn, repeat until no matches remain.
 * [a] is the cell the player dragged. Only the player's own match (depth 1) can make special pieces.
 * Consumes [rng] in place (one gem per spawn); pass a copy if the caller must keep its own.
 */
fun resolveSwap(
    board: Board,
    a: Pos,
    b: Pos,
    rng: Rng,
): Resolution {
    require(isValidSwap(board, a, b)) { "swap of $a and $b is not valid" }

    val steps = mutableListOf<Step>(Step.Swap(a, b))
    var current = board.swap(a, b)
    var score = 0
    var depth = 1

    while (true) {
        val groups = findMatchGroups(current)
        if (groups.isEmpty()) break

        val points = scoreGroups(groups, depth)
        val cleared = groups.flatMapTo(mutableSetOf()) { it.cells }
        steps += Step.Clear(cleared, points, depth)
        score += points
        current = cleared.fold(current) { b, pos -> b.with(pos, null) }

        if (depth == 1) {
            for (create in createdPieces(groups, a, b)) {
                steps += create
                current = current.with(create.pos, create.piece)
            }
        }

        val (afterFall, fall) = applyGravity(current)
        if (fall.moves.isNotEmpty()) steps += fall
        val (afterSpawn, spawn) = refill(afterFall, rng)
        steps += spawn
        current = afterSpawn
        depth++
    }
    return Resolution(current, steps, score)
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

/** Fills every empty cell with a new gem, column by column, top row first. */
private fun refill(
    board: Board,
    rng: Rng,
): Pair<Board, Step.Spawn> {
    val placed = mutableListOf<Placed>()
    var result = board
    for (col in 0 until BOARD_SIZE) {
        for (row in 0 until BOARD_SIZE) {
            if (result[Pos(row, col)] == null) {
                val piece = Piece(rng.nextGem())
                placed += Placed(piece, Pos(row, col))
                result = result.with(Pos(row, col), piece)
            }
        }
    }
    return result to Step.Spawn(placed)
}
