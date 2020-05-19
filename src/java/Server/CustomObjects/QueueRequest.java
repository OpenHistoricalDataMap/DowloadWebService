package Server.CustomObjects;

import java.util.List;

public class QueueRequest {
    private List<Coords> coordinates;
    private String date;
    private String mapName;

    public QueueRequest(List<Coords> coordinates, String date, String mapName) {
        this.coordinates = coordinates;
        this.date = date;
        this.mapName = mapName;
    }

    public Coords[] getCoordinates() {
        Coords[] returnArray = new Coords[coordinates.size()];
        int i = 0;
        for (Coords c : coordinates) {
            returnArray[i] = c;
            i++;
        }
        return returnArray;
    }

    public String getDate() {
        return date;
    }

    public void setMapName(String mapName) {
        this.mapName = mapName;
    }

    public String getMapName() {
        return mapName;
    }

    public String getPrintableCoordsString() {
        StringBuilder sb = new StringBuilder();
        for (Coords c : coordinates) {
            sb.append("x = " + c.x + " | y = " + c.y + "\n");
        }
        return sb.toString();
    }
}
