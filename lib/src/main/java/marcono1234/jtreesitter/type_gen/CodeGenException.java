package marcono1234.jtreesitter.type_gen;

/**
 * Exception thrown when code generation fails.
 */
public class CodeGenException extends Exception {
    public CodeGenException(String message) {
        super(message);
    }

    public CodeGenException(String message, Throwable cause) {
        super(message, cause);
    }
}
