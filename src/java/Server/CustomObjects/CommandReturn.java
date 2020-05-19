package Server.CustomObjects;

public class CommandReturn {
    public int returnCode;
    public String output;
    public String errOutput;

    private Process originalProcess;

    public CommandReturn(int returnCode, String output, String errOutput, Process originalProcess) {
        this.returnCode = returnCode;
        this.output = output;
        this.errOutput = errOutput;
        this.originalProcess = originalProcess;
    }

    public Process getOriginalProcess() {
        return originalProcess;
    }
}
