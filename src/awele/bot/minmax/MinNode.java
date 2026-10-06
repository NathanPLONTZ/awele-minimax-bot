package awele.bot.minmax;

import awele.core.Board;

/**
 * Min node: the best move available to the opponent.
 *
 * @author Nathan PLONTZ
 */
public class MinNode extends MinMaxNode
{
    /**
     * Root node.
     *
     * @param board The position to take a decision on
     */
    MinNode (Board board)
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
    MinNode (Board board, int depth, double alpha, double beta)
    {
        super (board, depth, alpha, beta);
    }

    @Override
    protected double minmax (double eval1, double eval2)
    {
        return Math.min (eval1, eval2);
    }

    /**
     * Cut when the value already matches or falls below what the parent can hold.
     */
    @Override
    protected boolean alphabeta (double eval, double alpha, double beta)
    {
        return eval <= alpha;
    }

    @Override
    protected MinMaxNode getNextNode (Board board, int depth, double alpha, double beta)
    {
        return new MaxNode (board, depth, alpha, beta);
    }

    @Override
    protected double alpha (double evaluation, double alpha)
    {
        return alpha;
    }

    @Override
    protected double beta (double evaluation, double beta)
    {
        return Math.min (evaluation, beta);
    }

    /** The worst case for the minimising side: a very large value. */
    @Override
    protected double worst ()
    {
        return Double.MAX_VALUE;
    }
}
