package Server.LogService;

import Server.CustomObjects.LogEntry;
import Server.CustomObjects.LogType;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Logger extends Thread implements LoggerInt {

    // singleton instance
    public static Logger instance;

    // prefix for every Logger File
    private String baseFile;
    // Path to currentWritingFile
    private File currentWritingDir;
    // current writing File
    private File currentWritingFile;

    // Buffer List for Logger Entries
    private List<LogEntry> BUFFER_LIST = new ArrayList<>();

    // Date Formatter for the Logger Directory
    private final SimpleDateFormat dirFormatter = new SimpleDateFormat("yyyy-MM-dd");
    // Date Formatter for the Logger File Name
    private final SimpleDateFormat fileFormatter = new SimpleDateFormat("HH-mm-ss z");
    // Date Formatter for the Log Entries
    private final SimpleDateFormat logFormatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");

    // log Default Directory
    private final String logDefaultDir;
    // max Size the the log file is allowed to have
    private final int maxLogFileSize;
    // boolean, that decides if the Logger should output to the System.out as well
    private final boolean logTerminalOutput;

    // if stop == true -> the Thread is about to stop or is already stopped
    private boolean stop = false;
    // true if the Logger is currently waiting on new inputs
    private boolean isWaiting = false;
    // true if the Thread is currently running
    private boolean isRunning = false;

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

    public List<LogEntry> getBUFFER_LIST() {
        return BUFFER_LIST;
    }
    public void setBUFFER_LIST(List<LogEntry> BUFFER_LIST) {
        this.BUFFER_LIST = BUFFER_LIST;
    }
    public File getCurrentWritingFile() {
        return currentWritingFile;
    }
    public void addLogEntry(LogType type, String TAG, String message) {
        BUFFER_LIST.add(new LogEntry(type, TAG, message));
        if (isWaiting)
            this.interrupt();
    }

    private void swapToNextFileIfNecessary() throws IOException {
        if ((float) this.currentWritingFile.length() / 1000 > maxLogFileSize) {
            this.currentWritingFile = new File(currentWritingDir + "/" + baseFile.replace(".txt", "") + "["+ fileFormatter.format(System.currentTimeMillis()) +"].txt");

            if (!this.currentWritingFile.exists())
                this.currentWritingFile.createNewFile();
        }
    }

    private void swapToNextDayDirIfNecessary() throws IOException {
        Date date = new Date(System.currentTimeMillis());

        String checkName = logDefaultDir + dirFormatter.format(date);
        if (!this.currentWritingDir.getPath().equals(checkName)) {
            this.currentWritingDir = new File(logDefaultDir + dirFormatter.format(date));

            if (!this.currentWritingDir.exists())
                if (!this.currentWritingDir.mkdir()) {
                    System.out.println("couldn't create new writing dir");
                }

            this.currentWritingFile = new File(currentWritingDir + "/" + baseFile.replace(".txt", "") + "[" + fileFormatter.format(System.currentTimeMillis()) + "].txt");

            if (!this.currentWritingFile.exists())
                this.currentWritingFile.createNewFile();
        }
    }

    @Override
    public void stopThread() {
        stop = true;
            interrupt();
    }

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
                // if this limit is met by the currentWritingFile,
                // then a new will be created in the currentWritingDir
                swapToNextFileIfNecessary();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // here, the Logger will wait for a new Logger entry, if there is no new entry to be made
            // this waiting can be interrupted, by adding a new Log Entry, or by calling the Method stopThread()
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

    // Method to write a new Entry into the currentWritingFile
    private void writeEntry(LogEntry entry) throws IOException {
        String temp = "";

        if (!currentWritingFile.exists())
            currentWritingFile.createNewFile();

        Date date = new Date(System.currentTimeMillis());
        String input = entry.type + " | " + logFormatter.format(date) + " | " + entry.TAG + " : " + entry.message;

        if (logTerminalOutput)
            System.out.println(input);

        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(currentWritingFile)));

        while(br.ready()) {
            temp += br.readLine() + "\n";
        }

        temp += input + "\n";
        br.close();

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(currentWritingFile)));
        bw.write(temp);
        bw.close();
    }

    public boolean isRunning() {
        return isRunning;
    }
}
