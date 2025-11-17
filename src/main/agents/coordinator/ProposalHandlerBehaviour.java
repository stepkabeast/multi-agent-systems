package main.agents.coordinator;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;

import java.util.*;

public class ProposalHandlerBehaviour extends Behaviour {

    private static final Logger logger = Logger.getMyLogger(ProposalHandlerBehaviour.class.getName());
    private static final MessageTemplate CFP_TEMPLATE =
            MessageTemplate.MatchPerformative(ACLMessage.CFP);
    private static final MessageTemplate ACCEPT_PROPOSAL_TEMPLATE =
            MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);

    private enum State { WAITING_FOR_CFP, WAITING_FOR_ACCEPT }
    private State currentState = State.WAITING_FOR_CFP;

    private ACLMessage lastCFP;
    private boolean taskAccepted = false;

    public ProposalHandlerBehaviour(Agent agent) {
        super(agent);
    }

    @Override
    public void action() {
        switch (currentState) {
            case WAITING_FOR_CFP:
                handleCFP();
                break;
            case WAITING_FOR_ACCEPT:
                handleAcceptProposal();
                break;
        }
    }

    @Override
    public boolean done() {
        return taskAccepted;
    }

    @Override
    public int onEnd() {
        return taskAccepted ? 1 : 0;
    }

    @Override
    public void reset() {
        super.reset();
        currentState = State.WAITING_FOR_CFP;
        lastCFP = null;
        taskAccepted = false;
    }

    private void handleCFP() {
        ACLMessage cfp = myAgent.receive(CFP_TEMPLATE);
        if (cfp != null) {
            lastCFP = cfp;
            sendProposal(cfp);
            currentState = State.WAITING_FOR_ACCEPT;
            logger.info("Sent proposal for CFP from " + cfp.getSender().getLocalName());
        } else {
            block();
        }
    }

    private void handleAcceptProposal() {
        ACLMessage accept = myAgent.receive(ACCEPT_PROPOSAL_TEMPLATE);
        if (accept != null) {
            if (isValidAcceptProposal(accept)) {
                processTaskDistribution(accept);
                taskAccepted = true;
            } else {
                logger.warning("Received invalid ACCEPT_PROPOSAL from " + accept.getSender().getLocalName());
                currentState = State.WAITING_FOR_CFP;
            }
        } else {
            block();
        }
    }

    private boolean isValidAcceptProposal(ACLMessage accept) {
        return lastCFP != null &&
                accept.getSender().equals(lastCFP.getSender()) &&
                lastCFP.getContent() != null;
    }

    private void sendProposal(ACLMessage cfp) {
        ACLMessage propose = cfp.createReply();
        propose.setPerformative(ACLMessage.PROPOSE);
        int cost = calculateCost();
        propose.setContent(String.valueOf(cost));
        myAgent.send(propose);
    }

    private void processTaskDistribution(ACLMessage accept) {
        try {
            // Получаем калькуляторы из DF
            Set<AID> calculators = discoverCalculatorAgents();
            if (calculators.isEmpty()) {
                logger.warning("No calculator agents found. Cannot distribute task.");
                sendRefusal(accept);
                return;
            }

            // Парсим интервал
            Interval taskInterval = parseTaskInterval(lastCFP.getContent());
            if (taskInterval == null) {
                logger.warning("Invalid task interval: " + lastCFP.getContent());
                sendRefusal(accept);
                return;
            }

            // Распределяем задачу
            distributeTaskToCalculators(calculators, taskInterval, accept);

            logger.info("Task distributed to " + calculators.size() + " calculators for interval " + taskInterval);

        } catch (Exception e) {
            logger.log(Logger.SEVERE, "Error during task distribution", e);
            sendRefusal(accept);
        }
    }

    private Set<AID> discoverCalculatorAgents() throws FIPAException {
        Set<AID> calculators = new HashSet<>();

        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("calculation");
        template.addServices(sd);

        DFAgentDescription[] results = DFService.search(myAgent, template);
        for (DFAgentDescription dfd : results) {
            calculators.add(dfd.getName());
        }

        return calculators;
    }

    private Interval parseTaskInterval(String content) {
        try {
            String[] parts = content.split(",");
            int a = Integer.parseInt(parts[0].trim());
            int b = Integer.parseInt(parts[1].trim());
            return new Interval(Math.min(a, b), Math.max(a, b));
        } catch (Exception e) {
            return null;
        }
    }

    private void distributeTaskToCalculators(Set<AID> calculators, Interval taskInterval, ACLMessage accept) {
        List<Interval> subIntervals = IntervalSplitter.split(taskInterval, calculators.size());

        // Сохраняем состояние для сборщика результатов
        CalculationTask task = new CalculationTask(accept, calculators);
        getDataStore().put("currentTask", task);

        // Отправляем подзадачи калькуляторам
        int i = 0;
        for (AID calculator : calculators) {
            Interval subInterval = subIntervals.get(i++);
            sendCalculationRequest(calculator, subInterval);
        }
    }

    private void sendCalculationRequest(AID calculator, Interval interval) {
        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
        request.addReceiver(calculator);
        request.setLanguage("sum");
        request.setContent(interval.start + "," + interval.end);
        myAgent.send(request);

        logger.fine("Sent calculation request for " + interval + " to " + calculator.getLocalName());
    }

    private void sendRefusal(ACLMessage accept) {
        ACLMessage refuse = accept.createReply();
        refuse.setPerformative(ACLMessage.REFUSE);
        refuse.setContent("Unable to process task");
        myAgent.send(refuse);
        currentState = State.WAITING_FOR_CFP;
    }

    private int calculateCost() {
        return 10 + (int) (Math.random() * 90);
    }
}