package awele.bot.common;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

import awele.core.Board;

/**
 * Cut-off driven move ordering.
 *
 * <p>Alpha-beta prunes far more when the strongest move is searched first.
 * Rather than guessing that order from the position, every move key keeps a
 * running score: +1 each time it produced a cut-off, -1 each time it did not.
 * The search then visits the moves from the highest score down. The statistics
 * survive across games, so the ordering keeps improving as the bot plays.</p>
 *
 * @author Nathan PLONTZ
 */
public final class MoveOrdering
{
    private MoveOrdering ()
    {
    }

    /**
     * Ranks the six holes by their recorded cut-off score.
     *
     * <p>The array is sorted in ascending order, so callers walk it backwards
     * to visit the most promising move first.</p>
     *
     * @param board The current board
     * @param history The cut-off statistics, keyed by move id
     * @return The hole indices, worst first
     */
    public static Integer [] rank (Board board, Map <Integer, Integer> history)
    {
        Integer [] indices = new Integer [Board.NB_HOLES];
        int [] scores = new int [Board.NB_HOLES];
        for (int i = 0; i < indices.length; i++)
        {
            indices[i] = i;
            Integer score = history.get (BoardSignature.moveId (board, i));
            scores[i] = (score == null) ? 0 : score;
        }
        Arrays.sort (indices, Comparator.comparingInt (i -> scores[i]));
        return indices;
    }

    /**
     * Rewards a move that caused a cut-off.
     *
     * @param board The board the move was played from
     * @param hole The hole that was played
     * @param history The cut-off statistics to update
     */
    public static void recordCutoff (Board board, int hole, Map <Integer, Integer> history)
    {
        MoveOrdering.add (board, hole, history, 1);
    }

    /**
     * Penalises a move that was searched without causing a cut-off.
     *
     * @param board The board the move was played from
     * @param hole The hole that was played
     * @param history The cut-off statistics to update
     */
    public static void recordMiss (Board board, int hole, Map <Integer, Integer> history)
    {
        MoveOrdering.add (board, hole, history, -1);
    }

    private static void add (Board board, int hole, Map <Integer, Integer> history, int delta)
    {
        int key = BoardSignature.moveId (board, hole);
        Integer current = history.get (key);
        if (current == null)
            history.put (key, delta);
        else
            history.replace (key, current + delta);
    }
}
