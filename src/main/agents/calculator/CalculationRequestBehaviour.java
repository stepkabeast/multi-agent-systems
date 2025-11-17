package main.agents.calculator;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;

public class CalculationRequestBehaviour extends CyclicBehaviour {

    private static final Logger logger = Logger.getMyLogger(CalculationRequestBehaviour.class.getName());
    private static final MessageTemplate REQUEST_TEMPLATE =
            MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchLanguage("sum")
            );

    private final ThreadedBehaviourFactory tbf;

    public CalculationRequestBehaviour(Agent agent, ThreadedBehaviourFactory tbf) {
        super(agent);
        this.tbf = tbf;
    }

    @Override
    public void action() {
        ACLMessage msg = myAgent.receive(REQUEST_TEMPLATE);

        if (msg != null) {
            processCalculationRequest(msg);
        } else {
            block();
        }
    }

    private void processCalculationRequest(ACLMessage msg) {
        try {
            String content = msg.getContent();
            logger.info("Received calculation request: " + content + " from " + msg.getSender().getLocalName());

            if (isValidInterval(content)) {
                Interval interval = parseInterval(content);
                myAgent.addBehaviour(tbf.wrap(new SumCalculationBehaviour(myAgent, msg.getSender(), interval)));
            } else {
                sendErrorMessage(msg, "Invalid interval format: " + content);
            }
        } catch (Exception e) {
            logger.log(Logger.WARNING, "Error processing calculation request", e);
            sendErrorMessage(msg, "Processing error: " + e.getMessage());
        }
    }

    private boolean isValidInterval(String content) {
        return content != null && content.matches("-?\\d+\\s*,\\s*-?\\d+");
    }

    private Interval parseInterval(String content) {
        String[] parts = content.split(",");
        int a = Integer.parseInt(parts[0].trim());
        int b = Integer.parseInt(parts[1].trim());
        return new Interval(Math.min(a, b), Math.max(a, b));
    }

    private void sendErrorMessage(ACLMessage originalMsg, String error) {
        ACLMessage reply = originalMsg.createReply();
        reply.setPerformative(ACLMessage.FAILURE);
        reply.setContent(error);
        myAgent.send(reply);
        logger.warning("Sent error response: " + error);
    }
}