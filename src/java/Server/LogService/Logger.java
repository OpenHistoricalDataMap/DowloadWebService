package Server.LogService;

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
    public String currentWritingFile;

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

        this.currentWritingFile = currentWritingDir + "/" + baseFile.replace(".txt", "") + "["+ fileFormatter.format(date) +"].txt";

        if (instance == null)
            instance = this;
    }

    public void addLogEntry(LogType type, String TAG, String message) {
        BUFFER_LIST.add(new LogEntry(type, TAG, message));
        if (isWaiting)
            this.interrupt();
    }

    private void swapToNextFileIfNecessary() throws IOException {
        if (new File(currentWritingFile).length() / 1000 > maxLogFileSize) {
            currentWritingFile = baseFile.replace(".txt", "")+ "[" + fileFormatter.format(System.currentTimeMillis()) + "].txt";

            File file = new File(currentWritingFile);

            if (!file.exists())
                file.createNewFile();
        }
    }

    private void swapToNextDayDirIfNecessary() throws IOException {
        Date date = new Date(System.currentTimeMillis());

        if (!currentWritingDir.equals(logDefaultDir + dirFormatter.format(date))) {
            File nwd = new File(logDefaultDir + dirFormatter.format(date));

            if (!nwd.exists())
                nwd.mkdir();

            currentWritingDir = "./" + nwd.getName();

            currentWritingFile = currentWritingDir + "/" + baseFile.replace(".txt", "") + "["+ fileFormatter.format(System.currentTimeMillis()) +"].txt";;

            File file = new File(currentWritingFile);

            if (!file.exists())
                file.createNewFile();

            newDirCreated = true;
        }
    }

    @Override
    public synchronized void run() {
        isRunning = true;
        if (!new File(currentWritingFile).exists()) {
            try {
                new File(currentWritingFile).createNewFile();
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
        File file = new File(currentWritingFile);

        if (!file.exists())
            file.createNewFile();

        Date date = new Date(System.currentTimeMillis());
        String input = entry.type + " | " + logFormatter.format(date) + " | " + entry.TAG + " : " + entry.message;

        if (logTerminalOutput)
            System.out.println(input);

        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

        while(br.ready()) {
            temp += br.readLine() + "\n";
        }

        temp += input + "\n";
        br.close();

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
        bw.write(temp);
        bw.close();
    }
    private class LogEntry {

        LogType type;
        String TAG;
        String message;

        public LogEntry(LogType type, String TAG, String message) {
            this.type = type;
            this.TAG = TAG;
            this.message = message;
        }
    }
}
