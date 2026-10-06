package awele.output;

import java.util.ArrayList;

/**
 * Broadcasts messages to every registered output channel.
 *
 * <p>Two sets of channels are kept: the regular ones, which report the course of
 * the championship, and the debug ones, which also receive the board after each
 * move.</p>
 *
 * @author Nathan PLONTZ
 */
public class OutputWriter
{
    private ArrayList <Output> outputs;
    private ArrayList <Output> debug;

    /**
     * Builds a writer with no channel registered.
     */
    public OutputWriter ()
    {
        this.outputs = new ArrayList <Output> ();
        this.debug = new ArrayList <Output> ();
    }

    /**
     * @param output The channel to register
     */
    public void addOutput (Output output)
    {
        output.initialiaze ();
        this.outputs.add (output);
    }

    /**
     * @param output The debug channel to register
     */
    public void addDebug (Output output)
    {
        output.initialiaze ();
        this.debug.add (output);
    }

    /**
     * Writes an empty line to every channel.
     */
    public void print ()
    {
        for (Output output : this.outputs)
            output.print ();
        for (Output output : this.debug)
            output.print ();
    }

    protected void printDebug ()
    {
        for (Output output : this.debug)
            output.print ();
    }

    /**
     * @param object The object to write
     */
    public void print (Object object)
    {
        for (Output output : this.outputs)
            output.print (object, false);
        for (Output output : this.debug)
            output.print (object, false);
    }

    /**
     * @param object The object to write
     * @param anonymous Whether this message identifies an author
     */
    public void print (Object object, boolean anonymous)
    {
        for (Output output : this.outputs)
            output.print (object, anonymous);
        for (Output output : this.debug)
            output.print (object, anonymous);
    }

    protected void printDebug (Object object)
    {
        for (Output output : this.debug)
            output.print (object);
    }
}
