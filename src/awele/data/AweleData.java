package awele.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

/**
 * The provided database of recorded moves.
 *
 * <p>303 situations observed over several games between an experienced player
 * and a novice, which the experienced player won every time. Loaded from
 * {@code data/awele.data}, relative to the working directory.</p>
 *
 * @author Nathan PLONTZ
 */
public class AweleData extends ArrayList <AweleObservation>
{
    private static final long serialVersionUID = 1L;

    private static final String PATH = "data/awele.data";

    /**
     * @return A freshly loaded copy of the dataset
     */
    public static AweleData getInstance ()
    {
        return new AweleData ();
    }

    private AweleData ()
    {
        this (AweleData.PATH);
    }

    private AweleData (String path)
    {
        super ();
        try
        {
            BufferedReader br = new BufferedReader (new FileReader (new File (path)));
            br.readLine ();
            String string;
            while ((string = br.readLine ()) != null)
                this.add (new AweleObservation (string));
            br.close ();
        }
        catch (Exception e)
        {
            e.printStackTrace ();
        }
    }
}
