package Server.FileService.FTPService.FTPCli;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.*;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
@Deprecated
public class FtpClient extends Thread implements FTPInterface {

    public FtpClient(OutputStream os) {
        logStream = new PrintStream(os);
    }

    public FtpClient() {
        LOGGING_OVER_FILE = false;
    }

    private FTPClient client;

    private PrintStream logStream;
    private String LOG_TAG = "FTP Client";
    private boolean LOGGING_OVER_FILE = true;
    @Override
    public int connect(String server, int port, String user, String pass) {
        if( client == null ) {
            log("Getting passive FTP client");
            client = new FTPClient();

            try {

                client.connect(server, port);
                // After connection attempt, you should check the reply code to verify
                // success.
                int reply = client.getReplyCode();

                if(!FTPReply.isPositiveCompletion(reply)) {
                    client.disconnect();
                    log(" ERROR : FTP server refused connection.");
                    return 1;
                }

                //after connecting to the server set the local passive mode
                client.enterLocalPassiveMode();

                //send username and password to login to the server
                if( !client.login(user, pass) ) {
                    log(" ERROR - Could not login to FTP Server");
                    return 2;
                }
            } catch (SocketException e) {
                String message = "Socket exception thrown, Server not found";
                log("ERROR :" + message+"\n" + e);
                return 3;
            } catch (IOException e) {
                String message = "IO Exception";
                log("ERROR :" + message+"\n" + e);
                return 4;
            }
        }
        return 0;
    }
    @Override
    public boolean isConnected() {
        return client.isConnected();
    }
    @Override
    public FTPFile[] getDirList(String path) throws IOException {
        if(client == null) {
            System.out.println("INFO : First initialize the FTPClient by calling 'initFTPPassiveClient()'");
            return null;
        }

        log("DEBUG : Getting file listing for current director");
        FTPFile[] files = client.listFiles(path);
        ArrayList<FTPFile> dirList = new ArrayList<>();
        for (FTPFile f : files) {
            if (f.isDirectory())
                dirList.add(f);
        }

        return dirList.toArray(new FTPFile[dirList.size()]);
    }
    @Override
    public FTPFile[] getFileList(String path) throws IOException {
        if(client == null) {
            System.out.println("INFO : First initialize the FTPClient by calling 'initFTPPassiveClient()'");
            return null;
        }

        log("DEBUG : Getting file listing for current director");
        FTPFile[] files = client.listFiles(path);
        ArrayList<FTPFile> actualFiles = new ArrayList<>();
        for (FTPFile f :
                files) {
            if (!f.isDirectory())
                actualFiles.add(f);
        }
        return actualFiles.toArray(new FTPFile[actualFiles.size()]);
    }
    @Override
    public void downloadFile(String remoteFileName, String downloadPath) throws IOException {
        // Download File using retrieveFile(String, OutputStream)
        OutputStream fileOutputStream = new BufferedOutputStream(new FileOutputStream(new File(downloadPath)));
        boolean success = client.retrieveFile(remoteFileName, fileOutputStream);
        fileOutputStream.close();

        if (success)
            log("INFO : File " + remoteFileName + " has been downloaded successfully.");
        else
            log("ERROR : couldn't download " + remoteFileName + "from server!");

    }
    @Override
    public void closeConnection() {
        if( client == null ) {
            log("Nothing to close, the FTPClient wasn't initialized");
            return;
        }

        //be polite and logout & close the connection before the application finishes
        try {
            client.logout();
            client.disconnect();
        } catch (IOException e) {
            String message = "Could not logout";
            log(message+"\n");
        }
    }
    private void log(String msg) {

        if (!LOGGING_OVER_FILE) {
            SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
            Date date = new Date(System.currentTimeMillis());
            System.out.println(LOG_TAG + " : " + formatter.format(date) + " | " + msg);
            return;
        }

        if (logStream == null) {
            throw new NullPointerException("ERROR: PrintStream for webServiceLog not initialized");
        }


        SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
        Date date = new Date(System.currentTimeMillis());
        logStream.println(LOG_TAG + " : " + formatter.format(date) + " | " + msg);
    }
}
