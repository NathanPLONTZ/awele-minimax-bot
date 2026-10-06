package awele.bot.common;

import java.util.Arrays;

import awele.core.Board;
import awele.data.AweleData;
import awele.data.AweleObservation;

/**
 * Nearest-neighbour lookup over the provided dataset.
 *
 * <p>Holds the recorded situations of one outcome — the winner's moves, or the
 * loser's — as twelve-dimensional points, and answers how many of a position's
 * nearest neighbours played each move. Two instances, one per outcome, give a
 * decision rule that rates a move by how often winners played it minus how often
 * losers did.</p>
 *
 * <p>This is the dataset-driven baseline the assignment asks for. It is kept as
 * the reference point the search-based bots are compared against: with only 303
 * recorded situations, nearest neighbours generalise poorly, which is what sent
 * this project towards tree search instead.</p>
 *
 * @author Nathan PLONTZ
 */
public class KnnData
{
    /** Number of board features in an observation: six holes each side. */
    private static final int NB_FEATURES = 2 * Board.NB_HOLES;

    /** Column holding the move, right after the board features. */
    private static final int MOVE_COLUMN = KnnData.NB_FEATURES;

    private final int [][] data;

    /**
     * Loads the observations of one outcome.
     *
     * @param won True to keep the moves played by the winner, false for the loser
     */
    public KnnData (boolean won)
    {
        AweleData observations = AweleData.getInstance ();
        int nb = 0;
        for (AweleObservation observation : observations)
            if (observation.isWon () == won)
                nb++;

        this.data = new int [nb][KnnData.NB_FEATURES + 1];
        int i = 0;
        for (AweleObservation observation : observations)
            if (observation.isWon () == won)
            {
                for (int j = 0; j < Board.NB_HOLES; j++)
                    this.data[i][j] = observation.getPlayerHoles ()[j];
                for (int j = 0; j < Board.NB_HOLES; j++)
                    this.data[i][j + Board.NB_HOLES] = observation.getOppenentHoles ()[j];
                /* Moves are recorded from one, hole indices start at zero. */
                this.data[i][KnnData.MOVE_COLUMN] = observation.getMove () - 1;
                i++;
            }
    }

    private static double squareDistance (int [] x1, int [] x2)
    {
        double dist = 0;
        for (int i = 0; i < KnnData.NB_FEATURES; i++)
        {
            int diff = x1[i] - x2[i];
            dist += diff * diff;
        }
        return dist;
    }

    private double [] squareDistances (int [] x)
    {
        double [] distances = new double [this.data.length];
        for (int i = 0; i < distances.length; i++)
            distances[i] = KnnData.squareDistance (x, this.data[i]);
        return distances;
    }

    private static double getThreshold (double [] distances, int k)
    {
        double [] copy = new double [distances.length];
        System.arraycopy (distances, 0, copy, 0, distances.length);
        Arrays.sort (copy);
        return copy[k];
    }

    /**
     * Counts, among the k nearest recorded situations, how many played each move.
     *
     * @param x A game situation, as twelve hole counts
     * @param k How many neighbours to consider
     * @return The neighbour count for each of the six moves
     */
    public double [] countNeighbors (int [] x, int k)
    {
        double [] neighbors = new double [Board.NB_HOLES];
        double [] distances = this.squareDistances (x);
        double threshold = KnnData.getThreshold (distances, k);
        for (int i = 0; i < distances.length; i++)
            if (distances[i] < threshold)
                neighbors[this.data[i][KnnData.MOVE_COLUMN]] += 1;
        return neighbors;
    }
}
