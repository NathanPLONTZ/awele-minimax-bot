package awele.bot;

import java.util.ArrayList;

import awele.core.Board;
import awele.core.InvalidBotException;
import awele.run.Main;

/**
 * Base class of every Awele player.
 *
 * <p>A bot is driven through four hooks: {@link #learn()} runs once when the bot
 * is loaded, {@link #initialize()} before each game, {@link #getDecision(Board)}
 * on every turn, and {@link #finish()} after each game.</p>
 *
 * <p>Competition entries extend {@link CompetitorBot} rather than this class.
 * Extending {@code Bot} directly, or {@link DemoBot}, gives a player that can
 * play but never enters the championship, which is what the sparring and
 * ablation bots do.</p>
 *
 * @author Nathan PLONTZ
 */
public abstract class Bot
{
    private String name;
    private ArrayList <String> authors;

    protected Bot ()
    {
        this.name = "";
        this.authors = new ArrayList <String> ();
    }

    /**
     * Names the bot. Must be called from the constructor of a subclass.
     *
     * @param name The name of the bot
     */
    protected void setBotName (String name)
    {
        this.name = name;
        Main.getInstance ().print ("Instantiating bot \"" + name + "\"");
    }

    /**
     * Declares every author at once.
     *
     * @param names The authors
     * @throws InvalidBotException If more than two authors are given
     */
    protected void setAuthors (String... names) throws InvalidBotException
    {
        if (names.length > 2)
            throw new InvalidBotException ("too many authors for this bot");
        this.authors = new ArrayList <String> ();
        for (String name : names)
            this.authors.add (name);
    }

    /**
     * Adds one author.
     *
     * @param name The author
     * @throws InvalidBotException If the bot already has two authors
     */
    protected void addAuthor (String name) throws InvalidBotException
    {
        if (this.authors.size () < 2)
            this.authors.add (name);
        else
            throw new InvalidBotException ("too many authors for this bot");
    }

    /**
     * @return The name of the bot
     */
    public String getName ()
    {
        return this.name;
    }

    /**
     * @return The authors of the bot
     */
    public String getAuthors ()
    {
        String string = this.authors.get (0);
        if (this.authors.size () == 2)
            string += " and " + this.authors.get (1);
        return string;
    }

    @Override
    public String toString ()
    {
        return this.getName ();
    }

    /**
     * Called before each game.
     */
    public abstract void initialize ();

    /**
     * Called after each game.
     */
    public abstract void finish ();

    /**
     * Chooses a move.
     *
     * @param board The state of the board
     * @return Six values rating how good each of the six moves is believed to
     *         be; the engine plays the highest-rated valid move
     */
    public abstract double [] getDecision (Board board);

    /**
     * Called once, when the bot is loaded.
     */
    public abstract void learn ();
}
