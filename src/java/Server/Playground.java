package Server;

import Server.CustomObjects.QueryRequest;
import com.google.gson.Gson;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

public class Playground {

    static String file = "request.txt";

    String testInput = "test";

    public synchronized static void main(String[] args) {

    }

    private static void updateRequestFile() throws FileNotFoundException {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(file))));
        System.currentTimeMillis();
    }

    private static void readRequestFile() {

    }
}
