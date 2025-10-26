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
    Object[] args = getArguments();
    protected String content = "";
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
        /*
           Агент должен ожидать и принимать сообщение от других агентов,
           проверить контекст сообщения и если оно совпадает с именем его соседа,
           то отправить сообщение о том, что это его сосед, иначе сообщить об обратном.
         */
            ACLMessage msg = receive();
            if (msg != null) {
                String receivedContent = msg.getContent();
                boolean isNeighbor = neighbors.contains(receivedContent);
                if (isNeighbor) {
                    ACLMessage response = msg.createReply();
                    response.addReceiver(msg.getSender());
                    String route =  name + " " + msg.getContent();
                    response.setContent(route);
                    send(response);
                    logger.log(Logger.INFO, "Route is: " + route);
                } else {
                    for (String neighbor : neighbors) {
                        sendMessage(receivedContent, neighbor);
                    }
;                }
            }
        }

        public void sendMessage(String message, String receiver) {
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.setContent(message);
            msg.addReceiver(new AID(receiver, AID.ISLOCALNAME));
            send(msg);
        }

    }
}
