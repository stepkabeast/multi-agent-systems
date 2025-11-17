package main.agents.calculator;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;

public class SumCalculationBehaviour extends OneShotBehaviour {

    private static final Logger logger = Logger.getMyLogger(SumCalculationBehaviour.class.getName());
    private static final long SIMULATED_PROCESSING_TIME = 5000; // 5 seconds

    private final AID requester;
    private final Interval interval;

    public SumCalculationBehaviour(Agent agent, AID requester, Interval interval) {
        super(agent);
        this.requester = requester;
        this.interval = interval;
    }

    @Override
    public void action() {
        try {
            logger.info("Starting calculation for interval [" + interval.start + ", " + interval.end + "]");
            long result = calculateArithmeticProgressionSum();
            simulateProcessingDelay();
            sendResult(result);
            logger.info("Calculation completed. Sum: " + result);

        } catch (Exception e) {
            logger.log(Logger.SEVERE, "Calculation failed for interval: " + interval, e);
            sendError(e.getMessage());
        }
    }

    private long calculateArithmeticProgressionSum() {
        //arithmetic progression: n * (a1 + an) / 2
        long n = interval.end - interval.start + 1;
        long sum = n * (interval.start + interval.end) / 2;
        return sum;
    }

    private void simulateProcessingDelay() {
        try {
            Thread.sleep(SIMULATED_PROCESSING_TIME);
        } catch (InterruptedException e) {
            logger.warning("Calculation was interrupted");
            Thread.currentThread().interrupt();
        }
    }

    private void sendResult(long result) {
        ACLMessage reply = new ACLMessage(ACLMessage.CONFIRM);
        reply.addReceiver(requester);
        reply.setContent(String.valueOf(result));
        myAgent.send(reply);
    }

    private void sendError(String error) {
        ACLMessage reply = new ACLMessage(ACLMessage.FAILURE);
        reply.addReceiver(requester);
        reply.setContent("Calculation error: " + error);
        myAgent.send(reply);
    }
}

// Helper class for interval representation
class Interval {
    final int start;
    final int end;

    public Interval(int start, int end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public String toString() {
        return "[" + start + ", " + end + "]";
    }
}