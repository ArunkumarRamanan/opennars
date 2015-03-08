package raytracer.exception;

/**
 * Diese Exception signalisiert eine unzul�ssige lineare Abh�ngigkeit.
 * 
 * @author Mathias Kosch
 *
 */
public class LinearlyDependentException extends RuntimeException
{
    /** Serielle Standardversions-ID. */
    private static final long serialVersionUID = 1L;
    
    
    /**
     * Erzeugt eine neue <code>LinearlyDependentException</code>-Exception.
     */
    public LinearlyDependentException()
    {
        super("Das Format ist ung�ltig!");
    }
    
    /**
     * Erzeugt eine neue <code>LinearlyDependentException</code>-Exception.
     * 
     * @param message Beschreibung der Exception.
     */
    public LinearlyDependentException(String message)
    {
        super(message);
    }
}