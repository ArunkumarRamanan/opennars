package raytracer.exception;

/**
 * Diese Exception signalisiert ein ung�ltiges Format.
 * 
 * @author Mathias Kosch
 *
 */
public class InvalidFormatException extends Exception
{
    /** Serielle Standardversions-ID. */
    private static final long serialVersionUID = 1L;
    
    
    /**
     * Erzeugt eine neue <code>InvalidFormatException</code>-Exception.
     */
    public InvalidFormatException()
    {
        super("Das Format ist ung�ltig!");
    }
    
    /**
     * Erzeugt eine neue <code>InvalidFormatException</code>-Exception.
     * 
     * @param message Beschreibung der Exception.
     */
    public InvalidFormatException(String message)
    {
        super(message);
    }
}