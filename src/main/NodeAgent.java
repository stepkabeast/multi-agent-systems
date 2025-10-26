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
    String responser = "";

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
                    String receivedContent = msg.getContent();
                    if (name.equals(receivedContent)) {
                        ACLMessage response = msg.createReply();
                        response.setPerformative(4);
                        response.setContent(msg.getSender().getLocalName() + "\n"+name);
                        send(response);
                    }
                    else {
                        for (String neighbor : neighbors) {
                            if (!neighbor.equals(msg.getSender().getLocalName())) {
                                ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
                                newMsg.setContent(receivedContent);
                                newMsg.addReceiver(new AID(neighbor, AID.ISLOCALNAME));
                                send(newMsg);
                            }
                        }
                    }
            }
            else {
                block();
            }
        }
        private String getPerformativeName(int performative) {
            return switch (performative) {
                case ACLMessage.INFORM -> "INFORM";
                case ACLMessage.REQUEST -> "REQUEST";
                case ACLMessage.CONFIRM -> "CONFIRM";
                case ACLMessage.QUERY_REF -> "QUERY_REF";
                case ACLMessage.AGREE -> "AGREE";
                case ACLMessage.CANCEL -> "CANCEL";
                case ACLMessage.FAILURE -> "FAILURE";
                case ACLMessage.REFUSE -> "REFUSE";
                case ACLMessage.PROPOSE -> "PROPOSE";
                default -> "UNKNOWN(" + performative + ")";
            };
        }
    }
}