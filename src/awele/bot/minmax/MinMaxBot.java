package awele.bot.minmax;

import awele.bot.CompetitorBot;
import awele.core.Board;
import awele.core.InvalidBotException;

/**
 * The reference minimax bot, used as a yardstick.
 *
 * <p>Fixed depth, score difference as the only evaluation, and nothing learned.
 * Beating it is the minimum bar; the interesting comparison is at equal depth,
 * since this search is far slower per node than the competition bot's.</p>
 *
 * @author Nathan PLONTZ
 */
public class MinMaxBot extends CompetitorBot
{
    /** Search depth. */
    private static final int MAX_DEPTH = 6;

    /** Debug counter: decisions taken. */
    private int i = 0;

    /**
     * @throws InvalidBotException If the bot cannot be registered
     */
    public MinMaxBot () throws InvalidBotException
    {
        this.setBotName ("MinMax");
        this.addAuthor ("Nathan PLONTZ");
    }

    @Override
    public void initialize ()
    {
    }

    /**
     * No learning phase.
     */
    @Override
    public void learn ()
    {
    }

    @Override
    public double [] getDecision (Board board)
    {
        this.i++;
        MinMaxNode.initialize (board, MinMaxBot.MAX_DEPTH);
        return new MaxNode (board).getDecision ();
    }

    @Override
    public void finish ()
    {
    }
}
