package awele.core;

/**
 * Raised when a bot breaks the rules it is bound by: an impossible decision
 * vector, too many authors, and so on.
 *
 * @author Nathan PLONTZ
 */
public class InvalidBotException extends Exception
{
    private static final long serialVersionUID = 1L;

    /**
     * Builds the exception with a generic message.
     */
    public InvalidBotException ()
    {
        super ("Invalid bot");
    }

    /**
     * @param message The reason the bot is invalid
     */
    public InvalidBotException (String message)
    {
        super ("Invalid bot: " + message);
    }
}
