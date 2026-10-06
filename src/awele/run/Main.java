package awele.run;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.reflections.Reflections;

import awele.bot.Bot;
import awele.bot.ChampionBot;
import awele.bot.CompetitorBot;
import awele.bot.YANNICKBOLASIE.YANNICKBOLASIE;
import awele.bot.demo.random.RandomBot;
import awele.core.Awele;
import awele.core.InvalidBotException;
import awele.output.LogFileOutput;
import awele.output.OutputWriter;
import awele.output.StandardOutput;
import javassist.Modifier;

/**
 * Entry point: discovers every bot on the classpath, qualifies them, then runs
 * the round-robin championship.
 *
 * <p>Qualifying a bot means instantiating it, timing its learning phase and its
 * decisions against the random bot, and measuring its memory footprint. A bot
 * that exceeds any of the limits below is disqualified and left out of the
 * championship.</p>
 *
 * @author Nathan PLONTZ
 */
public final class Main extends OutputWriter
{
    /** Which family of bots takes part in the championship. */
    public enum Tournament
    {
        /** Every bot found on the classpath. */
        ALL,
        /** Only the competition bots. */
        COMPETITORS,
        /** Only the champion bots. */
        CHAMPIONS
    }

    private static Main instance = null;

    private static final String LOG_FILE = "awele.log";
    private static final String ANONYMOUS_LOG_FILE = "awele.anonymous.log";

    /** Only CompetitorBot subclasses are evaluated. */
    //private static final Tournament TOURNAMENT = Tournament.ALL;
    //private static final Tournament TOURNAMENT = Tournament.CHAMPIONS;
    private static final Tournament TOURNAMENT = Tournament.COMPETITORS;

    /** Games per pairing. The assignment evaluates over 100; 10 keeps a local run short. */
    //private static final int NB_RUNS = 100;
    private static final int NB_RUNS = 10;

    /** Learning time limit, one hour. */
    private static final int MAX_LEARNING_TIME = 1000 * 60 * 60;

    /** Decision time limit, per move. */
    private static final int MAX_DECISION_TIME = 250;

    /** Memory limit retained after a garbage collection, 64 MiB. */
    private static final int MAX_MEMORY = 1024 * 1024 * 64;

    /** Peak memory limit during qualification, 1 GiB. */
    private static final int MAX_TOTAL_MEMORY = 1024 * 1024 * 1024;

    ArrayList <Bot> bots;

    /**
     * @return The singleton instance
     */
    public static Main getInstance ()
    {
        if (Main.instance == null)
            Main.instance = new Main ();
        return Main.instance;
    }

    private Main ()
    {
    }

    private static String formatDuration (final long l)
    {
        final long hr = TimeUnit.MILLISECONDS.toHours (l);
        final long min = TimeUnit.MILLISECONDS.toMinutes (l - TimeUnit.HOURS.toMillis (hr));
        final long sec = TimeUnit.MILLISECONDS.toSeconds (
                l - TimeUnit.HOURS.toMillis (hr) - TimeUnit.MINUTES.toMillis (min));
        final long ms = TimeUnit.MILLISECONDS.toMillis (l - TimeUnit.HOURS.toMillis (hr)
                - TimeUnit.MINUTES.toMillis (min) - TimeUnit.SECONDS.toMillis (sec));
        return String.format ("%02d:%02d:%02d.%03d", hr, min, sec, ms);
    }

    private static String formatMemory (long bytes)
    {
        int unit = 1024;
        if (bytes < unit)
            return bytes + " B";
        int exp = (int) (Math.log (bytes) / Math.log (unit));
        String pre = ("KMGTPE").charAt (exp - 1) + "i";
        return String.format ("%.1f %sB", bytes / Math.pow (unit, exp), pre);
    }

    private void printDisqual (String botName, String cause)
    {
        this.print ("Bot \"" + botName + "\" disqualified: " + cause);
    }

    private static long getUsedMemory ()
    {
        return Runtime.getRuntime ().totalMemory () - Runtime.getRuntime ().freeMemory ();
    }

    /** Collects every concrete subclass of the requested bot family. */
    private ArrayList <Class <? extends Bot>> findBotClasses ()
    {
        Reflections reflections = new Reflections ("awele.bot");
        Set <? extends Class <? extends Bot>> found;
        if (Main.TOURNAMENT == Tournament.CHAMPIONS)
            found = reflections.getSubTypesOf (ChampionBot.class);
        else if (Main.TOURNAMENT == Tournament.COMPETITORS)
            found = reflections.getSubTypesOf (CompetitorBot.class);
        else
            found = reflections.getSubTypesOf (Bot.class);

        ArrayList <Class <? extends Bot>> subClasses = new ArrayList <> ();
        for (Class <? extends Bot> subClass : found)
            if (!Modifier.isAbstract (subClass.getModifiers ()))
                subClasses.add (subClass);
        return subClasses;
    }

