# The Logger <LoggerInt>

A small Logger System with **re-usability** in mind. The Logger itself is a Thread.

This Module is build to be a Singleton.
Typical Logger File Path : log/2016-12-31/<prefix>[23-11-59 MESZ].txt

## Variables

| Name               | Type             | Standard Value                               | Description                                                  | Authors Note                                                 |
| ------------------ | ---------------- | -------------------------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| instance           | Logger (public)  | null (defined in Constructor)                | singleton instance                                           | This Module is made, with the intention in mind to be used as a Singleton. |
| baseFile           | String           | null (defined in Constructor)                | prefix for every Logger File                                 |                                                              |
| currentWritingDir  | File             | null (defined in Constructor)                | Path to currentWritingFile                                   |                                                              |
| currentWritingFile | File             | null (defined in Constructor)                | current writing File                                         |                                                              |
| BUFFER_LIST        | List<LogEntry>   | new                                          | Buffer List for Logger Entries                               | The list contains all the Entries that need to be made, but are not yet inserted into the currentWritingFile |
| dirFormatter       | SimpleDateFormat | new (pattern : "yyyy-MM-dd")                 | Date Formatter for the Logger Directory                      |                                                              |
| fileFormatter      | SimpleDateFormat | new (pattern : "HH-mm-ss z")                 | Date Formatter for the Logger File Name                      |                                                              |
| logFormatter       | SimpleDateFormat | new (pattern : "yyyy-MM-dd 'at' HH:mm:ss z") | Date Formatter for the Log Entries                           |                                                              |
| logDefaultDir      | String           | null (defined in Constructor)                | log Default Directory                                        |                                                              |
| maxLogFileSize     | int              | null (defined in Constructor)                | max Size the the log file is allowed to have                 |                                                              |
| logTerminalOutput  | boolean          | null (defined in Constructor)                | boolean, that decides if the Logger should output to the System.out as well |                                                              |
| stop               | boolean          | false                                        | if stop == true -> the Thread is about to stop or is already stopped | If the boolean is stop but the Thread is still running longer than a couple of seconds, then the Logger is stuck and needs to be restarted manually by shutting down the whole system and restarting it again |
| isWaiting          | boolean          | false                                        | true if the Logger is currently waiting on new inputs        |                                                              |
| isRunning          | boolean          | false                                        | true if the Thread is currently running                      |                                                              |

---

## Constructor

| Parameter List    | Type    | used for...                                          | Authors Note |
| :---------------- | ------- | ---------------------------------------------------- | ------------ |
| logDefaultDir     | String  | logDefaultDir, currentWritingDir, currentWritingFile |              |
| maxLogFileSize    | int     | maxLogFileSize                                       |              |
| logTerminalOutput | boolean | logTerminalOutput                                    |              |

Constructor Code :

```java
public Logger(String logDefaultDir, int maxLogFileSize, boolean logTerminalOutput) {
    this.logDefaultDir = logDefaultDir;
    this.maxLogFileSize = maxLogFileSize;
    this.logTerminalOutput = logTerminalOutput;

    this.baseFile = "LOG";

    long date = System.currentTimeMillis();
    this.currentWritingDir = new File(logDefaultDir + dirFormatter.format(date));
    this.currentWritingFile = new File(currentWritingDir + "/" + baseFile.replace(".txt", "") + "["+ fileFormatter.format(date) +"].txt");


    if (!this.currentWritingDir.exists())
        this.currentWritingDir.mkdir();

    if (!this.currentWritingFile.exists()) {
        try {
            this.currentWritingFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    if (instance == null)
        instance = this;
}
```

---

## The "run" Method

```java
	// every time a new Logger Entry is made, this Thread always checks
    // if the File or the Directory needs to be changed
    @Override
    public synchronized void run() {
        isRunning = true;
        do {
            try {
                // Every Day a new Directory is created
                // if a new day has started since the last time a Log Entry was made
                // the Method will update the currentWritingFile and the CurrentWritingDir
                swapToNextDayDirIfNecessary();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                // The max size of a logger File is defined in the constructor
                // if this limit is met by the currentWritingFile, then a new will be created in the 					currentWritingDir
                swapToNextFileIfNecessary();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // here, the Logger will wait for a new Logger entry, if there is no new entry to be made
            // this waiting can be interrupted, by adding a new Log Entry, or by calling the Method 				stopThread()
            if (BUFFER_LIST.isEmpty()) {
                isWaiting = true;
                try {
                    wait();
                } catch (InterruptedException e) {
                    // kindly ignore
                }
                isWaiting = false;
                // if a new Log Entry has to be written, then it will do just that
            } else {
                try {
                    writeEntry(BUFFER_LIST.get(0));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                BUFFER_LIST.remove(0);
            }

            // this can only be entered, if the method stopThread was called
            if (stop) {
                isRunning = false;
                return;
            }
            // repeat this forever, till an exception is thrown, or the Method jumps out
        } while (true);
    }
```

---

## Public Methods

### Interface Methods :

| Name        | Parameter list                              | return Value | Description                            | Authors Note                                                 |
| ----------- | ------------------------------------------- | ------------ | -------------------------------------- | ------------------------------------------------------------ |
| addLogEntry | type(LogType), TAG(String), message(String) | void         | Method to add a new Log Entry          |                                                              |
| stopThread  | empty                                       | void         | will stop the currently running Thread | f the boolean is stop but the Thread is still running longer than a couple of seconds, then the Logger is stuck and needs to be restarted manually by shutting down the whole system and restarting it again |

### Getter and Setter

| Name                  | Parameter List              | return Value   | Description                             | Authors Note                                    |
| --------------------- | --------------------------- | -------------- | --------------------------------------- | ----------------------------------------------- |
| getBUFFER_LIST        | empty                       | List<LogEntry> | gets the current BUFFER_LIST            |                                                 |
| setBUFFER_LIST        | BUFFER_LIST(List<LogEntry>) | void           | sets the BUFFER_LIST                    |                                                 |
| getCurrentWritingFile | empty                       | File           | gives back current writing File         | used for the Controller and in the Spring Class |
| isRunning             | empty                       | void           | true if the Thread is currently running |                                                 |

---

## Private Methods

| Name                        | Parameter List  | return Value | Description                                                  | Authors Note |
| --------------------------- | --------------- | ------------ | ------------------------------------------------------------ | ------------ |
| swapToNextFileIfNecessary   | empty           | void         | Every Day a new Directory is created if a new day has started since the last time a Log Entry was made the Method will update the currentWritingFile and the CurrentWritingDir |              |
| swapToNextDayDirIfNecessary | empty           | void         | The max size of a logger File is defined in the constructor if this limit is met by the currentWritingFile, then a new will be created in the currentWritingDir |              |
| writeEntry                  | entry(LogEntry) | void         | Method to write a new Entry into the currentWritingFile      |              |

