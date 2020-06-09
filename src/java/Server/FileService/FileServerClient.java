package Server.FileService;

import java.io.IOException;
import java.util.ArrayList;

public interface FileServerClient<RemoteFile, RemoteDirectory> {
    /**
     * if called, connects to a server
     *
     * @return 0 = successful connection,
     * 1 = SSH service refused connection,
     * 2 = Could not login to FTP Server (probably wrong password),
     * 3 = Transport Exception, couldn't speak to Server
     * 4 = IO Exception
     * 5 = if already connected
     */
    int connect();

    /**
     * checking if connected
     *
     * @return true if connected, else false
     */
    boolean isConnected();

    /**
     * closes connection
     * pls always use at the end !!!!
     */
    void closeConnection();

    /**
     * Returns a list of all directories in <code>path</code>
     *
     * @param path the path
     * @return all directories
     * @throws IOException
     */
    RemoteDirectory[] getDirList(String path) throws IOException;
    /**
     * gives back the File list, given in path dir
     * ignores sub dirs completely
     *
     * @return RemoteFile in current dir
     * @throws IOException couldn't read from current dir
     */
    RemoteFile[] getFileList(String path) throws IOException;

    /**
     * gives back the File list, given in path dir
     * including files from sub dirs
     *
     * @return FTPFiles in current dir
     * @throws IOException couldn't read from current dir
     */
    ArrayList<RemoteFile> getAllFileList(String path) throws IOException;

    /**
     * call to download a File from current dir
     *
     * @param remoteFileName file to download from Server
     * @param downloadPath   Path to write to
     * @throws IOException couldn't download from current dir
     */
    boolean downloadFile(String remoteFileName, String downloadPath) throws IOException;
}
