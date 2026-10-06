package awele.bot.minmax;

import awele.core.Board;

/**
 * Max node: the best move available to the bot.
 *
 * @author Nathan PLONTZ
 */
public class MaxNode extends MinMaxNode
{
    /**
     * Root node.
     *
     * @param board The position to take a decision on
     */
    MaxNode (Board board)
    {
        this (board, 0, -Double.MAX_VALUE, Double.MAX_VALUE);
    }

    /**
     * Inner node.
     *
     * @param board The position of this node
     * @param depth The depth of this node
     * @param alpha The alpha bound
     * @param beta The beta bound
     */
    MaxNode (Board board, int depth, double alpha, double beta)
    {
        super (board, depth, alpha, beta);
    }

    @Override
    protected double minmax (double eval1, double eval2)
    {
        return Math.max (eval1, eval2);
    }

    /**
     * Cut when the value already matches or exceeds what the parent can hold.
     */
    @Override
    protected boolean alphabeta (double eval, double alpha, double beta)
    {
        return eval >= beta;
    }

    @Override
    protected MinMaxNode getNextNode (Board board, int depth, double alpha, double beta)
    {
        return new MinNode (board, depth, alpha, beta);
    }

    @Override
    protected double alpha (double evaluation, double alpha)
    {
        return Math.max (evaluation, alpha);
    }

    @Override
    protected double beta (double evaluation, double beta)
    {
        return beta;
    }

    /** The worst case for the maximising side: a very small value. */
    @Override
    protected double worst ()
    {
        return -Double.MAX_VALUE;
    }
}
