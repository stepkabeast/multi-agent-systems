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

import java.util.Set;
import java.util.stream.Collectors;

public class ClientRequestHandlerBehaviour extends Behaviour {

    private static final String COORDINATOR_SERVICE_TYPE = "coordinator";
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
        requestProcessed = false; // Reset for potential reuse
        return 0;
    }

    private void processCalculationRequest(ACLMessage request) {
        try {
            String intervalContent = request.getContent();
            AID originalRequester = request.getSender();

            logger.info("Received calculation request for interval: " + intervalContent +
                    " from " + originalRequester.getLocalName());

            if (isValidInterval(intervalContent)) {
                initiateContractNetProtocol(intervalContent, originalRequester);
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

    private void initiateContractNetProtocol(String intervalContent, AID originalRequester) {
        try {
            Set<AID> coordinators = discoverCoordinatorAgents();

            if (coordinators.isEmpty()) {
                logger.warning("No coordinator agents found. Cannot process request.");
                return;
            }

            ACLMessage cfp = createCFPMessage(intervalContent, coordinators);
            myAgent.addBehaviour(threadedFactory.wrap(
                    new ContractNetInitiatorBehaviour(myAgent, cfp, originalRequester)
            ));

            logger.info("Initiated Contract Net protocol with " + coordinators.size() +
                    " coordinators for interval: " + intervalContent);

        } catch (FIPAException e) {
            logger.log(Logger.SEVERE, "Failed to discover coordinator agents", e);
        }
    }

    private Set<AID> discoverCoordinatorAgents() throws FIPAException {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription serviceDesc = new ServiceDescription();
        serviceDesc.setType(COORDINATOR_SERVICE_TYPE);
        template.addServices(serviceDesc);

        DFAgentDescription[] results = DFService.search(myAgent, template);

        return java.util.Arrays.stream(results)
                .map(DFAgentDescription::getName)
                .collect(Collectors.toSet());
    }

    private ACLMessage createCFPMessage(String intervalContent, Set<AID> coordinators) {
        ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
        cfp.setProtocol("fipa-contract-net");
        cfp.setContent(intervalContent);
        cfp.setReplyByDate(calculateTimeout()); // Set reasonable timeout

        coordinators.forEach(cfp::addReceiver);
        return cfp;
    }

    private java.util.Date calculateTimeout() {
        long timeout = System.currentTimeMillis() + 30000; // 30 seconds timeout
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
    }
}