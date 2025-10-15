package main;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.util.Logger;

import java.util.*;
public class NodeAgent extends Agent {
    private String name;
    private Set<String> neighbors = new HashSet<>();
    private Logger logger = Logger.getMyLogger(getClass().getName());

    protected void setup() {
        // Получаем имя и соседей из аргументов
        Object[] args = getArguments();
        if (args.length > 0) {
            this.name = (String) args[0];
            for (int i = 1; i < args.length; i++) {
                neighbors.add((String) args[i]);
            }
        }

        logger.log(Logger.INFO, "Agent " + name + " initialized with neighbors: " + neighbors);

        // Добавляем поведение для обработки запросов
        addBehaviour(new RequestHandler());
    }
    private class RequestHandler extends CyclicBehaviour {
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null && msg.getPerformative() == ACLMessage.REQUEST) {
                String targetName = msg.getContent();
                logger.log(Logger.INFO, "Received request from " + msg.getSender().getName() + " to find path to " + targetName);

                List<String> path = findPath(targetName);
                ACLMessage reply = msg.createReply();

                if (path != null) {
                    reply.setPerformative(ACLMessage.CONFIRM);
                    reply.setContent(String.join("\n", path));
                } else {
                    reply.setPerformative(ACLMessage.DISCONFIRM);
                    reply.setContent("No path found");
                }

                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    private List<String> findPath(String targetName) {
        // Поиск в ширину (BFS)
        Queue<String> queue = new LinkedList<>();
        Map<String, String> prev = new HashMap<>();
        Set<String> visited = new HashSet<>();

        queue.add(name);
        visited.add(name);

        while (!queue.isEmpty()) {
            String current = queue.poll();

            if (current.equals(targetName)) {
                return buildPath(prev, targetName);
            }

            for (String neighbor : getNeighbors(current)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    prev.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }

        return null;
    }

    private List<String> buildPath(Map<String, String> prev, String target) {
        List<String> path = new ArrayList<>();
        String at = target;
        while (at != null) {
            path.add(at);
            at = prev.get(at);
        }
        Collections.reverse(path);
        return path;
    }

    private Set<String> getNeighbors(String nodeName) {
        if (nodeName.equals(name)) {
            return neighbors;
        }
        return Collections.emptySet();
    }
}
