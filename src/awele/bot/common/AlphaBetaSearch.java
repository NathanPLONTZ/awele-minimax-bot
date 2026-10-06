package awele.bot.common;

import java.util.Map;

import awele.core.Board;
import awele.core.InvalidBotException;

/**
 * Depth-limited alpha-beta search over the Awele game tree.
 *
 * <p>The search is plain alpha-beta with one addition: when a cut-off history
 * is supplied, the moves of every node are visited in decreasing order of past
 * cut-off success and the outcome of each move feeds back into that history.
 * Passing no history falls back to the fixed right-to-left order, which is what
 * the unordered variants of the bot use.</p>
 *
 * @author Nathan PLONTZ
 */
public final class AlphaBetaSearch
{
    private static final Integer [] NATURAL_ORDER = {0, 1, 2, 3, 4, 5};

    /*
     * On the probeScore flag. The two earliest ablation bots simulate the
     * capture of each move before simulating the move itself, and then never
     * read the result. It is dead work: it clones the board and replays the move
     * for nothing. It is kept, behind this flag, because it roughly doubles the
     * per-node cost for those two bots, and their reported timings were measured
     * with it in place. The competition bot never had it enabled.
     */

    private AlphaBetaSearch ()
    {
    }

    /**
     * Searches the tree and returns the value of the position.
     *
     * @param board The position to search
     * @param isMax Whether this node maximises
     * @param alpha The best value the maximising side can already secure
     * @param beta The best value the minimising side can already secure
     * @param depth Plies left to search
     * @param scoreWeight Weight of the score difference
     * @param emptyWeight Weight of the opponent's empty holes
     * @param oneSeedWeight Weight of the opponent's one-seed holes
     * @param twoSeedWeight Weight of the opponent's two-seed holes
     * @param krouWeight Weight of our own krous
     * @param history Cut-off statistics to read and update, or null for no ordering
     * @param probeScore Whether to run the extra capture simulation per node; see
     *        the field comment for why some bots do and some do not
     * @return The value of the position
     */
    public static double search (Board board, boolean isMax, double alpha, double beta,
            double depth, double scoreWeight, double emptyWeight, double oneSeedWeight,
            double twoSeedWeight, double krouWeight, Map <Integer, Integer> history,
            boolean probeScore)
    {
        if (depth == 0)
            return Heuristic.evaluate (board, isMax, scoreWeight, emptyWeight, oneSeedWeight,
                    twoSeedWeight, krouWeight);

        double [] decision = new double [Board.NB_HOLES];
        Integer [] order = (history == null)
                ? AlphaBetaSearch.NATURAL_ORDER
                : MoveOrdering.rank (board, history);
        double best = isMax ? -Double.MAX_VALUE : Double.MAX_VALUE;

        for (int i = order.length - 1; i >= 0; i--)
        {
            int hole = order[i];
            if (board.getPlayerHoles ()[hole] == 0)
                continue;

            Board copy = (Board) board.clone ();
            try
            {
                decision[hole] = 1;
                if (probeScore)
                    board.playMoveSimulationScore (board.getCurrentPlayer (), decision);
                copy = copy.playMoveSimulationBoard (board.getCurrentPlayer (), decision);
            }
            catch (InvalidBotException e)
            {
                e.printStackTrace ();
            }

            double value = AlphaBetaSearch.search (copy, !isMax, alpha, beta, depth - 1,
                    scoreWeight, emptyWeight, oneSeedWeight, twoSeedWeight, krouWeight, history,
                    probeScore);
            if (isMax)
            {
                best = Math.max (best, value);
                alpha = Math.max (alpha, value);
            }
            else
            {
                best = Math.min (best, value);
                beta = Math.min (beta, value);
            }

            if (beta <= alpha)
            {
                if (history != null)
                    MoveOrdering.recordCutoff (board, hole, history);
                break;
            }
            if (history != null)
                MoveOrdering.recordMiss (board, hole, history);
            decision[hole] = 0;
        }
        return best;
    }
}
