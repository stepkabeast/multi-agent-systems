package main;
import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.CyclicBehaviour;


public class SampleAgent extends Agent {

    Logger logger = Logger.getMyLogger(getClass().getName());

    @Override
    protected void setup() {
        logger.info("Агент " + getLocalName() + " создан.");
        //addBehaviour(new PrintAgentNameBehaviour());
        System.out.println("Hello! Agent " + getAID().getName() + " is ready.");
        addBehaviour(new MessageReceiverBehaviour());
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.setContent("d");
        msg.addReceiver(new AID("a"));
        send(msg);
        logger.info("Сообщение: " + msg.getContent());
    }

    private class PrintAgentNameBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            //logger.info("Это поведение агента: " + myAgent.getLocalName());
        }
    }

    private class MessageReceiverBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null) {
//                logger.info("Получено сообщение:");
//                logger.info("Тип (коммуникативный акт): " + getPerformativeName(msg.getPerformative()));
//                logger.info("Тип (протокол): " + msg.getProtocol());
//                logger.info("Отправитель: " + msg.getSender().getLocalName());
//                logger.info("Содержание: " + msg.getContent());
            } else {
                block();
            }
        }
    }
    private String getPerformativeName(int performative) {
        switch (performative) {
            case ACLMessage.INFORM: return "INFORM";
            case ACLMessage.REQUEST: return "REQUEST";
            case ACLMessage.CONFIRM: return "CONFIRM";
            case ACLMessage.QUERY_REF: return "QUERY_REF";
            case ACLMessage.AGREE: return "AGREE";
            case ACLMessage.CANCEL: return "CANCEL";
            case ACLMessage.FAILURE: return "FAILURE";
            case ACLMessage.REFUSE: return "REFUSE";
            case ACLMessage.PROPOSE: return "PROPOSE";
            default: return "UNKNOWN(" + performative + ")";
        }
    }


}