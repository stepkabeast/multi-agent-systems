package main.agents.client;

import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;
import jade.util.Logger;

import java.util.Vector;

public class ContractNetInitiatorBehaviour extends ContractNetInitiator {

    private static final Logger logger = Logger.getMyLogger(ContractNetInitiatorBehaviour.class.getName());
    private final AID originalRequester;

    public ContractNetInitiatorBehaviour(Agent agent, ACLMessage cfp, AID originalRequester) {
        super(agent, cfp);
        this.originalRequester = originalRequester;
    }

    @Override
    protected void handleAllResponses(Vector responses, Vector acceptances) {
        logger.info("Received " + responses.size() + " proposals.");

        ACLMessage bestProposal = null;
        int minCost = Integer.MAX_VALUE;

        // Find the cheapest proposal
        for (Object obj : responses) {
            ACLMessage msg = (ACLMessage) obj;
            if (msg.getPerformative() == ACLMessage.PROPOSE) {
                try {
                    int cost = Integer.parseInt(msg.getContent().trim());
                    if (cost < minCost) {
                        minCost = cost;
                        bestProposal = msg;
                    }
                } catch (NumberFormatException e) {
                    logger.warning("Invalid cost from " + msg.getSender().getLocalName() + ": " + msg.getContent());
                }
            }
        }

        if (bestProposal != null) {
            // Accept the best proposal
            ACLMessage accept = bestProposal.createReply();
            accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
            acceptances.add(accept);
            logger.info("Accepted proposal from " + bestProposal.getSender().getLocalName() + " (cost: " + minCost + ")");

            // Reject all others
            for (Object obj : responses) {
                ACLMessage msg = (ACLMessage) obj;
                if (msg.getPerformative() == ACLMessage.PROPOSE && !msg.getSender().equals(bestProposal.getSender())) {
                    ACLMessage reject = msg.createReply();
                    reject.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    acceptances.add(reject);
                }
            }
        } else {
            logger.warning("No valid proposals received.");
            // Reject all proposals
            for (Object obj : responses) {
                ACLMessage msg = (ACLMessage) obj;
                if (msg.getPerformative() == ACLMessage.PROPOSE) {
                    ACLMessage reject = msg.createReply();
                    reject.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    acceptances.add(reject);
                }
            }
            sendFailureToOriginalRequester("No coordinators available");
        }
    }

    @Override
    protected void handleInform(ACLMessage inform) {
        String result = inform.getContent();
        logger.info("Result received from " + inform.getSender().getLocalName() + ": " + result);
        forwardResultToOriginalRequester(result);
    }

    @Override
    protected void handleFailure(ACLMessage failure) {
        logger.warning("Failure from " + failure.getSender().getLocalName() + ": " + failure.getContent());
        sendFailureToOriginalRequester("Coordinator failed to execute task: " + failure.getContent());
    }

    @Override
    protected void handleRefuse(ACLMessage refuse) {
        logger.info("Refused by " + refuse.getSender().getLocalName() + ": " + refuse.getContent());
    }

    private void forwardResultToOriginalRequester(String result) {
        ACLMessage reply = new ACLMessage(ACLMessage.INFORM);
        reply.addReceiver(originalRequester);
        reply.setContent(result);
        myAgent.send(reply);
        logger.info("Result forwarded to requester: " + result);
    }

    private void sendFailureToOriginalRequester(String error) {
        ACLMessage reply = new ACLMessage(ACLMessage.FAILURE);
        reply.addReceiver(originalRequester);
        reply.setContent(error);
        myAgent.send(reply);
        logger.warning("Failure sent to requester: " + error);
    }
}