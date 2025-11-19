package main.agents.client;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;

public class ClientRequestHandlerBehaviour extends Behaviour {

    private static final Logger logger = Logger.getMyLogger(ClientRequestHandlerBehaviour.class.getName());
    private static final MessageTemplate REQUEST_TEMPLATE =
            MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchLanguage("sum")
            );

    private final ThreadedBehaviourFactory threadedFactory;
    private boolean requestProcessed = false;

    public ClientRequestHandlerBehaviour(Agent agent, ThreadedBehaviourFactory tbf) {
        super(agent);
        this.threadedFactory = tbf;
    }

    @Override
    public void action() {
        ACLMessage request = myAgent.receive(REQUEST_TEMPLATE);

        if (request != null) {
            processCalculationRequest(request);
        } else {
            block();
        }
    }

    @Override
    public boolean done() {
        return requestProcessed;
    }

    @Override
    public int onEnd() {
        requestProcessed = false;
        return 0;
    }

    private void processCalculationRequest(ACLMessage request) {
        try {
            String intervalContent = request.getContent();
            AID originalRequester = request.getSender();

            logger.info("Received calculation request for interval: " + intervalContent +
                    " from " + originalRequester.getLocalName());

            if (isValidInterval(intervalContent)) {
                initiateContractNetProtocol(request, intervalContent);
                requestProcessed = true;
            } else {
                sendInvalidRequestResponse(request, intervalContent);
            }

        } catch (Exception e) {
            logger.log(Logger.SEVERE, "Error processing client request", e);
            sendErrorResponse(request, "Internal error: " + e.getMessage());
        }
    }

    private boolean isValidInterval(String intervalContent) {
        return intervalContent != null && intervalContent.matches("\\d+\\s*,\\s*\\d+");
    }

    private void initiateContractNetProtocol(ACLMessage request, String intervalContent) {
        try {
            AID coordinator = findCoordinatorAgent();
            if (coordinator == null) {
                logger.warning("No coordinator agent found in DF.");
                sendErrorResponse(request, "No coordinator available at the moment.");
                return;
            }

            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            cfp.setProtocol("fipa-contract-net");
            cfp.setContent(intervalContent);
            cfp.addReceiver(coordinator);
            cfp.setReplyByDate(calculateTimeout());

            AID originalRequester = request.getSender();
            myAgent.addBehaviour(threadedFactory.wrap(
                    new ContractNetInitiatorBehaviour(myAgent, cfp, originalRequester)
            ));

            logger.info("Sent CFP to coordinator: " + coordinator.getLocalName());

        } catch (Exception e) {
            logger.log(Logger.SEVERE, "Error during coordinator interaction", e);
            sendErrorResponse(request, "Internal error: " + e.getMessage());
        }
    }

    private AID findCoordinatorAgent() {
        try {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("coordinator");
            template.addServices(sd);

            DFAgentDescription[] results = DFService.search(myAgent, template);
            if (results.length > 0) {
                return results[0].getName();
            }
        } catch (FIPAException e) {
            logger.warning("Failed to search for coordinator: " + e.getMessage());
        }
        return null;
    }

    private java.util.Date calculateTimeout() {
        long timeout = System.currentTimeMillis() + 30000;
        return new java.util.Date(timeout);
    }

    private void sendInvalidRequestResponse(ACLMessage originalRequest, String invalidContent) {
        ACLMessage response = originalRequest.createReply();
        response.setPerformative(ACLMessage.FAILURE);
        response.setContent("Invalid interval format: " + invalidContent +
                ". Expected format: 'start,end' with numbers.");
        myAgent.send(response);
        logger.warning("Sent invalid request response for: " + invalidContent);
    }

    private void sendErrorResponse(ACLMessage originalRequest, String error) {
        ACLMessage response = originalRequest.createReply();
        response.setPerformative(ACLMessage.FAILURE);
        response.setContent(error);
        myAgent.send(response);
        logger.warning("Sent error response: " + error);
    }
}