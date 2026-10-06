package awele.bot.demo.NegaMaxBot;

import awele.bot.common.Heuristic;
import awele.core.Board;
import awele.core.InvalidBotException;

/**
 * A node of the negamax tree.
 *
 * <p>Each node searches its children with the two sides swapped and the window
 * negated, so one piece of code covers both the maximising and the minimising
 * side. The node evaluates the position itself when the game ends or when the
 * depth limit is reached.</p>
 *
 * @author Nathan PLONTZ
 */
public class NegaMaxNode
{
    /** Depth limit, shared by every node of a search. */
    private static int maxDepth;

    /** The value of this node. */
    private double evaluation;

    /** The value of each playable move from this node. */
    private final double [] decision;

    /**
     * Searches a position.
     *
     * @param board The position to search
     * @param depth The depth of this node
     * @param myTurn The index of the side to move
     * @param opponentTurn The index of the other side
     * @param a The alpha bound
     * @param b The beta bound
     */
    public NegaMaxNode (Board board, double depth, int myTurn, int opponentTurn, double a, double b)
    {
        this.decision = new double [Board.NB_HOLES];
        this.evaluation = -Double.MAX_VALUE;
        double [] decisionTemp = new double [Board.NB_HOLES];

        for (int i = 0; i < Board.NB_HOLES; i++)
            if (board.getPlayerHoles ()[i] != 0)
            {
                decisionTemp[i] = i + 1;
                try
                {
                    Board copy = board.playMoveSimulationBoard (myTurn, decisionTemp);
                    /* Evaluate directly when the game is over or the depth limit
                       is reached, otherwise search one ply deeper. */
                    if ((copy.getScore (myTurn) < 0) || (copy.getScore (opponentTurn) >= 25)
                            || (copy.getNbSeeds () <= 6) || !(depth < NegaMaxNode.maxDepth))
                        this.decision[i] = Heuristic.evaluateMaterial (copy, myTurn, opponentTurn);
                    else
                    {
                        NegaMaxNode child =
                                new NegaMaxNode (copy, depth + 1, opponentTurn, myTurn, -b, -a);
                        this.decision[i] = -child.getEvaluation ();
                    }

                    if (this.decision[i] > this.evaluation)
                        this.evaluation = this.decision[i];

                    if (depth > 0)
                    {
                        a = Double.max (a, this.decision[i]);
                        if (a >= b)
                            break;
                    }
                }
                catch (InvalidBotException e)
                {
                    this.decision[i] = 0;
                }
            }
    }

    /**
     * Sets the depth limit for the searches that follow.
     *
     * @param maxDepth The depth limit
     */
    protected static void initialize (int maxDepth)
    {
        NegaMaxNode.maxDepth = maxDepth;
    }

    /**
     * @return The value of this node
     */
    double getEvaluation ()
    {
        return this.evaluation;
    }

    /**
     * @return The value of each playable move from this node
     */
    double [] getDecision ()
    {
        return this.decision;
    }
}