    /**
     * Measures how long the engine itself takes per move, so that this overhead
     * can be subtracted from every bot's measured decision time.
     */
    private long calibrate (RandomBot random)
    {
        long runningTime = 0;
        int nbMoves = 0;
        for (int k = 0; k < Main.NB_RUNS; k++)
        {
            Awele awele = new Awele (random, random);
            try
            {
                awele.play ();
            }
            catch (InvalidBotException e)
            {
                e.printStackTrace ();
            }
            nbMoves += awele.getNbMoves ();
            runningTime += awele.getRunningTime ();
        }
        return runningTime / nbMoves;
    }

    private void loadBots ()
    {
        long startLoading = System.currentTimeMillis ();
        this.print ("Total memory: " + Main.formatMemory (Runtime.getRuntime ().maxMemory ()));

        RandomBot random = null;
        try
        {
            random = new RandomBot ();
            random.learn ();
        }
        catch (Exception e)
        {
            this.print ("Cannot instantiate the random bot");
            System.exit (0);
        }
        long randomAverageDecisionTime = this.calibrate (random);

        ArrayList <Class <? extends Bot>> subClasses = this.findBotClasses ();
        this.print (subClasses.size () + " classes found");
        for (Class <? extends Bot> botClass : subClasses)
            this.print (botClass);
        this.print ();

        this.bots = new ArrayList <> ();
        int index = 0;
        for (Class <? extends Bot> subClass : subClasses)
        {
            System.gc ();
            long memoryBefore = Main.getUsedMemory ();
            index++;
            this.print ("Bot " + index + "/" + subClasses.size ());
            this.print ("Class: " + subClass.getName ());
            try
            {
                Bot bot = (Bot) subClass.getConstructors ()[0].newInstance ();
                if (bot != null)
                {
                    this.print ("Bot name: " + bot.getName ());
                    this.print ("Author(s): " + bot.getAuthors (), true);

                    long start = System.currentTimeMillis ();
                    bot.learn ();
                    long runningTime = System.currentTimeMillis () - start;
                    this.print ("Learning time: " + Main.formatDuration (runningTime));
                    if (runningTime > Main.MAX_LEARNING_TIME)
                        this.printDisqual (bot.getName (), "learning time too long");
                    else
                    {
                        Awele awele = new Awele (bot, random);
                        awele.play ();
                        long decisionTime = (long) ((2 * awele.getRunningTime ())
                                / awele.getNbMoves ()) - randomAverageDecisionTime;
                        this.print ("Decision time: " + Main.formatDuration (decisionTime));
                        if (decisionTime > Main.MAX_DECISION_TIME)
                            this.printDisqual (bot.getName (), "decision time too long");
                        else
                        {
                            long memoryAfter = Main.getUsedMemory ();
                            System.gc ();
                            long totalUsedMemory = Math.max (0, memoryAfter - memoryBefore);
                            memoryAfter = Main.getUsedMemory ();
                            long usedMemory = Math.max (0, memoryAfter - memoryBefore);
                            this.print ("Memory usage: " + Main.formatMemory (usedMemory));
                            this.print ("Peak memory usage: "
                                    + Main.formatMemory (totalUsedMemory));
                            if ((usedMemory > Main.MAX_MEMORY)
                                    || (totalUsedMemory > Main.MAX_TOTAL_MEMORY))
                                this.printDisqual (bot.getName (), "memory usage too high");
                            else
                                this.bots.add (bot);
                        }
                    }
                }
                this.print ();
            }
            catch (Exception e)
            {
                this.printDisqual (subClass.getName (), "cannot be instantiated");
                this.print (e);
                e.printStackTrace ();
                this.print ();
            }
        }
        this.print (this.bots.size () + " bots instantiated");
        System.gc ();
        this.print ("Loading time: "
                + Main.formatDuration (System.currentTimeMillis () - startLoading));
        this.print ("Memory used: " + Main.formatMemory (Main.getUsedMemory ()));
    }

