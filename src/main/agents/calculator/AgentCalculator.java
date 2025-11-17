package main.agents.calculator;

import jade.core.Agent;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.util.Logger;

public class AgentCalculator extends Agent {

    private static final String SERVICE_TYPE = "calculation";
    private static final Logger logger = Logger.getMyLogger(AgentCalculator.class.getName());

    private final ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();

    @Override
    protected void setup() {
        registerWithDF();
        addBehaviour(new CalculationRequestBehaviour(this, tbf));
        logger.info("Calculator agent " + getLocalName() + " initialized successfully");
    }

    @Override
    protected void takeDown() {
        tbf.interrupt();
        deregisterFromDF();
        logger.info("Calculator agent " + getLocalName() + " terminated");
    }

    private void registerWithDF() {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());

            ServiceDescription sd = new ServiceDescription();
            sd.setType(SERVICE_TYPE);
            sd.setName(getLocalName() + "-calculation-service");
            dfd.addServices(sd);

            DFService.register(this, dfd);
            logger.fine("Registered in DF as '" + SERVICE_TYPE + "' service");
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
}