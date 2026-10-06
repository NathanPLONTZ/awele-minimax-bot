package awele.bot.common;

import awele.core.Board;

/**
 * Board evaluation shared by the alpha-beta bots.
 *
 * <p>The evaluation is a weighted sum of five features. The score difference
 * carries most of the signal; the three "vulnerable hole" counts measure how
 * much material the opponent is leaving within capture range; the krou count
 * measures how many of our own holes have grown past twelve seeds and can sweep
 * the whole board.</p>
 *
 * <p>The weights are deliberately passed as arguments rather than stored: the
 * evaluation runs at every leaf of the search, so it allocates nothing.</p>
 *
 * @author Nathan PLONTZ
 */
public final class Heuristic
{
    /** A hole holding at least this many seeds goes all the way round (a krou). */
    private static final int KROU_THRESHOLD = 12;

    /** Index of the opponent's empty-hole count in the feature vector. */
    public static final int EMPTY_HOLES = 0;

    /** Index of the opponent's one-seed-hole count in the feature vector. */
    public static final int ONE_SEED_HOLES = 1;

    /** Index of the opponent's two-seed-hole count in the feature vector. */
    public static final int TWO_SEED_HOLES = 2;

    /** Index of our own krou count in the feature vector. */
    public static final int OWN_KROUS = 3;

    private Heuristic ()
    {
    }

    /**
     * Counts the four board features used by the evaluation.
     *
     * @param board The board to describe
     * @return The feature vector, indexed by the constants of this class
     */
    public static int [] features (Board board)
    {
        int [] opponentHoles = board.getOpponentHoles ();
        int [] playerHoles = board.getPlayerHoles ();
        int [] features = new int [4];
        for (int i = 0; i < Board.NB_HOLES; i++)
        {
            if (opponentHoles[i] < 3)
            {
                if (opponentHoles[i] == 0)
                    features[Heuristic.EMPTY_HOLES]++;
                if (opponentHoles[i] == 1)
                    features[Heuristic.ONE_SEED_HOLES]++;
                if (opponentHoles[i] == 2)
                    features[Heuristic.TWO_SEED_HOLES]++;
            }
            if (playerHoles[i] >= Heuristic.KROU_THRESHOLD)
                features[Heuristic.OWN_KROUS]++;
        }
        return features;
    }

    /**
     * Evaluates a board from the point of view of the side that asked.
     *
     * <p>The features are always read from the side to move, so the value is
     * negated on a min node to bring it back to the maximising player's point
     * of view.</p>
     *
     * @param board The board to evaluate
     * @param isMax Whether the calling node maximises
     * @param scoreWeight Weight of the score difference
     * @param emptyWeight Weight of the opponent's empty holes
     * @param oneSeedWeight Weight of the opponent's one-seed holes
     * @param twoSeedWeight Weight of the opponent's two-seed holes
     * @param krouWeight Weight of our own krous
     * @return The evaluation
     */
    public static double evaluate (Board board, boolean isMax, double scoreWeight,
            double emptyWeight, double oneSeedWeight, double twoSeedWeight, double krouWeight)
    {
        int [] features = Heuristic.features (board);
        int scoreDiff = board.getScore (board.getCurrentPlayer ())
                - board.getScore (Board.otherPlayer (board.getCurrentPlayer ()));
        double value = scoreWeight * scoreDiff
                + emptyWeight * features[Heuristic.EMPTY_HOLES]
                + oneSeedWeight * features[Heuristic.ONE_SEED_HOLES]
                + twoSeedWeight * features[Heuristic.TWO_SEED_HOLES]
                + krouWeight * features[Heuristic.OWN_KROUS];
        if (isMax)
            return value;
        return -value;
    }

    /**
     * The alternative, material-only evaluation.
     *
     * <p>Instead of weighting features, this one scores each hole against fixed
     * thresholds: a krou is worth a little, an empty hole a lot, a nearly empty
     * hole somewhat less, and the same reading is applied to the opponent with
     * the opposite sign. The score difference then dominates everything with a
     * weight of 25.</p>
     *
     * <p>This is the evaluation used by the NegaMax variant; it is kept here so
     * that the two bots that use it share one definition.</p>
     *
     * @param board The board to evaluate
     * @param myTurn The index of the side the evaluation is for
     * @param opponentTurn The index of the other side
     * @return The evaluation
     */
    public static int evaluateMaterial (Board board, int myTurn, int opponentTurn)
    {
        int total = 0;
        int [] seedsPlayer = board.getPlayerHoles ();
        int [] seedsOpponent = board.getOpponentHoles ();
        for (int i = 0; i < Board.NB_HOLES; i++)
        {
            int seedP = seedsPlayer[i];
            int seedO = seedsOpponent[i];
            if (seedP > 12)
                total += 28;
            else if (seedP == 0)
                total -= 54;
            else if (seedP < 3)
                total -= 36;
            if (seedO > 12)
                total -= 28;
            else if (seedO == 0)
                total += 54;
            else if (seedO < 3)
                total += 36;
        }
        return (25 * (board.getScore (myTurn) - board.getScore (opponentTurn))) - total;
    }

    /**
     * @param values A vector of move evaluations
     * @return The index of the highest value, the first one if several tie
     */
    public static int argMax (double [] values)
    {
        double max = -Double.MAX_VALUE;
        int best = 0;
        for (int i = 0; i < values.length; i++)
            if (values[i] > max)
            {
                max = values[i];
                best = i;
            }
        return best;
    }
}
