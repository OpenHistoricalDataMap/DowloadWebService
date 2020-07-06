package Server.CustomObjects;

import java.text.DecimalFormat;

public class Coords {
    public Double x;
    public Double y;

    public Coords(double x, double y) {
        this.x = Double.valueOf(new DecimalFormat("##.#####").format(x).replaceAll(",", "."));
        this.y = Double.valueOf(new DecimalFormat("##.#####").format(y).replaceAll(",", "."));
    }

    @Override
    public String toString() {
        return String.valueOf(x.toString() + ' ' + y.toString());
    }
}
