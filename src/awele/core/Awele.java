package awele.core;

import awele.bot.Bot;
import awele.output.OutputWriter;

/**
 * One encounter between two bots.
 *
 * <p>An encounter is two games, so that each bot opens once, and the winner is
 * the one holding more seeds over the pair. A game ends when a player reaches 25
 * seeds, when fewer than six seeds are left on the board, when a player has no
 * legal move, or when nothing has been captured for a thousand moves.</p>
 *
 * @author Nathan PLONTZ
 */
public class Awele extends OutputWriter
{
    /** Moves without a capture after which the game is called off. */
    private static final int MAX_STAGNANT = 1000;

    private Bot [] players;
    private int [] scores;
    private double nbMoves;
    private long runningTime;

    /**
     * @param player1 The first bot
     * @param player2 The second bot
     */
    public Awele (Bot player1, Bot player2)
    {
        this.players = new Bot [2];
        this.players [0] = player1;
        this.players [1] = player2;
        this.scores = new int [2];
        this.nbMoves = 0;
        this.runningTime = 0;
    }

    private int [] game (int firstPlayer) throws InvalidBotException
    {
        boolean end = false;
        Board board = new Board ();
        board.setCurrentPlayer (firstPlayer);
        this.printDebug ();
        this.printDebug (board);
        this.printDebug ("Score: " + board.getScore (0) + " - " + board.getScore (1));
        int nbStagnant = 0;
        while (!end)
        {
            int currentPlayer = board.getCurrentPlayer ();
            this.nbMoves += 1;
            double [] decision = this.players [currentPlayer].getDecision (board);
            int moveScore = board.playMove (currentPlayer, decision);
            if (moveScore > 0)
                nbStagnant = 0;
            else
                nbStagnant++;
            if ((moveScore < 0)
                    || (board.getScore (Board.otherPlayer (board.getCurrentPlayer ())) >= 25)
                    || (board.getNbSeeds () <= 6)
                    || (nbStagnant >= Awele.MAX_STAGNANT))
                end = true;
            this.printDebug ();
            this.printDebug (board);
            this.printDebug ("Score: " + board.getScore (0) + " - " + board.getScore (1));
        }
        this.printDebug ();
        int [] score = new int [2];
        score [0] = board.getScore (0);
        score [1] = board.getScore (1);
        return score;
    }

    /**
     * @return The average number of moves played per game
     */
    public double getNbMoves ()
    {
        return this.nbMoves;
    }

    /**
     * @return The average duration of a game, in milliseconds
     */
    public long getRunningTime ()
    {
        return this.runningTime;
    }

    /**
     * Plays the two games of the encounter, swapping who opens.
     *
     * @throws InvalidBotException If a bot returns an unusable decision
     */
    public void play () throws InvalidBotException
    {
        this.print ("Game 1");
        long start = System.currentTimeMillis ();
        this.players [0].initialize ();
        this.players [1].initialize ();
        int [] game1Score = this.game (0);
        this.players [0].finish ();
        this.players [1].finish ();
        this.runningTime += System.currentTimeMillis () - start;
        this.print ("Score: " + game1Score [0] + " - " + game1Score [1]);

        this.print ("Game 2");
        start = System.currentTimeMillis ();
        this.players [0].initialize ();
        this.players [1].initialize ();
        int [] game2Score = this.game (1);
        this.players [0].finish ();
        this.players [1].finish ();
        this.runningTime += System.currentTimeMillis () - start;
        this.print ("Score: " + game2Score [0] + " - " + game2Score [1]);

        this.runningTime /= 2;
        this.nbMoves /= 2.;
        this.scores [0] = game1Score [0] + game2Score [0];
        this.scores [1] = game1Score [1] + game2Score [1];
        int winner = this.getWinner ();
        if (winner < 0)
            this.printDebug ("Draw");
        else
            this.printDebug ("Winner: " + this.players [winner]);
    }

    /**
     * @return 0 if the first bot won, 1 if the second did, -1 on a draw
     */
    public int getWinner ()
    {
        int winner = -1;
        if (this.scores [0] > this.scores [1])
            winner = 0;
        else if (this.scores [1] > this.scores [0])
            winner = 1;
        return winner;
    }
}
