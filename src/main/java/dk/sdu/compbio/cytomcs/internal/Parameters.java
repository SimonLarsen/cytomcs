package dk.sdu.compbio.cytomcs.internal;

public class Parameters {
    private final int max_nonimproving;
    private final float perturbation;
    private final int exceptions;
    private final boolean connected;
    private final boolean remove_leaf_exceptions;

    public Parameters(
            int max_nonimproving,
            float perturbation,
            int exceptions,
            boolean connected,
            boolean remove_leaf_exceptions
    ) {
        this.max_nonimproving = max_nonimproving;
        this.perturbation = perturbation;
        this.exceptions = exceptions;
        this.connected = connected;
        this.remove_leaf_exceptions = remove_leaf_exceptions;
    }

    public int getMaxNonimprovingIterations() {
        return max_nonimproving;
    }

    public float getPerturbation() { return perturbation; }

    public int getExceptions() { return exceptions; }

    public boolean getConnected() {
        return connected;
    }

    public boolean getRemoveLeafExceptions() { return remove_leaf_exceptions; }
}
