package awele.output;

/**
 * An output channel for everything the engine reports.
 *
 * <p>A channel may be anonymous, in which case it drops the messages that carry
 * author names. That is how the engine produces one full log and one shareable
 * log in the same run.</p>
 *
 * @author Nathan PLONTZ
 */
public abstract class Output
{
    private boolean anonymous;

    protected Output (boolean anonymous)
    {
        this.anonymous = anonymous;
    }

    /**
     * Writes an empty line.
     */
    public void print ()
    {
        this.print ("");
    }

    /**
     * @param object The object to write
     */
    public void print (Object object)
    {
        this.print (object.toString (), false);
    }

    /**
     * @param object The object to write
     * @param anonymous Whether this message identifies an author
     */
    public void print (Object object, boolean anonymous)
    {
        this.print (object.toString (), anonymous);
    }

    protected void print (String string, boolean anonymous)
    {
        if (!(this.anonymous && anonymous))
            this.print (string);
    }

    protected abstract void print (String string);

    abstract void initialiaze ();
}
