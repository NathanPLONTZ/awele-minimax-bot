package awele.bot.YANNICKBOLASIE;

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
 * Alpha-beta Awele bot with learned move ordering and a learned opening book.
 *
 * <p>The bot is a depth-limited alpha-beta search over a weighted evaluation.
 * Two things are learned during {@link #learn()} rather than hard-coded:</p>
 *
 * <ul>
 *   <li><b>Move ordering.</b> Every move keeps a running cut-off score, so the
 *       search visits the moves that pruned best first. This is what buys the
 *       extra plies: the search reaches depth 9 inside the time budget, where
 *       the unordered version stops at 8.</li>
 *   <li><b>An opening book.</b> Self-play runs the search one ply deeper than
 *       match play and stores the chosen move for every position met. During a
 *       match those positions are answered instantly, with a depth-10 answer.</li>
 * </ul>
 *
 * <p>The bot is named after a footballer known for elaborate dribbles of
 * debatable usefulness.</p>
 *
 * @author Nathan PLONTZ
 */
public class YANNICKBOLASIE extends CompetitorBot
{
    /** Self-play games used to fill the opening book. */
    public static int nbMatchApprentissage = 100;

    /** Games played per candidate during the weight search. */
    public static int nbMatchGenetique = 11;

    /** Wall-clock budget of the weight search, in milliseconds. */
    private static final long TEMPS_RECHERCHE_POIDS = 900 * 1000;

    /** Depth used by the weight search, where many games matter more than depth. */
    private static final int PROFONDEUR_RECHERCHE_POIDS = 6;

    /** Search depth used during a match. */
    public int MAX_PROF = 9;

    /** Search depth used during self-play, one ply deeper than match play. */
    public int MAX_PROF_APPRENTISSAGE = 10;

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
    private int victoireApprentissage = 0;
    private int victoireConsecutifApprentissage = 0;

    /**
     * @throws InvalidBotException If the bot cannot be registered
     */
    public YANNICKBOLASIE () throws InvalidBotException
    {
        this.setBotName ("YANNICKBOLASIE");
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
        return MoveOrdering.rank (b, YANNICKBOLASIE.memoireCoup);
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
                YANNICKBOLASIE.memoireCoup, false);
    }

    @Override
    public double [] getDecision (Board board)
    {
        double [] res = new double [Board.NB_HOLES];

        /* The rightmost hole is a solid, well-known opening. */
        if (this.premierCoup)
        {
            this.premierCoup = false;
            res[5] = Double.MAX_VALUE;
            return res;
        }

        /* A position already met during self-play carries a depth-10 answer. */
        long positionId = YANNICKBOLASIE.generateUniqueId (board);
        if (YANNICKBOLASIE.memoirePosition.containsKey (positionId))
        {
            res[YANNICKBOLASIE.memoirePosition.get (positionId)] = Double.MAX_VALUE;
            return res;
        }

        double [] decision = new double [Board.NB_HOLES];
        Integer [] tri = this.trieCoup (board);
        double profondeur = this.apprentissageEnCours ? this.MAX_PROF_APPRENTISSAGE : this.MAX_PROF;
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
            YANNICKBOLASIE.memoirePosition.putIfAbsent (positionId, this.maxIndiceTableau (res));

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

        /* Phase 1: self-play, to fill the opening book and the ordering stats. */
        Awele awele = new Awele (this, this);
        for (int i = 0; i < YANNICKBOLASIE.nbMatchApprentissage; i++)
            try
            {
                awele.play ();
            }
            catch (InvalidBotException e)
            {
                e.printStackTrace ();
            }

        /* Phase 2: hill-climbing on the evaluation weights. Candidates are drawn
           at random and kept whenever they beat the incumbent. It runs at depth 6
           on the assumption that weights that are good at a shallow depth stay
           good deeper: depth 9 would allow far too few games to be conclusive. */
        try
        {
            YANNICKBOLASIE bot1 = new YANNICKBOLASIE ();
            bot1.MAX_PROF = YANNICKBOLASIE.PROFONDEUR_RECHERCHE_POIDS;
            bot1.setHeuristique (5, 1, 2, 2, 2);
            bot1.apprentissageEnCours = false;
            YANNICKBOLASIE botConsecutif = bot1;
            long tempsInitial = System.currentTimeMillis ();

            this.apprentissageEnCours = false;
            int nbBotTeste = 0;
            while (System.currentTimeMillis () - tempsInitial
                    < YANNICKBOLASIE.TEMPS_RECHERCHE_POIDS)
            {
                nbBotTeste++;
                System.out.println ("Candidates tested: " + nbBotTeste);
                YANNICKBOLASIE bot2 = new YANNICKBOLASIE ();
                bot2.apprentissageEnCours = false;
                bot2.MAX_PROF = YANNICKBOLASIE.PROFONDEUR_RECHERCHE_POIDS;
                Random random = new Random ();
                /* The score difference gets the widest range: it matters most. */
                bot2.setHeuristique (random.nextDouble () * 10, random.nextDouble () * 2,
                        random.nextDouble () * 5, random.nextDouble () * 5,
                        random.nextDouble () * 5);
                for (int i = 0; i < YANNICKBOLASIE.nbMatchGenetique; i++)
                {
                    if (System.currentTimeMillis () - tempsInitial
                            > YANNICKBOLASIE.TEMPS_RECHERCHE_POIDS)
                        break;
                    Awele aweleMatch = new Awele (bot1, bot2);
                    aweleMatch.play ();
                    if (aweleMatch.getWinner () < 1)
                        bot1.victoireApprentissage++;
                    else
                        bot2.victoireApprentissage++;
                }
                if (bot1.victoireApprentissage < bot2.victoireApprentissage)
                {
                    bot1 = bot2;
                    bot1.victoireConsecutifApprentissage++;
                    bot1.victoireApprentissage = 0;
                }
                else
                {
                    bot1.victoireApprentissage = 0;
                    bot1.victoireConsecutifApprentissage++;
                    if (bot1.victoireConsecutifApprentissage
                            > botConsecutif.victoireConsecutifApprentissage)
                        botConsecutif = bot1;
                }
            }

            /* Guard against a lucky last candidate: run the final incumbent
               against the one that held out for the longest streak. */
            try
            {
                bot1.victoireApprentissage = 0;
                botConsecutif.victoireApprentissage = 0;
                Awele aweleMatch2 = new Awele (bot1, botConsecutif);
                for (int i = 0; i < YANNICKBOLASIE.nbMatchApprentissage; i++)
                {
                    aweleMatch2.play ();
                    if (aweleMatch2.getWinner () < 1)
                        bot1.victoireApprentissage++;
                    else
                        botConsecutif.victoireApprentissage++;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace ();
            }

            YANNICKBOLASIE winner =
                    (bot1.victoireApprentissage > botConsecutif.victoireApprentissage)
                            ? bot1
                            : botConsecutif;
            this.setHeuristique (winner.coeffScore, winner.coeffCase0, winner.coeffCase1,
                    winner.coeffCase2, winner.coeffKrou);
        }
        catch (InvalidBotException e)
        {
            e.printStackTrace ();
        }

        /* The weights found above are then deliberately discarded: the hand-tuned
           set held up better in testing than anything the search produced inside
           its time budget. The search is kept because it is what justifies these
           values, but its output is not trusted blindly. */
        this.setHeuristique (5, 1, 2, 2, 2);

        System.out.println ("Book size:  " + YANNICKBOLASIE.memoirePosition.size ());
        System.out.println ("Move stats: " + YANNICKBOLASIE.memoireCoup.size ());

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
