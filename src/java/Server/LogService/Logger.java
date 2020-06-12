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
    private String currentWritingDir;
    public File currentWritingFile;

    private List<LogEntry> BUFFER_LIST = new ArrayList<>();

    private SimpleDateFormat dirFormatter = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat fileFormatter = new SimpleDateFormat("HH-mm-ss z");
    private SimpleDateFormat logFormatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");

    private Boolean isWaiting = false;
    public boolean isRunning = false;

    private boolean newDirCreated = false;

    private String logDefaultDir;
    private int maxLogFileSize;
    private boolean logTerminalOutput;

    public Logger(String logDefaultDir, int maxLogFileSize, boolean logTerminalOutput) {

        this.logDefaultDir = logDefaultDir;
        this.maxLogFileSize = maxLogFileSize;
        this.logTerminalOutput = logTerminalOutput;

        this.baseFile = "LOG";

        Date date = new Date(System.currentTimeMillis());
        this.currentWritingDir = logDefaultDir + dirFormatter.format(date);

        if (!new File(currentWritingDir).exists())
            new File(currentWritingDir).mkdir();

        this.currentWritingFile = new File(currentWritingDir + "/" + baseFile.replace(".txt", "") + "["+ fileFormatter.format(date) +"].txt");

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
        if (currentWritingFile.length() / 1000 > maxLogFileSize) {
            this.currentWritingFile = new File(currentWritingDir + "/" + baseFile.replace(".txt", "") + "["+ fileFormatter.format(System.currentTimeMillis()) +"].txt");

            if (!currentWritingFile.exists())
                currentWritingFile.createNewFile();
        }
    }

    private void swapToNextDayDirIfNecessary() throws IOException {
        Date date = new Date(System.currentTimeMillis());

        if (!currentWritingDir.equals(logDefaultDir + dirFormatter.format(date))) {
            File nwd = new File(logDefaultDir + dirFormatter.format(date));

            if (!nwd.exists())
                nwd.mkdir();

            currentWritingDir = "./" + nwd.getName();

            this.currentWritingFile = new File(currentWritingDir + "/" + baseFile.replace(".txt", "") + "["+ fileFormatter.format(System.currentTimeMillis()) +"].txt");

            if (!currentWritingFile.exists())
                currentWritingFile.createNewFile();

            newDirCreated = true;
        }
    }

    public void stopThread() {

    }

    @Override
    public synchronized void run() {
        isRunning = true;
        if (!currentWritingFile.exists()) {
            try {
                currentWritingFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        while (true) {
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
                try { wait(); } catch (InterruptedException e) { /*kindly ignore*/ }
                isWaiting = false;
            } else {
                try {
                    writeEntry(BUFFER_LIST.get(0));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                BUFFER_LIST.remove(0);
            }
        }
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
}
