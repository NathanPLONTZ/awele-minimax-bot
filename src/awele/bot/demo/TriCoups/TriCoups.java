package awele.bot.demo.TriCoups;

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
import awele.bot.common.MoveOrdering;
import awele.core.Awele;
import awele.core.Board;
import awele.core.InvalidBotException;

/**
 * Ablation bot: move ordering only, no opening book.
 *
 * <p>Isolates the contribution of the cut-off-driven move ordering. Comparing it
 * against {@code MemoirePosition}, which has the opening book but no ordering,
 * shows which of the two learned components does the heavy lifting.</p>
 *
 * @author Nathan PLONTZ
 */
public class TriCoups extends DemoBot
{
    /** Self-play games used to build the ordering statistics. */
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

    /** Cut-off statistics driving move ordering: move key to score. */
    public static HashMap <Integer, Integer> memoireCoup = new HashMap <> ();

    private boolean premierCoup;
    private boolean apprentissageEnCours = true;

    /**
     * @throws InvalidBotException If the bot cannot be registered
     */
    public TriCoups () throws InvalidBotException
    {
        this.setBotName ("TriCoups");
        this.addAuthor ("Nathan PLONTZ");
    }

    /**
     * Identifies a move by its start hole, its seed count and its landing hole.
     *
     * @param b The board
     * @param coupDeDepart The hole the move starts from
     * @return The move key
     */
    public int idCoups (Board b, int coupDeDepart)
    {
        return BoardSignature.moveId (b, coupDeDepart);
    }

    /**
     * Ranks the moves by their recorded cut-off score, worst first.
     *
     * @param b The board
     * @return The hole indices, in ascending cut-off order
     */
    public Integer [] trieCoup (Board b)
    {
        return MoveOrdering.rank (b, TriCoups.memoireCoup);
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
     * Alpha-beta search with learned move ordering.
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
                this.coeffCase0, this.coeffCase1, this.coeffCase2, this.coeffKrou,
                TriCoups.memoireCoup, true);
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
        Integer [] tri = this.trieCoup (board);
        double profondeur = this.apprentissageEnCours
                ? TriCoups.MAX_PROF_APPRENTISSAGE
                : TriCoups.MAX_PROF;
        for (int i = Board.NB_HOLES - 1; i >= 0; i--)
            if (board.getPlayerHoles ()[tri[i]] != 0)
            {
                decision[tri[i]] = 1;
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
                res[tri[i]] = this.alphaBeta (copy, false, -Double.MAX_VALUE, Double.MAX_VALUE,
                        profondeur);
                decision[tri[i]] = 0;
            }

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
        for (int i = 0; i < TriCoups.nbMatchApprentissage; i++)
        {
            System.out.println ("Game " + i);
            System.out.println ("Move stats " + TriCoups.memoireCoup.size ());
            try
            {
                awele.play ();
            }
            catch (InvalidBotException e)
            {
                e.printStackTrace ();
            }
        }

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
