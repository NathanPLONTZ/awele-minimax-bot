package awele.bot.demo.MemoirePosition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;

import awele.bot.CompetitorBot;
import awele.bot.DemoBot;
import awele.bot.common.AlphaBetaSearch;
import awele.bot.common.BoardSignature;
import awele.bot.common.Heuristic;
import awele.core.Awele;
import awele.core.Board;
import awele.core.InvalidBotException;

/**
 * Ablation bot: opening book only, no move ordering.
 *
 * <p>Isolates the contribution of the learned opening book. Comparing it against
 * {@code TriCoups}, which has the ordering but no book, shows which of the two
 * learned components does the heavy lifting.</p>
 *
 * <p><b>Known defect, deliberately preserved.</b> In {@link #getDecision(Board)}
 * the decision vector is only cleared on the match-play branch, so during
 * self-play several holes stay set to {@code 1} at once. The engine then sees an
 * ambiguous decision vector, falls back on its random tie-break, and the moves
 * actually simulated are not the ones intended. This makes the bot's output
 * non-reproducible from one run to the next. It is kept as-is so that this
 * experiment still reports what it reported at the time; it is not a pattern to
 * copy, and the competition bot does not share it.</p>
 *
 * @author Nathan PLONTZ
 */
public class MemoirePosition extends DemoBot
{
    /** Self-play games used to fill the opening book. */
    public static int nbMatchApprentissage = 20;

    /** Search depth used during a match. */
    public final static int MAX_PROF = 9;

    /** Search depth used during self-play. */
    public final static int MAX_PROF_APPRENTISSAGE = 9;

    /** Weight of the score difference. */
    public double coeffScore = 5;

    /** Weight of the opponent's empty holes. */
    public double coeffCase0 = 1;

    /** Weight of the opponent's one-seed holes. */
    public double coeffCase1 = 2;

    /** Weight of the opponent's two-seed holes. */
    public double coeffCase2 = 2;

    /** Weight of our own krous. */
    public double coeffKrou = 2;

    /** The opening book: position key to the move to play. */
    public static HashMap <Long, Integer> memoirePosition = new HashMap <> ();

    private boolean premierCoup;
    private boolean apprentissageEnCours = true;

    /**
     * @throws InvalidBotException If the bot cannot be registered
     */
    public MemoirePosition () throws InvalidBotException
    {
        this.setBotName ("MemoirePosition");
        this.addAuthor ("Nathan PLONTZ");
    }

    /**
     * Identifies a position, ignoring the scores.
     *
     * @param b The board
     * @return The position key
     */
    public static long generateUniqueId (Board b)
    {
        return BoardSignature.positionId (b);
    }

    @Override
    public void initialize ()
    {
        this.premierCoup = true;
    }

    /**
     * Counts the opponent's empty, one-seed and two-seed holes, and our krous.
     *
     * @param board The board
     * @return The feature vector, in that order
     */
    public int [] donneeHeuristique (Board board)
    {
        return Heuristic.features (board);
    }

    /**
     * @param tab A vector of move evaluations
     * @return The index of the best move
     */
    int maxIndiceTableau (double [] tab)
    {
        return Heuristic.argMax (tab);
    }

    /**
     * Evaluates a position from the point of view of the calling node.
     *
     * @param board The board
     * @param isMax Whether the calling node maximises
     * @return The evaluation
     */
    public double heuristique (Board board, boolean isMax)
    {
        return Heuristic.evaluate (board, isMax, this.coeffScore, this.coeffCase0,
                this.coeffCase1, this.coeffCase2, this.coeffKrou);
    }

    /**
     * Alpha-beta search in the fixed right-to-left move order.
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
        return AlphaBetaSearch.search (board, isMax, alpha, beta, profondeur, this.coeffScore,
                this.coeffCase0, this.coeffCase1, this.coeffCase2, this.coeffKrou, null, true);
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

        long positionId = MemoirePosition.generateUniqueId (board);
        if (MemoirePosition.memoirePosition.containsKey (positionId))
        {
            res[MemoirePosition.memoirePosition.get (positionId)] = Double.MAX_VALUE;
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
                if (this.apprentissageEnCours)
                    /* See the class comment: the missing reset of decision[i] on
                       this branch is the known defect, kept on purpose. */
                    res[i] = this.alphaBeta (copy, false, -Double.MAX_VALUE, Double.MAX_VALUE,
                            MemoirePosition.MAX_PROF_APPRENTISSAGE);
                else
                {
                    res[i] = this.alphaBeta (copy, false, -Double.MAX_VALUE, Double.MAX_VALUE,
                            MemoirePosition.MAX_PROF);
                    decision[i] = 0;
                }
            }

        if (this.apprentissageEnCours)
            MemoirePosition.memoirePosition.putIfAbsent (positionId, this.maxIndiceTableau (res));

        return res;
    }

    @Override
    public void learn ()
    {
        System.out.println ("LEARN");
        if (!this.apprentissageEnCours)
            return;

        long memoryBefore = Runtime.getRuntime ().totalMemory ()
                - Runtime.getRuntime ().freeMemory ();

        Awele awele = new Awele (this, this);
        for (int i = 0; i < MemoirePosition.nbMatchApprentissage; i++)
        {
            System.out.println ("Game " + i);
            System.out.println ("Book size " + MemoirePosition.memoirePosition.size ());
            try
            {
                awele.play ();
            }
            catch (InvalidBotException e)
            {
                e.printStackTrace ();
            }
        }

        System.out.println ("Book size after learning: "
                + MemoirePosition.memoirePosition.size ());
        long memoryAfter = Runtime.getRuntime ().totalMemory ()
                - Runtime.getRuntime ().freeMemory ();
        System.out.println ("Memory used during learning: "
                + (memoryAfter - memoryBefore) + " bytes");

        this.apprentissageEnCours = false;
    }

    @Override
    public void finish ()
    {
    }
}
