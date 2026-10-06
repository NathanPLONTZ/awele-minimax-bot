package awele.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * The state of the board, and the rules that move it forward.
 *
 * <p>Twelve holes, two rows of six, four seeds in each at the start. Sowing runs
 * anticlockwise and skips the hole it started from once it has gone all the way
 * round. A move captures when its last seed lands on the opponent's side in a
 * hole that then holds two or three seeds, and the capture walks backwards for
 * as long as that stays true.</p>
 *
 * <p>Two rules constrain what may be played: a move must leave the opponent
 * something to play if they would otherwise have nothing, and a move that would
 * capture every one of the opponent's seeds is legal but captures none.</p>
 *
 * @author Nathan PLONTZ
 */
public class Board
{
    /**
     * Number of holes on each side of the board
     */
    public static final int NB_HOLES = 6;
    private static final int NB_SEEDS = 4;
    public int [][] holes;
    private int [] score;
    int currentPlayer;
    private List<List<Integer>> log;
    private Random random;

    /**
     * Builds the opening position: four seeds in each of the twelve holes.
     */
    public Board ()
    {
        this.random = new Random (1 + System.currentTimeMillis ());
        this.score = new int [2];
        this.holes = new int [2][Board.NB_HOLES];
        for (int i = 0; i < Board.NB_HOLES; i++)
        {
            this.holes [0][i] = Board.NB_SEEDS;
            this.holes [1][i] = Board.NB_SEEDS;
        }
        this.log = new ArrayList<List<Integer>> ();
        this.log.add (new ArrayList<Integer> ());
        this.log.add (new ArrayList<Integer> ());
    }

    /**
     * @param player The index of a player
     * @return The moves that player has played, in order
     */
    public int [] getLog (int player)
    {
        return this.log.get (player).stream ().mapToInt (Integer::intValue).toArray ();
    }

    /**
     * @return The number of seeds still in play
     */
    public int getNbSeeds ()
    {
        int sum = 0;
        for (int i = 0; i < Board.NB_HOLES; i++)
            sum += this.holes [0][i] + this.holes [1][i];
        return sum;
    }

    /**
     * @return The number of seeds on the side of the player to move
     */
    public int getPlayerSeeds ()
    {
        int sum = 0;
        for (int i = 0; i < Board.NB_HOLES; i++)
            sum += this.holes [this.currentPlayer][i];
        return sum;
    }

    /**
     * @return The number of seeds on the opponent's side
     */
    public int getOpponentSeeds ()
    {
        int sum = 0;
        for (int i = 0; i < Board.NB_HOLES; i++)
            sum += this.holes [Board.otherPlayer (this.currentPlayer)][i];
        return sum;
    }

    /**
     * @return The seeds in each hole of the player to move
     */
    public int [] getPlayerHoles ()
    {
        int [] holes = new int [this.holes [this.currentPlayer].length];
        for (int i = 0; i < holes.length; i++)
            holes [i] = this.holes [this.currentPlayer][i];
        return holes;
    }

    /**
     * @return The seeds in each hole of the opponent
     */
    public int [] getOpponentHoles ()
    {
        int otherPlayer = Board.otherPlayer (this.currentPlayer);
        int [] holes = new int [this.holes [otherPlayer].length];
        for (int i = 0; i < holes.length; i++)
            holes [i] = this.holes [otherPlayer][i];
        return holes;
    }

    void setCurrentPlayer (int currentPlayer)
    {
        this.currentPlayer = currentPlayer;
    }

    void changeCurrentPlayer ()
    {
        this.setCurrentPlayer (Board.otherPlayer (this.currentPlayer));
    }

    /**
     * @param player The index of a player
     * @return The index of the other player
     */
    public static int otherPlayer (int player)
    {
        return 1 - player;
    }

    int getNbSeeds (int player)
    {
        int sum = 0;
        for (int i = 0; i < Board.NB_HOLES; i++)
            sum += this.holes [player][i];
        return sum;
    }

    /**
     * @param player The index of a player
     * @return Whether that player has no seed left
     */
    public boolean isEmpty (int player)
    {
        return this.getNbSeeds (player) == 0;
    }

    /**
     * Lists the legal moves.
     *
     * <p>A hole can be played if it is not empty, and either the opponent still
     * has seeds or the move reaches their side and feeds them.</p>
     *
     * @param player The index of a player
     * @return One flag per hole, true when the move is legal
     */
    public boolean [] validMoves (int player)
    {
        boolean [] valid = new boolean [Board.NB_HOLES];
        boolean notEmpty = !this.isEmpty (Board.otherPlayer (player));
        for (int i = 0; i < Board.NB_HOLES; i++)
            valid [i] = (this.holes [player][i] > 0)
                    && (notEmpty || (i + this.holes [player][i] >= Board.NB_HOLES));
        return valid;
    }

    /**
     * Picks the highest-rated legal move, breaking ties at random.
     */
    private int selectMove (int player, double [] decision) throws InvalidBotException
    {
        boolean [] valid = this.validMoves (player);
        int bestMove = -1;
        double bestDecision = Double.NEGATIVE_INFINITY;
        int nbBest = 0;
        for (int i = 0; i < Board.NB_HOLES; i++)
            if (Double.isNaN (decision [i]))
                throw new InvalidBotException ("NaN in the decision vector");
            else if (valid [i])
                if (decision [i] > bestDecision)
                {
                    bestDecision = decision [i];
                    nbBest = 1;
                }
                else if (decision [i] == bestDecision)
                    nbBest++;
        if (nbBest > 0)
        {
            int select = this.random.nextInt (nbBest);
            loop:
                for (int i = 0; i < Board.NB_HOLES; i++)
                    if (valid [i] && (decision [i] == bestDecision))
                        if (select == 0)
                        {
                            bestMove = i;
                            break loop;
                        }
                        else
                            select--;
        }
        return bestMove;
    }

