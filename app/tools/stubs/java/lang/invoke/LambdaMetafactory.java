package java.lang.invoke;

/**
 * Compile time stub. The android.jar mirror used by build.sh omits this class,
 * which javac needs to compile lambdas. D8 desugars every lambda, so this is
 * never packaged or called at runtime.
 */
public final class LambdaMetafactory {
    private LambdaMetafactory() {}

    public static CallSite metafactory(MethodHandles.Lookup caller, String name, MethodType type,
                                       MethodType samType, MethodHandle impl, MethodType instantiated)
            throws LambdaConversionException {
        throw new UnsupportedOperationException();
    }

    public static CallSite altMetafactory(MethodHandles.Lookup caller, String name, MethodType type, Object... args)
            throws LambdaConversionException {
        throw new UnsupportedOperationException();
    }
}
