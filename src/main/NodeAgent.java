package main;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NodeAgent extends Agent {
    private List<String> neighbors = new ArrayList<>();
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
                String target = msg.getContent();
                String sender = msg.getSender().getLocalName();
                ACLMessage reply = msg.createReply();

                if (msg.getPerformative() == ACLMessage.REQUEST) {

                    if (neighbors.contains(target)) {
                        reply.setPerformative(ACLMessage.CONFIRM);
                        reply.setContent(target + name);
                    } else {
                        reply.setPerformative(ACLMessage.DISCONFIRM);
                        reply.setContent("No path");
                    }
                    send(reply);
                    logger.log(Logger.INFO, reply.getContent());
                    if (reply.getPerformative() == ACLMessage.DISCONFIRM) {
                        ACLMessage forwardMsg = new ACLMessage(ACLMessage.REQUEST);
                        forwardMsg.setContent(target);

                        for (String neighbor : neighbors) {
                            if (!neighbor.equals(sender)) {
                                forwardMsg.addReceiver(new AID(neighbor, AID.ISLOCALNAME));
                                send(forwardMsg);
                            }
                        }
                    }
                }
                else if (msg.getPerformative() == ACLMessage.CONFIRM) {
                    reply.setPerformative(ACLMessage.CONFIRM);
                    reply.setContent(target + name);
                    send(reply);
                }
            }
            else {
                block();
            }
        }
    }
}