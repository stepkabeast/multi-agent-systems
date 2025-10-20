package main;
import java.util.*;

public class Graph {
    private int vertices;
    private LinkedList<Integer>[] adj;

    public Graph(int v) {
        vertices = v;
        adj = new LinkedList[v];
        for (int i = 0; i < v; ++i) {
            adj[i] = new LinkedList<>();
        }
    }

    public void addEdge(int v, int w) {
        adj[v].add(w);
    }

    public List<Integer> BFS(int start, int target) {
        boolean[] visited = new boolean[vertices];
        Queue<Integer> queue = new LinkedList<>();
        Map<Integer, Integer> prev = new HashMap<>();

        visited[start] = true;
        queue.add(start);

        while (!queue.isEmpty()) {
            int current = queue.poll();

            if (current == target) {
                // Строим путь из prev
                List<Integer> path = new ArrayList<>();
                int at = target;
                while (at != start) {
                    path.add(at);
                    at = prev.get(at);
                }
                path.add(start);
                Collections.reverse(path);
                return path;
            }

            for (int neighbor : adj[current]) {
                if (!visited[neighbor]) {
                    visited[neighbor] = true;
                    queue.add(neighbor);
                    prev.put(neighbor, current);
                }
            }
        }

        return null; // Путь не найден
    }
}