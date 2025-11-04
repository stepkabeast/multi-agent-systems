package main;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeAgent extends Agent {
    private List<String> neighbors = new ArrayList<>();
    private Map<String, String> requestSenders = new HashMap<>(); // Храним отправителей запросов
    Logger logger = Logger.getMyLogger(getClass().getName());
    String name = "";

    @Override
    public void setup() {
        name = getLocalName();
        addBehaviour(new NodeBehaviour());
        Object[] args = getArguments();
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof String) {
                    neighbors.add((String) arg);
                }
            }
        }

        logger.log(Logger.INFO, "Agent <" + name + "> started.");
        logger.log(Logger.INFO, "ID: " + getAID());
        logger.log(Logger.INFO, "His neighbors are: " + neighbors);
    }

    private class NodeBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = receive();
            if (msg != null) {
                String content = msg.getContent();
                String sender = msg.getSender().getLocalName();
                ACLMessage reply = msg.createReply();

                if (msg.getPerformative() == ACLMessage.REQUEST) {
                    String target = content;
                    requestSenders.put(target, sender);

                    if (neighbors.contains(target)) {
                        reply.setPerformative(ACLMessage.CONFIRM);
                        reply.setContent(target + ":" + name);
                    } else {
                        reply.setPerformative(ACLMessage.DISCONFIRM);
                        reply.setContent("No path");

                        ACLMessage forwardMsg = new ACLMessage(ACLMessage.REQUEST);
                        forwardMsg.setContent(target);
                        for (String neighbor : neighbors) {
                            if (!neighbor.equals(sender)) {
                                forwardMsg.addReceiver(new AID(neighbor, AID.ISLOCALNAME));
                            }
                        }
                        send(forwardMsg);
                    }
                    send(reply);
                }
                else if (msg.getPerformative() == ACLMessage.CONFIRM) {
                    String[] parts = content.split(":");
                    if (parts.length >= 2) {
                        String originalTarget = parts[0];
                        String path = parts[1];

                        if (requestSenders.containsKey(originalTarget) &&
                                requestSenders.get(originalTarget).equals(name)) {
                            logger.log(Logger.INFO, "Path to " + originalTarget + ": " + path);
                        } else {
                            String previousSender = requestSenders.get(originalTarget);
                            if (previousSender != null) {
                                ACLMessage forwardConfirm = new ACLMessage(ACLMessage.CONFIRM);
                                forwardConfirm.setContent(originalTarget + ":" + path + "->" + name);
                                forwardConfirm.addReceiver(new AID(previousSender, AID.ISLOCALNAME));
                                send(forwardConfirm);
                            }
                        }
                    }
                }
            } else {
                block();
            }
        }
    }
}