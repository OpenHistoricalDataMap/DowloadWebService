package Server.CustomObjects;

public class LogEntry {

    public LogType type;
    public String TAG;
    public String message;

    public LogEntry(LogType type, String TAG, String message) {
        this.type = type;
        this.TAG = TAG;
        this.message = message;
    }
}
