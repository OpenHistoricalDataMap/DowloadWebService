import FTPClient.FtpClientImpl;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;

public class Playground {
    static FTPClient client = null;
    static String server = "localhost";
    static int port = 5000;
    static String user = "ohdmOffViewer";
    static String pass = "H!3r0glyph Sat3llite Era$er";
    public static void main(String [] args) {

        //initialise the client
        initPassiveClient();

        //do stuff
        FTPFile[] files = listFiles("./");
        if( files != null ) {
            System.err.println("Listing Files:");
            for( FTPFile f : files) {
                System.err.println(f.getName());
            }
        }

        //close the client
        close();
    }

    /**
     * getPassiveClient retrive a FTPClient object that's set to local passive mode
     *
     * @return FTPClient
     */
    public static FTPClient initPassiveClient() {
        if( client == null ) {
            System.out.println("Getting passive FTP client");
            client = new FTPClient();

            try {
                client.connect(server, port);
                // After connection attempt, you should check the reply code to verify
                // success.
                int reply = client.getReplyCode();

                if(!FTPReply.isPositiveCompletion(reply)) {
                    client.disconnect();
                    System.err.println("FTP server refused connection.");
                    System.exit(0);
                }

                //after connecting to the server set the local passive mode
                //client.enterLocalPassiveMode();
                client.enterRemoteActiveMode(InetAddress.getByName(server), port);

                //send username and password to login to the server
                if( !client.login(user, pass) ) {
                    System.err.println("Could not login to FTP Server");
                    System.exit(0);
                }
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return client;
    }

    public static void close() {
        if( client == null ) {
            System.err.println("Nothing to close, the FTPClient wasn't initialized");
            return;
        }

        //be polite and logout & close the connection before the application finishes
        try {
            client.logout();
            client.disconnect();
        } catch (IOException e) {
            String message = "Could not logout";
            System.err.println(message+"\n");
        }
    }

    /**
     * listFiles uses the FTPClient to retrieve files in the specified directory
     *
     * @return array of FTPFile objects
     */
    private static FTPFile[] listFiles(String dir) {
        if( client == null ) {
            System.err.println("First initialize the FTPClient by calling 'initFTPPassiveClient()'");
            return null;
        }

        try {
            System.out.println("DEBUG: Getting file listing for current director");
            FTPFile[] files = client.listFiles(dir);

            return files;
        } catch (IOException e) {
            String message = "";
            System.err.println(message+"\n");
        }

        return null;
    }
}
