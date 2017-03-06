package dk.sdu.compbio.cytomcs.internal;

public class Parameters {
    private final int iterations;
    private final float perturbation;
    private final int exceptions;
    private final boolean connected;

    public Parameters(int iterations, float perturbation, int exceptions, boolean connected) {
        this.iterations = iterations;
        this.perturbation = perturbation;
        this.exceptions = exceptions;
        this.connected = connected;
    }

    public int getIterations() {
        return iterations;
    }

    public float getPerturbation() { return perturbation; }

    public int getExceptions() { return exceptions; }

    public boolean getConnected() {
        return connected;
    }
}
