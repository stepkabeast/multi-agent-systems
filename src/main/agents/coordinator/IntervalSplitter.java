package main.agents.coordinator;

import java.util.ArrayList;
import java.util.List;

public class IntervalSplitter {

    public static List<Interval> split(Interval interval, int parts) {
        List<Interval> intervals = new ArrayList<>();
        if (parts <= 0) return intervals;

        int totalNumbers = interval.size();
        int baseSize = totalNumbers / parts;
        int remainder = totalNumbers % parts;
        int start = interval.start;

        for (int i = 0; i < parts; i++) {
            int size = baseSize + (i < remainder ? 1 : 0);
            int end = start + size - 1;
            intervals.add(new Interval(start, end));
            start = end + 1;
        }

        return intervals;
    }
}