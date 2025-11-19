package main.agents.calculator;

import jade.core.Agent;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.util.Logger;

public class AgentCalculator extends Agent {

    private static final Logger logger = Logger.getMyLogger(AgentCalculator.class.getName());

    private final ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
    private boolean isCoordinator = false;

    @Override
    protected void setup() {
        registerWithDF();
        logger.info("Calculator agent " + getLocalName() + " initialized");
        addBehaviour(new WakerBehaviour(this, 15000) {
            @Override
            protected void onWake() {
                if (!ElectionBehaviour.electionCompleted) {
                    myAgent.addBehaviour(new ElectionBehaviour());
                }
            }
        });

        addBehaviour(new CalculationRequestBehaviour(this, tbf));
    }

    @Override
    protected void takeDown() {
        tbf.interrupt();
        logger.info("Calculator agent " + getLocalName() + " terminated");
    }

    private void registerWithDF() {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());

            ServiceDescription sd = new ServiceDescription();
            sd.setType("calculation");
            sd.setName(getLocalName() + "-calculation-service");
            dfd.addServices(sd);

            DFService.register(this, dfd);
            logger.fine("Registered in DF as 'calculation' service");
        } catch (FIPAException e) {
            logger.log(Logger.SEVERE, "DF registration failed", e);
        }
    }

    public boolean isCoordinator() {
        return isCoordinator;
    }

    public void setCoordinator(boolean coordinator) {
        isCoordinator = coordinator;
    }

    public void deregisterAsCoordinator() {
        ElectionBehaviour.deregisterAsCoordinator(this);
    }
}