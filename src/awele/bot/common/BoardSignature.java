package awele.bot.common;

import awele.core.Board;

/**
 * Compact integer keys that identify a position or a move.
 *
 * <p>Both are used as hash-map keys by the search: the position key indexes the
 * opening book learned during self-play, the move key indexes the cut-off
 * statistics that drive move ordering. Neither key includes the score, so two
 * boards holding the same seeds collide on purpose.</p>
 *
 * @author Nathan PLONTZ
 */
public final class BoardSignature
{
    /** Number of distinct seed counts a hole is expected to hold. */
    private static final int SEED_RANGE = 48;

    private BoardSignature ()
    {
    }

    /**
     * Hashes the twelve holes of a board, ignoring the scores.
     *
     * <p>The classic 17/31 polynomial hash keeps collisions rare enough that no
     * clash was observed over the self-play runs used to build the book.</p>
     *
     * @param board The board to identify
     * @return The position key
     */
    public static long positionId (Board board)
    {
        long hash = 17;
        for (int value : board.getPlayerHoles ())
            hash = 31 * hash + value;
        for (int value : board.getOpponentHoles ())
            hash = 31 * hash + value;
        return hash;
    }

    /**
     * Identifies a move by where it starts, how many seeds it sows and how full
     * the landing hole is.
     *
     * <p>Moves that look alike under those three criteria share a key, so the
     * cut-off statistics gathered for one of them transfer to the others. The
     * lap correction accounts for the sowing skipping its own source hole once
     * it wraps around the board.</p>
     *
     * @param board The current board
     * @param startHole The hole the move is played from
     * @return The move key
     */
    public static int moveId (Board board, int startHole)
    {
        int [] playerHoles = board.getPlayerHoles ();
        int seeds = playerHoles[startHole];

        int laps;
        if (seeds < 12)
            laps = 0;
        else if (seeds < 23)
            laps = 1;
        else if (seeds < 34)
            laps = 2;
        else if (seeds < 45)
            laps = 3;
        else
            laps = 4;

        int landing = (startHole + laps + seeds) % (2 * Board.NB_HOLES);
        if (landing < Board.NB_HOLES)
            return startHole + Board.NB_HOLES * seeds
                    + Board.NB_HOLES * SEED_RANGE * playerHoles[landing];
        return startHole + Board.NB_HOLES * seeds
                + Board.NB_HOLES * SEED_RANGE * board.getOpponentHoles ()[landing - Board.NB_HOLES];
    }
}
