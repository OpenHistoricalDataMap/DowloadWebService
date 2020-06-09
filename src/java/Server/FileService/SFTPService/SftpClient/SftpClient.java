package Server.FileService.SFTPService.SftpClient;

import Server.FileService.FileServerClient;
import com.jcraft.jsch.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class SftpClient implements FileServerClient<RemoteFile, RemoteDirectory> {

    private Session session;
    private ChannelSftp channel;

    private String output;
    private String[] usableOutput;

    private final String server_ip;
    private final int sftp_port;
    private final String username;
    private final String password;

    public SftpClient(String server_ip, int sftp_port, String username, String password) {
        this.server_ip = server_ip;
        this.sftp_port = sftp_port;
        this.username = username;
        this.password = password;
    }


    @Override
    public int connect() {
        // just checks if the session isn't already established
        if (!(session == null))
            // returns error code 5
            return 5;

        try {
            // sets up the session
            session = (new JSch()).getSession(username, server_ip, sftp_port);
            session.setPassword(password);
            session.setConfig( "StrictHostKeyChecking", "no" );
            session.connect();
        } catch( JSchException ex ) {
            return 2;
            //throw new IOException("Fehler beim SFTP-Connect mit '" + username + "' an '" + host + "': ", ex);
        }
        try {
            channel = (ChannelSftp) session.openChannel( "sftp" );
            if( channel == null ) {
                channel.exit();
                System.err.println( "Fehler beim Oeffnen des SFTP-Channel zur SFTP-Session mit '" + session.getUserName() + "' an '" + session.getHost() + "'. " );
            }
            channel.connect();
        } catch( JSchException ex ) {
            channel.exit();
            System.err.println("Fehler beim Oeffnen des SFTP-Channel zur SFTP-Session mit '" + session.getUserName() + "' an '" + session.getHost() + "': ");
            return 3;
        }
        return 0;
    }

    @Override
    public boolean isConnected() {
        return session.isConnected() && channel.isConnected();
    }

    @Override
    public void closeConnection() {
        if (session == null || channel == null)
            return;

        channel.exit();
        session.disconnect();
    }

    private void updateOutput(String path) throws SftpException {
        output = channel.ls(path).toString();
        usableOutput = analyseOutput();
    }

    private String[] analyseOutput() {
        String[] outputSplit;
        outputSplit = output.substring(1, output.length()-1).split(",");
        for (int i = 0; i < outputSplit.length; i++) {
            outputSplit[i] = outputSplit[i].trim();

            if (outputSplit[i].endsWith(" ..") || outputSplit[i].endsWith(" ."))
                outputSplit[i] = null;
        }

        int counter = 0;
        for (int i = 0; i < outputSplit.length; i++) {
            if (!(outputSplit[i] == null)) {
                outputSplit[counter] = outputSplit[i];
                counter++;
            }
        }

        String[] cleansedSplit = new String[outputSplit.length-(outputSplit.length - counter)];
        for (int i = 0; i < cleansedSplit.length; i++) {
            cleansedSplit[i] = outputSplit[i];
        }

        return cleansedSplit;
    }
    private static String readDate(String[] currentSplit) {
        int offset = 0;
        String date = "";
        try {
            date = Integer.parseInt(currentSplit[5]) + "-";
            offset++;
        } catch (NumberFormatException e) {
            date = new SimpleDateFormat("yyyy").format(System.currentTimeMillis()) + "-";
        }

        switch (currentSplit[5 + offset]) {
            case "Jan" : date+="01-"; break;
            case "Feb" : date+="02-"; break;
            case "Mar" : date+="03-"; break;
            case "Apr" : date+="04-"; break;
            case "May" : date+="05-"; break;
            case "Jun" : date+="06-"; break;
            case "Jul" : date+="07-"; break;
            case "Aug" : date+="08-"; break;
            case "Sep" : date+="09-"; break;
            case "Okt" : date+="10-"; break;
            case "Nov" : date+="11-"; break;
            case "Dec" : date+="12-"; break;
        }

        try {
            date += Integer.parseInt(currentSplit[6+offset]);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return date;
    }

    @Override
    public RemoteDirectory[] getDirList(String path) throws IOException {
        try {
            updateOutput(path);
        } catch (SftpException e) {
            throw new IOException("cannot talk to Server anymore");
        }

        List<RemoteDirectory> remoteDirectories = new ArrayList<>();

        for (String s :
                usableOutput) {
            System.out.println(s);
        }

        for (String s: usableOutput) {
            String[] currentSplit = s.split(" ");

            int counter = 0;
            for (int i = 0; i < currentSplit.length; i++) {
                if (!(currentSplit[i].equals(""))) {
                    currentSplit[counter] = currentSplit[i];
                    counter++;
                }
            }

            String[] cleanedSplit = new String[currentSplit.length-(currentSplit.length - counter)];
            for (int i = 0; i < cleanedSplit.length; i++) {
                cleanedSplit[i] = currentSplit[i];
            }

            if (currentSplit[0].startsWith("d"))
                remoteDirectories.add(new RemoteDirectory(path + "/" + cleanedSplit[cleanedSplit.length-1] + "/", readDate(cleanedSplit)));

        }
        return remoteDirectories.toArray(new RemoteDirectory[remoteDirectories.size()]);
    }

    // path = "/"
    @Override
    public RemoteFile[] getFileList(String path) throws IOException {
        try {
            updateOutput(path);
        } catch (SftpException e) {
            throw new IOException(e.toString());
        }
        List<RemoteFile> remoteFiles = new ArrayList<>();

        for (String s :
                usableOutput) {
            System.out.println(s);
        }

        for (String s: usableOutput) {
            String[] currentSplit = s.split(" ");

            int counter = 0;
            for (int i = 0; i < currentSplit.length; i++) {
                if (!(currentSplit[i].equals(""))) {
                    currentSplit[counter] = currentSplit[i];
                    counter++;
                }
            }

            String[] cleanedSplit = new String[currentSplit.length-(currentSplit.length - counter)];
            for (int i = 0; i < cleanedSplit.length; i++) {
                cleanedSplit[i] = currentSplit[i];
            }

            if (currentSplit[0].startsWith("-")) {
                remoteFiles.add(new RemoteFile(cleanedSplit[cleanedSplit.length - 1], path, Long.parseLong(cleanedSplit[4]), readDate(cleanedSplit)));
            }
        }
        return remoteFiles.toArray(new RemoteFile[remoteFiles.size()]);
    }


    @Override
    public ArrayList getAllFileList(String path) throws IOException {
        if (!session.isConnected())
        {
            //Log.e(TAG, "getAllFileList : wasnt connected to server, call connect() first");
            return null;
        }
        return null;
    }

    @Override
    public boolean downloadFile(String remoteFileName, String downloadPath) throws IOException {
        try {
            channel.get(remoteFileName, downloadPath);
        } catch (SftpException e) {
            return false;
        }
    return true;
    }
}
