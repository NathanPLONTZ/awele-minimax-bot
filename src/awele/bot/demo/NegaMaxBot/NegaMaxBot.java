package awele.bot.demo.NegaMaxBot;

import awele.bot.CompetitorBot;
import awele.core.Board;
import awele.core.InvalidBotException;

/**
 * Negamax variant with alpha-beta pruning.
 *
 * <p>The same search as the competition bot in substance, written in the negamax
 * form: a single node type that negates the value returned by its children,
 * instead of alternating between a min node and a max node. It uses the
 * material-only evaluation and learns nothing.</p>
 *
 * @author Nathan PLONTZ
 */
public class NegaMaxBot extends CompetitorBot
{
    /** Search depth. */
    private static final int MAX_DEPTH = 9;

    /**
     * @throws InvalidBotException If the bot cannot be registered
     */
    public NegaMaxBot () throws InvalidBotException
    {
        this.setBotName ("NegaMax3 Profondeur = " + NegaMaxBot.MAX_DEPTH);
        this.addAuthor ("Nathan PLONTZ");
    }

    @Override
    public void initialize ()
    {
        NegaMaxNode.initialize (NegaMaxBot.MAX_DEPTH);
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
        return new NegaMaxNode (board, 0, board.getCurrentPlayer (),
                Board.otherPlayer (board.getCurrentPlayer ()), -9999, 9999).getDecision ();
    }

    @Override
    public void finish ()
    {
    }
}
