package awele.bot.YANNICKBOLASIEBIS;

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
 * The competition bot without its weight search, kept as a sparring partner.
 *
 * <p>This variant has both learned components of {@code YANNICKBOLASIE} — the
 * move ordering and the opening book — but keeps the hand-tuned weights and
 * skips the weight search entirely, so it trains in seconds instead of a
 * quarter of an hour. It also searches one ply shallower.</p>
 *
 * <p>It extends {@code DemoBot}, so it never enters the championship.</p>
 *
 * @author Nathan PLONTZ
 */
public class YANNICKBOLASIEBIS extends DemoBot
{
    /** Self-play games used to fill the opening book. */
    public static int nbMatchApprentissage = 30;

    /** Search depth used during a match. */
    public final static int MAX_PROF = 8;

    /** Search depth used during self-play, one ply deeper than match play. */
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

    /** Cut-off statistics driving move ordering: move key to score. */
    public static HashMap <Integer, Integer> memoireCoup = new HashMap <> ();

    private boolean premierCoup;
    private boolean apprentissageEnCours = true;

    /**
     * @throws InvalidBotException If the bot cannot be registered
     */
    public YANNICKBOLASIEBIS () throws InvalidBotException
    {
        this.setBotName ("YANNICKBOLASIEBIS");
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
        return MoveOrdering.rank (b, YANNICKBOLASIEBIS.memoireCoup);
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
     * Sets the evaluation weights.
     *
     * @param coeffScore Weight of the score difference
     * @param coeffCase0 Weight of the opponent's empty holes
     * @param coeffCase1 Weight of the opponent's one-seed holes
     * @param coeffCase2 Weight of the opponent's two-seed holes
     * @param coeffKrou Weight of our own krous
     */
    public void setHeuristique (double coeffScore, double coeffCase0, double coeffCase1,
            double coeffCase2, double coeffKrou)
    {
        this.coeffScore = coeffScore;
        this.coeffCase0 = coeffCase0;
        this.coeffCase1 = coeffCase1;
        this.coeffCase2 = coeffCase2;
        this.coeffKrou = coeffKrou;
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
                YANNICKBOLASIEBIS.memoireCoup, false);
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

        long positionId = YANNICKBOLASIEBIS.generateUniqueId (board);
        if (YANNICKBOLASIEBIS.memoirePosition.containsKey (positionId))
        {
            res[YANNICKBOLASIEBIS.memoirePosition.get (positionId)] = Double.MAX_VALUE;
            return res;
        }

        double [] decision = new double [Board.NB_HOLES];
        Integer [] tri = this.trieCoup (board);
        double profondeur = this.apprentissageEnCours
                ? YANNICKBOLASIEBIS.MAX_PROF_APPRENTISSAGE
                : YANNICKBOLASIEBIS.MAX_PROF;
        for (int i = Board.NB_HOLES - 1; i >= 0; i--)
            if (board.getPlayerHoles ()[tri[i]] != 0)
            {
                decision[tri[i]] = 1;
                Board copy = null;
                //int score = 0;
                try
                {
                    //score = board.playMoveSimulationScore (board.getCurrentPlayer (), decision);
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

        if (this.apprentissageEnCours)
            YANNICKBOLASIEBIS.memoirePosition.putIfAbsent (positionId,
                    this.maxIndiceTableau (res));

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

        /* An earlier attempt kept here for the record: the weight search used to
           live in this bot too, before it moved to the competition bot.

           try {
               YANNICKBOLASIE bot1 = new YANNICKBOLASIE ();
               bot1.setHeuristique (5, 1, 2, 2, 2);
               YANNICKBOLASIE botConsecutif = null;
               long tempsInitial = System.currentTimeMillis ();
               while (System.currentTimeMillis () - tempsInitial < 300 * 1000) {
                   YANNICKBOLASIE bot2 = new YANNICKBOLASIE ();
                   Random random = new Random ();
                   bot2.setHeuristique (random.nextDouble () * 20, random.nextDouble () * 20,
                           random.nextDouble () * 20, random.nextDouble () * 20,
                           random.nextDouble () * 20);
                   for (int i = 0; i < nbMatchGenetique; i++) {
                       Awele awele = new Awele (bot1, bot2);
                       if (awele.getWinner () < 1) bot1.victoireApprentissage++;
                       else bot2.victoireApprentissage++;
                   }
                   if (bot1.victoireApprentissage < bot2.victoireApprentissage) {
                       bot1.coeffScore = bot2.coeffScore;
                       bot1.coeffCase0 = bot2.coeffCase0;
                       bot1.coeffCase1 = bot2.coeffCase1;
                       bot1.coeffCase2 = bot2.coeffCase2;
                       bot1.coeffKrou = bot2.coeffKrou;
                   }
               }
               this.coeffScore = bot1.coeffScore;
               this.coeffCase0 = bot1.coeffCase0;
               this.coeffCase1 = bot1.coeffCase1;
               this.coeffCase2 = bot1.coeffCase2;
               this.coeffKrou = bot1.coeffKrou;
           } catch (InvalidBotException e) {
               e.printStackTrace ();
           }
        */

        Awele awele = new Awele (this, this);
        for (int i = 0; i < YANNICKBOLASIEBIS.nbMatchApprentissage; i++)
            try
            {
                awele.play ();
            }
            catch (InvalidBotException e)
            {
                e.printStackTrace ();
            }

        System.out.println ("Book size:  " + YANNICKBOLASIEBIS.memoirePosition.size ());
        System.out.println ("Move stats: " + YANNICKBOLASIEBIS.memoireCoup.size ());

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
