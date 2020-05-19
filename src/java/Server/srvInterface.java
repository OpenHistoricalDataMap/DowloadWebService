package Server;

import Server.CustomObjects.Coords;

public interface srvInterface {

    /**
     * checks WORK_QUEUE for requested maps and downloads them
     * ( will run in thread )
     */
    void watch();

    /**
     * creates folders at startup that are necessary for the app to run
     */
    void fixFolders();

    /**
     * calls a script to generate an .osm Map-file with given coordinates as boundaries for a specified date.
     *
     * @param coords coords of User
     * @param date date for map
     * @param name map Name
     *
     * @return 0 on success, anything else on failure.
     */
    int download_map(Coords[] coords, String date, String name);

    /**
     * calls a script to convert an .osm to a .map file.
     *
     * @param nme map Name
     * @return 0 on success, -1 if file with same name already exists, anything else on failure.
     */
    int convert_map(String nme);

}
