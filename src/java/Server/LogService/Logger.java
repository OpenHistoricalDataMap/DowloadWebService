package Server.LogService;

import Server.CustomObjects.LogEntry;
import Server.CustomObjects.LogType;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Logger extends Thread implements LoggerInt {

    public static Logger instance;

    private String baseFile;
    private File currentWritingDir;
    public File currentWritingFile;

    private List<LogEntry> BUFFER_LIST = new ArrayList<>();

    private final SimpleDateFormat dirFormatter = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat fileFormatter = new SimpleDateFormat("HH-mm-ss z");
    private final SimpleDateFormat logFormatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");

    private final String logDefaultDir;
    private final int maxLogFileSize;
    private final boolean logTerminalOutput;

    private boolean stop = false;
    private boolean isWaiting = false;
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

    public void stopThread() {
        stop = true;
            interrupt();
    }

    @Override
    public synchronized void run() {
        isRunning = true;
        do {
            try {
                swapToNextDayDirIfNecessary();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                swapToNextFileIfNecessary();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (BUFFER_LIST.isEmpty()) {
                isWaiting = true;
                try {
                    wait();
                } catch (InterruptedException e) {
                    // kindly ignore
                }
                isWaiting = false;
            } else {
                try {
                    writeEntry(BUFFER_LIST.get(0));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                BUFFER_LIST.remove(0);
            }

            if (stop) {
                isRunning = false;
                return;
            }
        } while (true);
    }

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
