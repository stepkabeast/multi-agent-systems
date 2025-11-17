package main.agents.coordinator;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;

import java.util.HashMap;
import java.util.Map;

public class ResultCollectorBehaviour extends Behaviour {

    private static final Logger logger = Logger.getMyLogger(ResultCollectorBehaviour.class.getName());
    private static final MessageTemplate CONFIRM_TEMPLATE =
            MessageTemplate.MatchPerformative(ACLMessage.CONFIRM);

    private CalculationTask currentTask;
    private Map<AID, Boolean> resultsReceived;
    private long totalResult = 0;
    private boolean collectionComplete = false;

    public ResultCollectorBehaviour(Agent agent) {
        super(agent);
    }

    @Override
    public void onStart() {
        initializeTaskFromDataStore();
    }

    @Override
    public void action() {
        if (currentTask == null) {
            logger.warning("No active task for result collection");
            collectionComplete = true;
            return;
        }

        ACLMessage msg = myAgent.receive(CONFIRM_TEMPLATE);
        if (msg != null) {
            processResultMessage(msg);
        } else {
            block();
        }
    }

    @Override
    public boolean done() {
        return collectionComplete;
    }

    @Override
    public int onEnd() {
        cleanup();
        return 0;
    }

    private void initializeTaskFromDataStore() {
        currentTask = (CalculationTask) getDataStore().get("currentTask");
        if (currentTask != null) {
            initializeResultsTracking();
            logger.info("Started collecting results from " + currentTask.getCalculatorCount() + " calculators");
        } else {
            logger.warning("No task found in data store for result collection");
            collectionComplete = true;
        }
    }

    private void initializeResultsTracking() {
        resultsReceived = new HashMap<>();
        for (AID calculator : currentTask.getCalculators()) {
            resultsReceived.put(calculator, false);
        }
        totalResult = 0;
    }

    private void processResultMessage(ACLMessage msg) {
        AID calculator = msg.getSender();

        if (isValidResultFromCalculator(calculator, msg)) {
            processValidResult(calculator, msg);
        } else {
            processInvalidMessage(msg);
        }

        checkCollectionCompletion();
    }

    private boolean isValidResultFromCalculator(AID calculator, ACLMessage msg) {
        return resultsReceived.containsKey(calculator) &&
                !resultsReceived.get(calculator) &&
                msg.getContent() != null &&
                msg.getContent().matches("-?\\d+");
    }

    private void processValidResult(AID calculator, ACLMessage msg) {
        try {
            long partialResult = Long.parseLong(msg.getContent());
            resultsReceived.put(calculator, true);
            totalResult += partialResult;

            logger.info("Received result " + partialResult + " from " + calculator.getLocalName() +
                    " (Total: " + totalResult + ", Remaining: " + getPendingCount() + ")");

        } catch (NumberFormatException e) {
            logger.warning("Invalid numeric result from " + calculator.getLocalName() + ": " + msg.getContent());
        }
    }

    private void processInvalidMessage(ACLMessage msg) {
        AID sender = msg.getSender();
        if (currentTask.getCalculators().contains(sender) && resultsReceived.get(sender)) {
            logger.warning("Duplicate result from " + sender.getLocalName());
        } else {
            logger.warning("Unexpected message from " + sender.getLocalName() + ", sending REFUSE");
            sendRefuseResponse(msg);
        }
    }

    private void checkCollectionCompletion() {
        if (getPendingCount() == 0) {
            sendFinalResultToInitiator();
            collectionComplete = true;
            logger.info("Result collection completed. Total sum: " + totalResult);
        }
    }

    private void sendFinalResultToInitiator() {
        ACLMessage resultMessage = currentTask.getOriginalAccept().createReply();
        resultMessage.setPerformative(ACLMessage.INFORM);
        resultMessage.setContent(String.valueOf(totalResult));
        myAgent.send(resultMessage);

        logger.info("Sent final result " + totalResult + " to " +
                currentTask.getOriginalAccept().getSender().getLocalName());
    }

    private void sendRefuseResponse(ACLMessage msg) {
        ACLMessage refuse = msg.createReply();
        refuse.setPerformative(ACLMessage.REFUSE);
        refuse.setContent("Unexpected message");
        myAgent.send(refuse);
    }

    private int getPendingCount() {
        return (int) resultsReceived.values().stream().filter(received -> !received).count();
    }

    private void cleanup() {
        if (getDataStore().containsKey("currentTask")) {
            getDataStore().remove("currentTask");
        }
        currentTask = null;
        resultsReceived = null;
        totalResult = 0;
    }
}