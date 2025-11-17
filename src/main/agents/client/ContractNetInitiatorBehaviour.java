package main.agents.client;

import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;
import jade.util.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class ContractNetInitiatorBehaviour extends ContractNetInitiator {

    private static final Logger logger = Logger.getMyLogger(ContractNetInitiatorBehaviour.class.getName());
    private final AID originalRequester;

    public ContractNetInitiatorBehaviour(Agent agent, ACLMessage cfp, AID originalRequester) {
        super(agent, cfp);
        this.originalRequester = originalRequester;
    }

    @Override
    protected void handleRefuse(ACLMessage refuse) {
        logger.info("Proposal refused by " + refuse.getSender().getLocalName() +
                ": " + refuse.getContent());
    }

    @Override
    protected void handleProposal(ACLMessage propose, Vector acceptances) {
        logger.info("Received proposal from " + propose.getSender().getLocalName() +
                " with cost: " + propose.getContent());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void handleAllResponses(Vector responses, Vector acceptances) {
        logger.info("Processing " + responses.size() + " responses from coordinators");

        Optional<ACLMessage> bestProposal = findBestProposal(responses);

        if (bestProposal.isPresent()) {
            processBestProposal(bestProposal.get(), responses, acceptances);
        } else {
            handleNoValidProposals(responses, acceptances);
        }
    }

    private Optional<ACLMessage> findBestProposal(Vector responses) {
        return responses.stream()
                .map(msg -> (ACLMessage) msg)
                .filter(msg -> msg.getPerformative() == ACLMessage.PROPOSE)
                .filter(this::isValidProposal)
                .min(this::compareProposalsByCost);
    }

    private boolean isValidProposal(ACLMessage proposal) {
        try {
            String content = proposal.getContent();
            return content != null && content.matches("\\d+");
        } catch (Exception e) {
            logger.warning("Invalid proposal content from " + proposal.getSender().getLocalName() +
                    ": " + proposal.getContent());
            return false;
        }
    }

    private int compareProposalsByCost(ACLMessage prop1, ACLMessage prop2) {
        try {
            int cost1 = Integer.parseInt(prop1.getContent());
            int cost2 = Integer.parseInt(prop2.getContent());
            return Integer.compare(cost1, cost2);
        } catch (NumberFormatException e) {
            logger.warning("Invalid cost format in proposal comparison");
            return 0;
        }
    }

    private void processBestProposal(ACLMessage bestProposal, Vector responses, Vector acceptances) {
        ACLMessage acceptance = bestProposal.createReply();
        acceptance.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
        acceptance.setContent("Accepted - lowest cost proposal");
        acceptances.add(acceptance);

        // Reject all other proposals
        sendRejectionsToOtherProposers(bestProposal, responses, acceptances);

        logger.info("Accepted proposal from " + bestProposal.getSender().getLocalName() +
                " with cost: " + bestProposal.getContent());
    }

    @SuppressWarnings("unchecked")
    private void sendRejectionsToOtherProposers(ACLMessage bestProposal, Vector responses, Vector acceptances) {
        List<ACLMessage> otherProposals = responses.stream()
                .map(msg -> (ACLMessage) msg)
                .filter(msg -> msg.getPerformative() == ACLMessage.PROPOSE)
                .filter(msg -> !msg.getSender().equals(bestProposal.getSender()))
                .collect(Collectors.toList());

        for (ACLMessage proposal : otherProposals) {
            ACLMessage rejection = proposal.createReply();
            rejection.setPerformative(ACLMessage.REJECT_PROPOSAL);
            rejection.setContent("Not selected - higher cost");
            acceptances.add(rejection);

            logger.fine("Rejected proposal from " + proposal.getSender().getLocalName());
        }
    }

    private void handleNoValidProposals(Vector responses, Vector acceptances) {
        logger.warning("No valid proposals received. Rejecting all proposals.");

        responses.stream()
                .map(msg -> (ACLMessage) msg)
                .filter(msg -> msg.getPerformative() == ACLMessage.PROPOSE)
                .forEach(proposal -> {
                    ACLMessage rejection = proposal.createReply();
                    rejection.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    rejection.setContent("No proposals accepted");
                    acceptances.add(rejection);
                });

        // Notify original requester about failure
        sendFailureToOriginalRequester("No coordinator accepted the task");
    }

    @Override
    protected void handleInform(ACLMessage inform) {
        String result = inform.getContent();
        logger.info("Successfully received result from " + inform.getSender().getLocalName() +
                ": " + result);

        forwardResultToOriginalRequester(result);
    }

    @Override
    protected void handleFailure(ACLMessage failure) {
        logger.warning("Task execution failed by " + failure.getSender().getLocalName() +
                ": " + failure.getContent());

        sendFailureToOriginalRequester("Coordinator failed: " + failure.getContent());
    }

    @Override
    protected void handleOutOfSequence(ACLMessage message, String sequenceKey) {
        logger.warning("Out of sequence message from " + message.getSender().getLocalName() +
                " with key: " + sequenceKey);
    }

    @Override
    protected void handleAllResultNotifications(Vector resultNotifications) {
        logger.fine("Contract Net protocol completed with " + resultNotifications.size() + " notifications");
    }

    private void forwardResultToOriginalRequester(String result) {
        ACLMessage resultMessage = new ACLMessage(ACLMessage.INFORM);
        resultMessage.addReceiver(originalRequester);
        resultMessage.setContent(result);
        myAgent.send(resultMessage);

        logger.info("Forwarded result " + result + " to original requester " +
                originalRequester.getLocalName());
    }

    private void sendFailureToOriginalRequester(String error) {
        ACLMessage failureMessage = new ACLMessage(ACLMessage.FAILURE);
        failureMessage.addReceiver(originalRequester);
        failureMessage.setContent(error);
        myAgent.send(failureMessage);

        logger.warning("Sent failure notification to " + originalRequester.getLocalName() +
                ": " + error);
    }
}