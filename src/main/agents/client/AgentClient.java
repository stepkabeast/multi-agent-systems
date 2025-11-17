package main.agents.client;

import jade.core.Agent;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.util.Logger;

public class AgentClient extends Agent {

    private static final Logger logger = Logger.getMyLogger(AgentClient.class.getName());
    private final ThreadedBehaviourFactory threadedFactory = new ThreadedBehaviourFactory();

    @Override
    protected void setup() {
        logger.info("Client agent " + getLocalName() + " initialized");

        // Основной обработчик запросов на вычисления
        addBehaviour(new ClientRequestHandlerBehaviour(this, threadedFactory));

        // Обработчик финальных результатов
        addBehaviour(new CalculationResultHandler(this));
    }

    @Override
    protected void takeDown() {
        threadedFactory.interrupt();
        logger.info("Client agent " + getLocalName() + " terminated");
    }
}