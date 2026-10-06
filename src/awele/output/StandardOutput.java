package awele.output;

/**
 * Writes to the console. Singleton.
 *
 * @author Nathan PLONTZ
 */
public class StandardOutput extends Output
{
    private static StandardOutput instance = null;

    private StandardOutput ()
    {
        super (false);
    }

    /**
     * @return The singleton instance
     */
    public static StandardOutput getInstance ()
    {
        if (StandardOutput.instance == null)
            StandardOutput.instance = new StandardOutput ();
        return StandardOutput.instance;
    }

    @Override
    public void print (String string)
    {
        System.out.println (string);
    }

    @Override
    public void initialiaze ()
    {
    }
}
