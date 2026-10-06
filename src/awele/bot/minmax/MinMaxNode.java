package awele.bot.minmax;

import awele.core.Board;
import awele.core.InvalidBotException;

/**
 * A node of the reference minimax tree.
 *
 * <p>One node per position, specialised into {@link MaxNode} and {@link MinNode}
 * through the abstract hooks below. The evaluation is the plain score
 * difference, with no positional term at all.</p>
 *
 * <p><b>Note on the pruning.</b> This node maintains alpha and beta but never
 * breaks out of the loop, so no branch is ever actually cut: the tree is fully
 * explored. That, together with allocating one object per node, is why this
 * reference search cannot be pushed much past depth 6 inside the time budget,
 * while the competition bot reaches depth 9.</p>
 *
 * @author Nathan PLONTZ
 */
public abstract class MinMaxNode
{
    /** The index of the side the search is run for. */
    private static int player;

    /** Depth limit, shared by every node of a search. */
    private static int maxDepth;

    /** The value of this node. */
    private double evaluation;

    /** The value of each playable move from this node. */
    private double [] decision;

    /** Debug counter: nodes expanded. */
    public static int nbNoeud = 0;

    /** Debug counter: positions evaluated because the game ended. */
    public static int p = 0;

    /**
     * Searches a position.
     *
     * @param board The position to search
     * @param depth The depth of this node
     * @param alpha The alpha bound
     * @param beta The beta bound
     */
    public MinMaxNode (Board board, int depth, double alpha, double beta)
    {
        this.decision = new double [Board.NB_HOLES];
        this.evaluation = this.worst ();
        for (int i = 0; i < Board.NB_HOLES; i++)
            if (board.getPlayerHoles ()[i] != 0)
            {
                double [] decision = new double [Board.NB_HOLES];
                decision[i] = 1;
                Board copy = (Board) board.clone ();
                try
                {
                    int score = copy.playMoveSimulationScore (copy.getCurrentPlayer (), decision);
                    copy = copy.playMoveSimulationBoard (copy.getCurrentPlayer (), decision);
                    /* Evaluate directly when the move ends the game. */
                    if ((score < 0)
                            || (copy.getScore (Board.otherPlayer (copy.getCurrentPlayer ())) >= 25)
                            || (copy.getNbSeeds () <= 6))
                    {
                        this.decision[i] = this.diffScore (copy);
                        MinMaxNode.p++;
                    }
                    else if (depth < MinMaxNode.maxDepth)
                    {
                        MinMaxNode.nbNoeud++;
                        MinMaxNode child = this.getNextNode (copy, depth + 1, alpha, beta);
                        this.decision[i] = child.getEvaluation ();
                    }
                    else
                        this.decision[i] = this.diffScore (copy);

                    this.evaluation = this.minmax (this.decision[i], this.evaluation);
                    if (depth > 0)
                    {
                        alpha = this.alpha (this.evaluation, alpha);
                        beta = this.beta (this.evaluation, beta);
                    }
                }
                catch (InvalidBotException e)
                {
                    this.decision[i] = 0;
                }
            }
    }

    /** @return The worst possible value for this kind of node */
    protected abstract double worst ();

    /**
     * Sets the search parameters for the searches that follow.
     *
     * @param board The position the search starts from
     * @param maxDepth The depth limit
     */
    protected static void initialize (Board board, int maxDepth)
    {
        MinMaxNode.maxDepth = maxDepth;
        MinMaxNode.player = board.getCurrentPlayer ();
    }

    private int diffScore (Board board)
    {
        return board.getScore (MinMaxNode.player)
                - board.getScore (Board.otherPlayer (MinMaxNode.player));
    }

    /**
     * Updates alpha, on a max node only.
     *
     * @param evaluation The current value of this node
     * @param alpha The previous alpha
     * @return The new alpha
     */
    protected abstract double alpha (double evaluation, double alpha);

    /**
     * Updates beta, on a min node only.
     *
     * @param evaluation The current value of this node
     * @param beta The previous beta
     * @return The new beta
     */
    protected abstract double beta (double evaluation, double beta);

    /**
     * Takes the min or the max, depending on the kind of node.
     *
     * @param eval1 A value
     * @param eval2 Another value
     * @return The better of the two for this kind of node
     */
    protected abstract double minmax (double eval1, double eval2);

    /**
     * Whether an alpha-beta cut should be made, given the kind of node.
     *
     * <p>Implemented by both subclasses but never called: see the note on
     * pruning in the class comment.</p>
     *
     * @param eval The current value of this node
     * @param alpha The alpha bound
     * @param beta The beta bound
     * @return Whether the branch can be cut
     */
    protected abstract boolean alphabeta (double eval, double alpha, double beta);

    /**
     * Builds the node of the next ply, of the opposite kind.
     *
     * @param board The position of the child
     * @param depth The depth of the child
     * @param alpha The alpha bound
     * @param beta The beta bound
     * @return The child node
     */
    protected abstract MinMaxNode getNextNode (Board board, int depth, double alpha, double beta);

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
