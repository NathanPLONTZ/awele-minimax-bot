package awele.bot.demo.random;

import java.util.Random;

import awele.bot.DemoBot;
import awele.core.Board;
import awele.core.InvalidBotException;

/**
 * Bot that plays at random.
 *
 * <p>Used by the runner as a calibration opponent: the time it takes to play a
 * move is subtracted from every other bot's measured decision time, so that the
 * engine's own overhead is not charged to the bots.</p>
 *
 * @author Nathan PLONTZ
 */
public class RandomBot extends DemoBot
{
    private Random random;

    /**
     * @throws InvalidBotException If the bot cannot be registered
     */
    public RandomBot () throws InvalidBotException
    {
        this.setBotName ("Random");
        this.addAuthor ("Nathan PLONTZ");
    }

    @Override
    public void initialize ()
    {
    }

    /**
     * Gives every move a random priority.
     */
    @Override
    public double [] getDecision (Board board)
    {
        double [] decision = new double [Board.NB_HOLES];
        for (int i = 0; i < decision.length; i++)
            decision[i] = this.random.nextDouble ();
        return decision;
    }

    /**
     * Seeds the random number generator.
     */
    @Override
    public void learn ()
    {
        this.random = new Random (System.currentTimeMillis ());
    }

    @Override
    public void finish ()
    {
    }
}
