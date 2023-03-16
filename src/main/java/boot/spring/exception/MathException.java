package boot.spring.exception;

public class MathException extends Exception {
	
	private static final long serialVersionUID = 1L;

    protected final String message;

    public MathException(String message)
    {
        this.message = message;
    }

    @Override
    public String getMessage()
    {
        return message;
    }
}
