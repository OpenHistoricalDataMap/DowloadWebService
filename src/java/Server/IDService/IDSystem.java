package Server.IDService;

import Server.CustomObjects.LogEntry;
import Server.CustomObjects.LogType;
import Server.LogService.Logger;

import java.io.*;
import java.util.Date;
import java.util.Random;

public class IDSystem {

    private static final String LOG_TAG = "IDSystem";

    private static final String idCharacterAlphabet = "abcdefghijklmnopqrstuvwxyz";
    private static final String idNumbersAlphabet = "1234567890";
    private static final String idSpecialCharactersAlphabet = "_-";
    
    private static final int idSize = 8;
    
    private static File idSaveFile;

    public static void setIDSaveFile(File idSaveFile) throws IOException {
        IDSystem.idSaveFile = idSaveFile;
        if (!idSaveFile.exists()) {
            new File(idSaveFile.getParent()).mkdirs();
            idSaveFile.createNewFile();
            Logger.instance.addLogEntry(LogType.INFO, LOG_TAG, "created new id-savefile at " + idSaveFile.getPath());
        }
    }

    public static String createNewID() throws IOException {
        boolean newIDCreated = false;
        String newID = null;
        while (!newIDCreated) {
            newID = generateID(new Random(), idCharacterAlphabet + idCharacterAlphabet.toUpperCase() + idNumbersAlphabet, 8);
            newIDCreated = !idAlreadyExists(newID);
        }

        writeEntry(newID);
        return newID;
    }

    private static void writeEntry(String id) throws IOException {
        String temp = "";

        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(idSaveFile)));

        while(br.ready()) {
            temp += br.readLine() + "\n";
        }

        temp += id + "\n";
        br.close();

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(idSaveFile)));
        bw.write(temp);
        bw.flush();
        bw.close();
    }

    public static boolean idAlreadyExists(String id) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(idSaveFile)));
        while (br.ready()) {
            if (id.equals(br.readLine())) {
                br.close();
                return true;
            }
        }
        br.close();
        return false;
    }

    private static String generateID(Random rng, String characters, int length)
    {
        char[] text = new char[length];
        for (int i = 0; i < length; i++)
        {
            text[i] = characters.charAt(rng.nextInt(characters.length()));
        }
        return new String(text);
    }
}
