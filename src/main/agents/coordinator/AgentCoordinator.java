package main.agents.coordinator;

import jade.core.Agent;
import jade.core.behaviours.DataStore;
import jade.core.behaviours.FSMBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;

public class AgentCoordinator extends Agent {

    public static final String COORDINATOR_SERVICE_TYPE = "coordinator";
    public static final String CALCULATOR_SERVICE_TYPE = "calculation";
    public static final Logger logger = Logger.getMyLogger(AgentCoordinator.class.getName());

    // Состояния FSM
    public static final String STATE_PROPOSAL_HANDLER = "proposal-handler";
    public static final String STATE_RESULT_COLLECTOR = "result-collector";

    // Коды переходов
    public static final int TRANSITION_CONTINUE = 0;
    public static final int TRANSITION_TO_COLLECTION = 1;

    @Override
    protected void setup() {
        registerWithDF();
        setupFSM();
        logger.info("Coordinator agent " + getLocalName() + " initialized successfully");
    }

    @Override
    protected void takeDown() {
        deregisterFromDF();
        logger.info("Coordinator agent " + getLocalName() + " terminated");
    }

    private void registerWithDF() {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());

            ServiceDescription sd = new ServiceDescription();
            sd.setType(COORDINATOR_SERVICE_TYPE);
            sd.setName(getLocalName() + "-coordinator-service");
            dfd.addServices(sd);

            DFService.register(this, dfd);
            logger.fine("Registered in DF as '" + COORDINATOR_SERVICE_TYPE + "' service");
        } catch (FIPAException e) {
            logger.log(Logger.SEVERE, "DF registration failed", e);
        }
    }

    private void deregisterFromDF() {
        try {
            DFService.deregister(this);
            logger.fine("Deregistered from DF");
        } catch (FIPAException e) {
            logger.log(Logger.WARNING, "DF deregistration failed", e);
        }
    }

    private void setupFSM() {
        FSMBehaviour fsm = new FSMBehaviour(this);
        DataStore sharedDataStore = new DataStore();

        ProposalHandlerBehaviour proposalHandler = new ProposalHandlerBehaviour(this);
        ResultCollectorBehaviour resultCollector = new ResultCollectorBehaviour(this);

        proposalHandler.setDataStore(sharedDataStore);
        resultCollector.setDataStore(sharedDataStore);

        fsm.registerFirstState(proposalHandler, STATE_PROPOSAL_HANDLER);
        fsm.registerState(resultCollector, STATE_RESULT_COLLECTOR);

        fsm.registerTransition(STATE_PROPOSAL_HANDLER, STATE_RESULT_COLLECTOR, TRANSITION_TO_COLLECTION);
        fsm.registerTransition(STATE_PROPOSAL_HANDLER, STATE_PROPOSAL_HANDLER, TRANSITION_CONTINUE);
        fsm.registerDefaultTransition(STATE_RESULT_COLLECTOR, STATE_PROPOSAL_HANDLER);

        addBehaviour(fsm);
    }
}