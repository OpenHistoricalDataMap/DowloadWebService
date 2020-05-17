package FTPClient;

import org.apache.commons.net.ftp.FTPFile;

import java.io.IOException;

public interface FTPInterface {

    /**
     * if called, connects to a server
     *
     * @param server server ip/hostname
     * @param port FTPServer port
     * @param user user to log in with
     * @param pass passwort used to log in
     * @return
     * 0 = successful connection,
     * 1 = FTP server refused connection,
     * 2 = Could not login to FTP Server (probably wrong password),
     * 3 = Socket exception thrown, Server not found,
     * 4 = IO Exception
     */
    int connect(String server, int port, String user, String pass);

    /**
     * checking if connected
     * @return
     */
    boolean isConnected();

    /**
     * gives back the File list, given in current working dir
     * @return FTPFiles in current dir
     * @throws IOException couldn't read from current dir
     */
    FTPFile[] getFileList() throws IOException;

    /**
     * call to download a File from current dir
     * @param remoteFileName file to download from Server
     * @param localFile file to write to
     * @throws IOException couldn't download from current dir
     */
    void downloadFile(String remoteFileName, String localFile) throws IOException;

    /**
     * closes connection
     * pls always use at the end !!!!
     */
    void closeConnection();
}
