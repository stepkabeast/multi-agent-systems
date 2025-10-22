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

    private static Map<String, Integer> nameToId = new HashMap<>(); // Отображение имени → ID
    private static int nextId = 0;
    private static Graph graph;

//    public NodeAgent() {
//    }

    protected void setup() {
        this.name = getLocalName();
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            for (Object arg : args) {
                neighbors.add((String) arg);
            }
        }
        synchronized (NodeAgent.class) {
            if (graph == null) {
                graph = new Graph();
            }

            if (!nameToId.containsKey(name)) {
                nameToId.put(name, nextId++);
            }
            int agentId = nameToId.get(name);

            for (String neighbor : neighbors) {
                if (!nameToId.containsKey(neighbor)) {
                    nameToId.put(neighbor, nextId++);
                }
                int neighborId = nameToId.get(neighbor);
                graph.addEdge(agentId, neighborId);
            }
        }

        logger.log(Logger.INFO, "Agent " + name + " initialized with neighbors: " + neighbors);
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
        int targetId = nameToId.getOrDefault(targetName, -1);
        if (targetId == -1) {
            return null;
        }

        int agentId = nameToId.get(name);
        List<Integer> pathIds = graph.BFS(agentId, targetId);

        if (pathIds == null) {
            return null;
        }

        List<String> pathNames = new ArrayList<>();
        for (Integer id : pathIds) {
            for (Map.Entry<String, Integer> entry : nameToId.entrySet()) {
                if (entry.getValue().equals(id)) {
                    pathNames.add(entry.getKey());
                    break;
                }
            }
        }

        return pathNames;
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
