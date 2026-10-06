package awele.bot.YANNICKBOLASIE2;

import java.util.HashMap;
import java.util.Random;

import awele.bot.CompetitorBot;
import awele.bot.DemoBot;
import awele.bot.common.Heuristic;
import awele.bot.minmax.MinMaxNode;
import awele.core.Awele;
import awele.core.Board;
import awele.core.InvalidBotException;

/**
 * Ablation bot: plain alpha-beta, nothing learned.
 *
 * <p>The baseline the other variants are measured against. No move ordering, no
 * opening book, no weight search: a fixed-depth alpha-beta on the material-only
 * evaluation, with an empty {@link #learn()}.</p>
 *
 * <p>The evaluation it uses is not the weighted one of the competition bot. It
 * is the material-only function of the NegaMax variant, reused here to compare
 * the two evaluations at equal depth.</p>
 *
 * @author Nathan PLONTZ
 */
public class YANNICKBOLASIE2 extends CompetitorBot
{
    /** Search depth used during a match. */
    public final static int MAX_PROF = 9;

    private boolean premierCoup;

    /**
     * @throws InvalidBotException If the bot cannot be registered
     */
    public YANNICKBOLASIE2 () throws InvalidBotException
    {
        this.setBotName ("YANNICKBOLASIE2");
        this.addAuthor ("Nathan PLONTZ");
    }

    @Override
    public void initialize ()
    {
        //System.out.println ("INITIALIZE");
        this.premierCoup = true;
    }

    /**
     * Counts the opponent's empty, one-seed and two-seed holes, and our krous.
     *
     * <p>Kept for comparison with the weighted bots; this bot's own evaluation
     * does not use it.</p>
     *
     * @param board The board
     * @return The feature vector, in that order
     */
    public int [] donneeHeuristique (Board board)
    {
        return Heuristic.features (board);
    }

    /**
     * Evaluates a position with the material-only function.
     *
     * <p>The {@code isMax} flag is accepted for signature compatibility with the
     * other bots but is not used: the value is always read from the side to
     * move, and since every leaf of a fixed-depth search sits at the same parity
     * they all share one point of view.</p>
     *
     * @param board The board
     * @param isMax Unused
     * @return The evaluation
     */
    public double heuristique (Board board, boolean isMax)
    {
        return Heuristic.evaluateMaterial (board, board.getCurrentPlayer (),
                Board.otherPlayer (board.getCurrentPlayer ()));
    }

    /**
     * Plain alpha-beta, in the fixed right-to-left move order.
     *
     * @param board The position to search
     * @param isMax Whether this node maximises
     * @param alpha The alpha bound
     * @param beta The beta bound
     * @param profondeur Plies left to search
     * @return The value of the position
     */
    public double alphaBeta (Board board, boolean isMax, double alpha, double beta,
            double profondeur)
    {
        if (profondeur == 0)
            return this.heuristique (board, isMax);

        double [] decision = new double [Board.NB_HOLES];
        double best = isMax ? -Double.MAX_VALUE : Double.MAX_VALUE;
        for (int i = Board.NB_HOLES - 1; i >= 0; i--)
        {
            if (board.getPlayerHoles ()[i] == 0)
                continue;

            Board copy = (Board) board.clone ();
            try
            {
                decision[i] = 1;
                copy = copy.playMoveSimulationBoard (board.getCurrentPlayer (), decision);
            }
            catch (InvalidBotException e)
            {
                e.printStackTrace ();
            }

            double value = this.alphaBeta (copy, !isMax, alpha, beta, profondeur - 1);
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
                break;
            decision[i] = 0;
        }
        return best;
    }

    @Override
    public double [] getDecision (Board board)
    {
        double [] res = new double [Board.NB_HOLES];

        if (this.premierCoup)
        {
            this.premierCoup = false;
            res[5] = Double.MAX_VALUE;
            return res;
        }

        double [] decision = new double [Board.NB_HOLES];
        for (int i = 0; i < Board.NB_HOLES; i++)
            if (board.getPlayerHoles ()[i] != 0)
            {
                decision[i] = 1;
                Board copy = null;
                int score = 0;
                try
                {
                    score = board.playMoveSimulationScore (board.getCurrentPlayer (), decision);
                    copy = board.playMoveSimulationBoard (board.getCurrentPlayer (), decision);
                }
                catch (InvalidBotException e)
                {
                    e.printStackTrace ();
                }
                res[i] = this.alphaBeta (copy, false, -Double.MAX_VALUE, Double.MAX_VALUE,
                        YANNICKBOLASIE2.MAX_PROF);
                decision[i] = 0;
            }

        return res;
    }

    @Override
    public void learn ()
    {
        System.out.println ("LEARN");
    }

    @Override
    public void finish ()
    {
        //System.out.println ("FINISH");
    }
}