    /** Plays one pairing and returns the points each side earned, per game. */
    private double [] playPairing (Bot first, Bot second)
    {
        double [] localPoints = new double [2];
        double nbMoves = 0;
        long runningTime = 0;
        for (int k = 0; k < Main.NB_RUNS; k++)
        {
            Awele awele = new Awele (first, second);
            //this.print ();
            //awele.addOutputs (this.getOutputs ());
            //awele.addDebug (StandardOutput.getInstance ());
            try
            {
                awele.play ();
            }
            catch (InvalidBotException e)
            {
                e.printStackTrace ();
            }
            nbMoves += awele.getNbMoves ();
            runningTime += awele.getRunningTime ();
            if (awele.getWinner () >= 0)
                localPoints[awele.getWinner ()] += 3;
            else
            {
                localPoints[0]++;
                localPoints[1]++;
            }
        }
        localPoints[0] /= Main.NB_RUNS;
        localPoints[1] /= Main.NB_RUNS;
        nbMoves /= Main.NB_RUNS;
        runningTime /= Main.NB_RUNS;

        this.print ("Score: " + localPoints[0] + " - " + localPoints[1]);
        if (localPoints[0] == localPoints[1])
            this.print ("Draw");
        else if (localPoints[0] > localPoints[1])
            this.print (first.getName () + " won");
        else
            this.print (second.getName () + " won");
        this.print ("Moves played: " + nbMoves + " per game");
        this.print ("Duration: " + Main.formatDuration (runningTime) + " per game");
        System.gc ();
        this.print ("Memory used: " + Main.formatMemory (Main.getUsedMemory ()));
        return localPoints;
    }

    private void tournament ()
    {
        this.print ();
        this.print ("Let the championship begin");
        int nbBots = this.bots.size ();
        final double [] points = new double [nbBots];
        int nbGames = nbBots * (nbBots - 1) / 2;
        int game = 0;
        long start = System.currentTimeMillis ();

        for (int i = 0; i < nbBots; i++)
            for (int j = i + 1; j < nbBots; j++)
            {
                game++;
                this.print ();
                this.print ("Pairing " + game + "/" + nbGames);
                this.print (this.bots.get (i).getName () + " vs. " + this.bots.get (j).getName ());
                double [] localPoints = this.playPairing (this.bots.get (i), this.bots.get (j));
                points[i] += localPoints[0];
                points[j] += localPoints[1];
            }

        this.print ();
        this.print ("Championship duration: "
                + Main.formatDuration (System.currentTimeMillis () - start));
        for (int i = 0; i < points.length; i++)
            points[i] = Math.round (points[i] * 100) / 100.;

        this.print ();
        this.print ("Final scores:");
        for (int i = 0; i < nbBots; i++)
            this.print (this.bots.get (i) + ": " + points[i]);

        this.print ();
        final Map <String, Integer> map = new HashMap <> ();
        for (int i = 0; i < this.bots.size (); i++)
            map.put (this.bots.get (i).getName (), i);
        Collections.sort (this.bots, new Comparator <Bot> ()
        {
            @Override
            public int compare (Bot bot1, Bot bot2)
            {
                return Double.compare (points[map.get (bot1.getName ())],
                        points[map.get (bot2.getName ())]);
            }
        });
        Arrays.sort (points);

        this.print ("Ranking:");
        for (int i = nbBots - 1; i >= 0; i--)
            this.print ((nbBots - i) + ". " + this.bots.get (i) + ": " + points[i]);
    }

    /**
     * @param args Unused
     */
    public static void main (String [] args)
    {
        long startTime = System.currentTimeMillis ();
        long initialFreeMemory = Runtime.getRuntime ().freeMemory ();
        long initialTotalMemory = Runtime.getRuntime ().totalMemory ();

        System.out.println ("Free memory at start: " + initialFreeMemory + " bytes");
        System.out.println ("Total memory at start: " + initialTotalMemory + " bytes");

        Main main = Main.getInstance ();
        main.addOutput (StandardOutput.getInstance ());
        main.addOutput (new LogFileOutput (Main.LOG_FILE));
        main.addOutput (new LogFileOutput (Main.ANONYMOUS_LOG_FILE, true));
        main.loadBots ();
        main.tournament ();

        long finalFreeMemory = Runtime.getRuntime ().freeMemory ();
        long finalTotalMemory = Runtime.getRuntime ().totalMemory ();

        System.out.println ("Free memory at end: " + finalFreeMemory + " bytes");
        System.out.println ("Total memory at end: " + finalTotalMemory + " bytes");
        System.out.println ("Memory used: " + (initialTotalMemory - finalFreeMemory) + " bytes");
        System.out.println ("Elapsed: " + (System.currentTimeMillis () - startTime) + " ms");

        System.out.println ("Move-stats size: " + YANNICKBOLASIE.memoireCoup.size ());
        System.out.println ("Book size: " + YANNICKBOLASIE.memoirePosition.size ());
    }
}
