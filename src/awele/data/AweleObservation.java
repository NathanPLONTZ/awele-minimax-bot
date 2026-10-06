package awele.data;

import awele.core.Board;

/**
 * One recorded game situation from the provided dataset.
 *
 * <p>A line holds fourteen fields: six holes for the player to move, six for the
 * opponent, the move that was played, and whether that player went on to win the
 * game.</p>
 *
 * @author Nathan PLONTZ
 */
public class AweleObservation
{
    private int [] playerHoles;
    private int [] oppenentHoles;
    private int move;
    private boolean won;

    AweleObservation (String string)
    {
        String [] strings = string.split (",");
        this.playerHoles = new int [Board.NB_HOLES];
        for (int i = 0; i < Board.NB_HOLES; i++)
            this.playerHoles[i] = Integer.parseInt (strings[i]);
        this.oppenentHoles = new int [Board.NB_HOLES];
        for (int i = 0; i < Board.NB_HOLES; i++)
            this.oppenentHoles[i] = Integer.parseInt (strings[i + 6]);
        /* The move is recorded as "C" followed by the hole number. */
        this.move = Integer.parseInt (strings[12].charAt (1) + "");
        this.won = strings[13].equals ("G");
    }

    /**
     * @return The seeds in each hole of the player to move
     */
    public int [] getPlayerHoles ()
    {
        return this.playerHoles;
    }

    /**
     * @return The seeds in each hole of the opponent
     */
    public int [] getOppenentHoles ()
    {
        return this.oppenentHoles;
    }

    /**
     * @return The move that was played, numbered from one
     */
    public int getMove ()
    {
        return this.move;
    }

    /**
     * @return Whether the player to move won that game
     */
    public boolean isWon ()
    {
        return this.won;
    }
}
