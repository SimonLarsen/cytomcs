package dk.sdu.compbio.cytomcs.internal;

public class Parameters {
    private final boolean directed;
    private final int max_nonimproving;
    private final float perturbation;
    private final int exceptions;
    private final boolean remove_leaf_exceptions;

    public Parameters(
            boolean directed,
            int max_nonimproving,
            float perturbation,
            int exceptions,
            boolean remove_leaf_exceptions
    ) {
        this.directed = directed;
        this.max_nonimproving = max_nonimproving;
        this.perturbation = perturbation;
        this.exceptions = exceptions;
        this.remove_leaf_exceptions = remove_leaf_exceptions;
    }

    public boolean getDirected() { return directed; }

    public int getMaxNonimprovingIterations() {
        return max_nonimproving;
    }

    public float getPerturbation() { return perturbation; }

    public int getExceptions() { return exceptions; }

    public boolean getRemoveLeafExceptions() { return remove_leaf_exceptions; }
}
