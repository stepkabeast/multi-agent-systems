package main.agents.coordinator;

public class Interval {
    public final int start;
    public final int end;

    public Interval(int start, int end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public String toString() {
        return "[" + start + ", " + end + "]";
    }

    public int size() {
        return end - start + 1;
    }
}