package Server.FileService.FTPService.FTPCli;

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
     * gives back the File list, given in given path
     * @param path path where it lists everything
     * @return FTPFiles in path
     * @throws IOException couldn't read from path
     */
    FTPFile[] getFileList(String path) throws IOException;


    /**
     * gives bcak the Dir list, in given path
     * @param path path where it lists everything
     * @return Dirs in path
     * @throws IOException couldn't read from path
     */
    FTPFile[] getDirList(String path) throws IOException;

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
