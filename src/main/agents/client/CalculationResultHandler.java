package main.agents.client;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;

public class CalculationResultHandler extends CyclicBehaviour {

    private static final Logger logger = Logger.getMyLogger(CalculationResultHandler.class.getName());
    private static final MessageTemplate RESULT_TEMPLATE =
            MessageTemplate.or(
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchPerformative(ACLMessage.FAILURE)
            );

    public CalculationResultHandler(Agent agent) {
        super(agent);
    }

    @Override
    public void action() {
        ACLMessage result = myAgent.receive(RESULT_TEMPLATE);

        if (result != null) {
            processResultMessage(result);
        } else {
            block();
        }
    }

    private void processResultMessage(ACLMessage result) {
        switch (result.getPerformative()) {
            case ACLMessage.INFORM:
                handleSuccessfulResult(result);
                break;
            case ACLMessage.FAILURE:
                handleFailedResult(result);
                break;
            default:
                logger.warning("Unexpected message type: " + result.getPerformative());
        }
    }

    private void handleSuccessfulResult(ACLMessage result) {
        String calculationResult = result.getContent();
        logger.info("FINAL RESULT: Calculation completed successfully. Result: " + calculationResult);

        // Здесь можно добавить дополнительную обработку успешного результата
        // Например, сохранение в базу данных, вывод пользователю и т.д.
        notifyUserInterface("Calculation completed: " + calculationResult);
    }

    private void handleFailedResult(ACLMessage result) {
        String errorMessage = result.getContent();
        logger.severe("FINAL RESULT: Calculation failed. Error: " + errorMessage);

        // Обработка неудачного завершения
        notifyUserInterface("Calculation failed: " + errorMessage);
    }

    private void notifyUserInterface(String message) {
        // Заглушка для взаимодействия с пользовательским интерфейсом
        // В реальной системе здесь может быть вызов GUI, веб-сервиса и т.д.
        System.out.println("[" + myAgent.getLocalName() + "] " + message);
    }
}