    /**
     * Whether the capture would sweep every seed from the opponent's side, in
     * which case the move stays legal but captures nothing.
     */
    private boolean takeAll (int player, int currentHole)
    {
        boolean takeAll = true;
        int opponent = Board.otherPlayer (player);
        for (int i = 0; i <= currentHole; i++)
            if ((this.holes [opponent][i] == 1) || (this.holes [opponent][i] > 3))
                takeAll = false;
        for (int i = currentHole + 1; i < Board.NB_HOLES; i++)
            if (this.holes [opponent][i] != 0)
                takeAll = false;
        return takeAll;
    }

    void addLog (int currentPlayer, int move)
    {
        this.log.get (currentPlayer).add (move);
    }

    int playMove (int player, double [] decision) throws InvalidBotException
    {
        int score = 0;
        int bestMove = this.selectMove (player, decision);
        if (bestMove >= 0)
        {
            this.addLog (player, bestMove);
            int nbSeeds = this.holes [player][bestMove];
            this.holes [player][bestMove] = 0;
            int currentSide = player;
            int currentHole = bestMove;
            /* Sow one seed per hole, skipping the hole we started from. */
            while (nbSeeds > 0)
            {
                currentHole++;
                if (currentHole >= Board.NB_HOLES)
                {
                    currentSide = Board.otherPlayer (currentSide);
                    currentHole = 0;
                }
                if ((currentSide != player) || (currentHole != bestMove))
                {
                    this.holes [currentSide][currentHole]++;
                    nbSeeds--;
                }
            }
            /* Capture, then walk backwards while the holes still hold two or three. */
            if ((currentSide == Board.otherPlayer (player))
                    && ((this.holes [currentSide][currentHole] == 2) || (this.holes [currentSide][currentHole] == 3))
                    && !this.takeAll (player, currentHole))
            {
                while ((currentHole >= 0)
                        && ((this.holes [currentSide][currentHole] == 2) || (this.holes [currentSide][currentHole] == 3)))
                {
                    score += this.holes [currentSide][currentHole];
                    this.holes [currentSide][currentHole] = 0;
                    currentHole--;
                }
            }
        }
        else
            score = -1;
        /* No legal move: the player collects whatever is left on their side. */
        if (score < 0)
        {
            this.score [this.currentPlayer] += this.getNbSeeds (this.currentPlayer);
            Arrays.fill (this.holes [this.currentPlayer], 0);
        }
        else
        {
            this.score [this.currentPlayer] += score;
            this.changeCurrentPlayer ();
        }
        return score;
    }

    /**
     * @return The index of the player to move
     */
    public int getCurrentPlayer ()
    {
        return this.currentPlayer;
    }

    /**
     * Plays a move on a copy of the board and returns what it captures.
     *
     * @param player The index of the player making the move
     * @param decision Six values rating each of the six possible moves
     * @return The number of seeds captured
     * @throws InvalidBotException If the decision vector is not usable
     */
    public int playMoveSimulationScore (int player, double [] decision) throws InvalidBotException
    {
        Board clone = (Board) this.clone ();
        return clone.playMove (player, decision);
    }

    /**
     * Plays a move on a copy of the board and returns the resulting position.
     *
     * @param player The index of the player making the move
     * @param decision Six values rating each of the six possible moves
     * @return The position after the move
     * @throws InvalidBotException If the decision vector is not usable
     */
    public Board playMoveSimulationBoard (int player, double [] decision) throws InvalidBotException
    {
        Board clone = (Board) this.clone ();
        clone.playMove (player, decision);
        return clone;
    }

    @Override
    public String toString ()
    {
        String string = "|";
        for (int i = Board.NB_HOLES - 1; i >= 0; i--)
        {
            if (this.holes [1][i] < 10)
                string += " ";
            string += this.holes [1][i] + "|";
        }
        string += "\n|";
        for (int i = 0; i < Board.NB_HOLES; i++)
        {
            if (this.holes [0][i] < 10)
                string += " ";
            string += this.holes [0][i] + "|";
        }
        return string;
    }

    @Override
    public Object clone ()
    {
        Board clone = new Board ();
        clone.currentPlayer = this.currentPlayer;
        clone.score [0] = this.score [0];
        clone.score [1] = this.score [1];
        for (int i = 0; i < Board.NB_HOLES; i++)
        {
            clone.holes [0][i] = this.holes [0][i];
            clone.holes [1][i] = this.holes [1][i];
        }
        clone.log = new ArrayList<List<Integer>> ();
        clone.log.add (new ArrayList<Integer> ());
        for (Integer i: this.log.get (0))
            clone.log.get (0).add (i);
        clone.log.add (new ArrayList<Integer> ());
        for (Integer i: this.log.get (1))
            clone.log.get (1).add (i);
        return clone;
    }

    /**
     * @param player The index of a player
     * @return That player's score
     */
    public int getScore (int player)
    {
        return this.score [player];
    }
}
