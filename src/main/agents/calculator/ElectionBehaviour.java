package main.agents.calculator;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.DataStore;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.util.Logger;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static main.agents.coordinator.AgentCoordinator.STATE_PROPOSAL_HANDLER;
import static main.agents.coordinator.AgentCoordinator.STATE_RESULT_COLLECTOR;
import static main.agents.coordinator.AgentCoordinator.TRANSITION_CONTINUE;
import static main.agents.coordinator.AgentCoordinator.TRANSITION_TO_COLLECTION;

import main.agents.coordinator.ProposalHandlerBehaviour;
import main.agents.coordinator.ResultCollectorBehaviour;

public class ElectionBehaviour extends OneShotBehaviour {

    private static final Logger logger = Logger.getMyLogger(ElectionBehaviour.class.getName());
    private static final String CALCULATOR_SERVICE_TYPE = "calculation";
    private static final int ELECTION_TIMEOUT = 5000;

    public static volatile boolean electionCompleted = false;

    @Override
    public void action() {
        if (electionCompleted) {
            logger.fine("Election already completed. Skipping...");
            return;
        }

        Agent myAgent = getAgent();
        String myName = myAgent.getLocalName();

        try {
            Set<String> calculatorNames = discoverAllCalculators();
            if (calculatorNames.isEmpty()) {
                logger.warning("No calculator agents found. Cannot run election.");
                electionCompleted = true;
                return;
            }

            logger.info("Discovered calculators: " + calculatorNames);

            String winnerName = calculatorNames.stream().min(String::compareTo).orElse(myName);
            logger.info("Election winner: " + winnerName + " (current: " + myName + ")");

            if (myName.equals(winnerName)) {
                if (isCoordinatorRegistered()) {
                    logger.info("Coordinator already exists. Skipping promotion.");
                } else {
                    promoteToCoordinator();
                    logger.info("I am the elected coordinator: " + myName);
                }
            } else {
                if (isCoordinatorRegistered()) {
                    logger.info("Coordinator already exists: " + winnerName);
                } else {
                    logger.warning("I am not the winner, but no coordinator found. Waiting...");
                    waitForCoordinatorRegistration();
                }
            }

            electionCompleted = true;

        } catch (Exception e) {
            logger.log(Logger.SEVERE, "Unexpected error during election", e);
            electionCompleted = true;
        }
    }

    private Set<String> discoverAllCalculators() throws FIPAException {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(CALCULATOR_SERVICE_TYPE);
        template.addServices(sd);

        DFAgentDescription[] results = DFService.search(myAgent, template);
        return Arrays.stream(results)
                .map(desc -> desc.getName().getLocalName())
                .collect(Collectors.toSet());
    }

    private boolean isCoordinatorRegistered() {
        try {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("coordinator");
            template.addServices(sd);

            DFAgentDescription[] results = DFService.search(myAgent, template);
            return results.length > 0;
        } catch (FIPAException e) {
            logger.warning("Failed to check coordinator registration: " + e.getMessage());
            return false;
        }
    }

    private void waitForCoordinatorRegistration() {
        long deadline = System.currentTimeMillis() + ELECTION_TIMEOUT;
        while (System.currentTimeMillis() < deadline) {
            if (isCoordinatorRegistered()) {
                logger.info("Coordinator registered by another agent");
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        logger.warning("Timeout waiting for coordinator registration");
    }

    private void promoteToCoordinator() {
        Agent myAgent = getAgent();

        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(myAgent.getAID());

            ServiceDescription calcSd = new ServiceDescription();
            calcSd.setType("calculation");
            calcSd.setName(myAgent.getLocalName() + "-calculation-service");
            dfd.addServices(calcSd);

            ServiceDescription coordSd = new ServiceDescription();
            coordSd.setType("coordinator");
            coordSd.setName(myAgent.getLocalName() + "-coordinator-service");
            dfd.addServices(coordSd);

            DFService.modify(myAgent, dfd);
            logger.fine("Successfully promoted to coordinator via DF modify");

            FSMBehaviour fsm = new FSMBehaviour(myAgent);
            DataStore sharedDataStore = new DataStore();

            ProposalHandlerBehaviour proposalHandler = new ProposalHandlerBehaviour(myAgent);
            ResultCollectorBehaviour resultCollector = new ResultCollectorBehaviour(myAgent);

            proposalHandler.setDataStore(sharedDataStore);
            resultCollector.setDataStore(sharedDataStore);

            fsm.registerFirstState(proposalHandler, STATE_PROPOSAL_HANDLER);
            fsm.registerState(resultCollector, STATE_RESULT_COLLECTOR);

            fsm.registerTransition(STATE_PROPOSAL_HANDLER, STATE_RESULT_COLLECTOR, TRANSITION_TO_COLLECTION);
            fsm.registerTransition(STATE_PROPOSAL_HANDLER, STATE_PROPOSAL_HANDLER, TRANSITION_CONTINUE);
            fsm.registerDefaultTransition(STATE_RESULT_COLLECTOR, STATE_PROPOSAL_HANDLER);

            myAgent.addBehaviour(fsm);
            ((AgentCalculator) myAgent).setCoordinator(true);
            myAgent.addBehaviour(new SuicideBehaviour(myAgent));

            logger.info("Coordinator FSM initialized. Will self-destruct in 30 seconds.");

        } catch (FIPAException e) {
            logger.log(Logger.SEVERE, "Failed to promote agent to coordinator", e);
        }
    }

    public static void deregisterAsCoordinator(Agent agent) {
        try {
            DFAgentDescription template = new DFAgentDescription();
            template.setName(agent.getAID());

            ServiceDescription sd = new ServiceDescription();
            sd.setType("coordinator");
            template.addServices(sd);

            DFService.deregister(agent, template);
            logger.fine("Coordinator service deregistered by request");
        } catch (FIPAException e) {
            logger.log(Logger.WARNING, "Failed to deregister coordinator service", e);
        }
    }

    /**
     * Поведение для автоматического завершения координатора через 30 секунд
     */
    private static class SuicideBehaviour extends WakerBehaviour {
        private final Agent myAgent;

        public SuicideBehaviour(Agent agent) {
            super(agent, 30000);
            this.myAgent = agent;
        }

        @Override
        protected void onWake() {
            Logger.getMyLogger(ElectionBehaviour.class.getName())
                    .info("Coordinator is committing suicide after 30 seconds...");

            // Дерегистрируем сервис coordinator
            try {
                DFAgentDescription template = new DFAgentDescription();
                template.setName(myAgent.getAID());

                ServiceDescription sd = new ServiceDescription();
                sd.setType("coordinator");
                template.addServices(sd);

                DFService.deregister(myAgent, template);
                Logger.getMyLogger(ElectionBehaviour.class.getName()).info("Coordinator deregistered from DF");
            } catch (FIPAException e) {
                Logger.getMyLogger(ElectionBehaviour.class.getName()).warning("Failed to deregister: " + e.getMessage());
            }

            electionCompleted = false;
            // Перезапускаем выборы
            myAgent.addBehaviour(new ElectionBehaviour());
        }
    }
}