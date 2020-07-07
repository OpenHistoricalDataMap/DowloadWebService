package Server.IDService;

import Server.CustomObjects.LogType;
import Server.LogService.Logger;

import java.io.*;
import java.util.Random;

public class IDSystem {

    // Tag for the Logger
    private static final String TAG = "IDSystem";

    // Characters used in the IDs
    private static final String idCharacterAlphabet = "abcdefghijklmnopqrstuvwxyz";
    // Numbers used in the IDs
    private static final String idNumbersAlphabet = "1234567890";
    // special Characters used in the IDs
    private static final String idSpecialCharactersAlphabet = "_-";

    // allowed size of an ID
    private static final int idSize = 8;

    // File where the IDs will be saved
    private static File idSaveFile;

    /**
     * setting the ID Save File to read from or write to
     * ( used to add new ID's and check if an ID already exist)
     * @param idSaveFile save file
     * @throws IOException can be thrown when creating a new file/directory
     */
    public static void setIDSaveFile(File idSaveFile) throws IOException {
        IDSystem.idSaveFile = idSaveFile;
        if (!idSaveFile.exists()) {
            new File(idSaveFile.getParent()).mkdirs();
            idSaveFile.createNewFile();
            Logger.instance.addLogEntry(LogType.INFO, TAG, "created new id-savefile at " + idSaveFile.getPath());
        }
    }

    /**
     * creates a new ID , saves it in the idSaveFile and returns it
     * @return newly created ID
     * @throws IOException exception can be thrown when writing to the file
     */
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

    /**
     * appending a new line to the save file
     * @param id id to append
     * @return returns true, if the writing process was successful
     * @throws IOException exception can be thrown when writing to the file
     */
    public static boolean writeEntry(String id) throws IOException {
        if (idAlreadyExists(id)) {
            return false;
        }

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

        return true;
    }

    /**
     * checks the file if the ID already exists
     * @param id id to check if it already exist
     * @return true if the id already exist, false if not
     * @throws IOException can be thrown when reading the File
     */
    private static boolean idAlreadyExists(String id) throws IOException {
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

    /**
     * Generates a new ID with the given Randomizer, allowed Characters and length
     * @param rng randomizer
     * @param characters allowed characters
     * @param length id length
     * @return
     */
    private static String generateID(Random rng, String characters, int length) throws IOException {
        char[] text = new char[length];
        do {
            for (int i = 0; i < length; i++) {
                text[i] = characters.charAt(rng.nextInt(characters.length()));
            }
            // repeat till the created id isn't already used
        } while (idAlreadyExists(new String(text)));

        return new String(text);
    }

    /**
     * returns all saved ids from the idSaveFile
     * @return all saved ids
     * @throws IOException can be thrown when the file is read
     */
    public static String getAllIDs() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(idSaveFile)));
        String read = "";
        while (br.ready()) {
            read += br.readLine() + "\n";
        }
        br.close();
        return read;
    }
}
