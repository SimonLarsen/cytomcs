package dk.sdu.compbio.cytomcs.internal;

public class Parameters {
    private final int iterations;
    private final float perturbation;
    private final boolean connected;

    public Parameters(int iterations, float perturbation, boolean connected) {
        this.iterations = iterations;
        this.perturbation = perturbation;
        this.connected = connected;
    }

    public int getIterations() {
        return iterations;
    }

    public float getPerturbation() {
        return perturbation;
    }

    public boolean getConnected() {
        return connected;
    }
}
