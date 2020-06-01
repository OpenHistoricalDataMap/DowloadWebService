package Server.LogService;

import Server.CustomObjects.LogType;

public interface LoggerInt {

    void addLogEntry(LogType type, String TAG, String message);
}
