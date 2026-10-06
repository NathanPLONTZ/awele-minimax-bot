package awele.output;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Appends to a text file, which is truncated when the channel is registered.
 *
 * @author Nathan PLONTZ
 */
public class LogFileOutput extends Output
{
    private String path;

    /**
     * @param path Path to the log file
     */
    public LogFileOutput (String path)
    {
        super (false);
        this.path = path;
    }

    /**
     * @param path Path to the log file
     * @param anonymous Whether author names must be dropped
     */
    public LogFileOutput (String path, boolean anonymous)
    {
        super (anonymous);
        this.path = path;
    }

    @Override
    public void print (String string)
    {
        try
        {
            FileWriter fw = new FileWriter (this.path, true);
            BufferedWriter bw = new BufferedWriter (fw);
            PrintWriter out = new PrintWriter (bw);
            out.println (string);
            out.close ();
        }
        catch (IOException e)
        {
            e.printStackTrace ();
        }
    }

    @Override
    public void initialiaze ()
    {
        File file = new File (this.path);
        file.delete ();
    }
}
