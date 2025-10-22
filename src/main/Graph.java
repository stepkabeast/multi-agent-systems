package main;
import java.util.*;

public class Graph {
    private Map<Integer, List<Integer>> adj;

    public Graph() {
        adj = new HashMap<>();
    }

    public synchronized void addEdge(int v, int w) {
        adj.computeIfAbsent(v, k -> new ArrayList<>()).add(w);
        // Добавляем обратное ребро для неориентированного графа
        adj.computeIfAbsent(w, k -> new ArrayList<>()).add(v);
    }

    public synchronized List<Integer> BFS(int start, int target) {
        if (!adj.containsKey(start) || !adj.containsKey(target)) {
            return null;
        }

        Set<Integer> visited = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();
        Map<Integer, Integer> prev = new HashMap<>();

        visited.add(start);
        queue.add(start);
        prev.put(start, -1); // Маркер начала пути

        while (!queue.isEmpty()) {
            int current = queue.poll();

            if (current == target) {
                // Строим путь из prev
                List<Integer> path = new ArrayList<>();
                int at = target;
                while (at != -1) {
                    path.add(at);
                    at = prev.get(at);
                }
                Collections.reverse(path);
                return path;
            }

            for (int neighbor : adj.getOrDefault(current, Collections.emptyList())) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                    prev.put(neighbor, current);
                }
            }
        }

        return null; // Путь не найден
    }
}