package v3.domain;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

// Represents a position on the plane with x and y coordinates.
public class Position implements Serializable, Comparable<Position> {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int x;
    private final int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }
    public int getY() { return y; }

    @Override
    public int compareTo(Position other) {
        int cmp = Integer.compare(this.x, other.x);
        return (cmp != 0) ? cmp : Integer.compare(this.y, other.y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Position position = (Position) o;
        return x == position.x && y == position.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "Position(" + x + ", " + y + ")";
    }
}