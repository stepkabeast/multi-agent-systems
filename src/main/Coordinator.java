package main;

import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;
import jade.core.behaviours.CyclicBehaviour;
import java.util.*;

public class Coordinator extends Agent {
    private Logger logger = Logger.getMyLogger(getClass().getName());
    private List<String> calculatorNames;
    private Map<String, Integer> responses = new HashMap<>();
    private int expectedResponses;
    private int totalSum = 0;
    private ACLMessage originalRequest;

    @Override
    protected void setup() {
        logger.info("Координатор " + getLocalName() + " создан.");
        System.out.println("Hello! Coordinator Agent " + getAID().getName() + " is ready.");
        calculatorNames = List.of((String[]) getArguments()[0]);
        expectedResponses = calculatorNames.size();
        addBehaviour(new RequestReceiverBehaviour());
    }

    private class RequestReceiverBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null) {
                if (msg.getPerformative() == ACLMessage.REQUEST) {
                    originalRequest = msg;
                    processRequest(msg);
                } else if (msg.getPerformative() == ACLMessage.CONFIRM) {
                    processResponse(msg);
                }
            } else {
                block();
            }
        }

        private void processRequest(ACLMessage request) {
            logger.info("Получен REQUEST: " + request.getContent());

            try {
                String[] parts = request.getContent().split(",");
                int start = Integer.parseInt(parts[0].trim());
                int end = Integer.parseInt(parts[1].trim());

                int rangeSize = end - start + 1;
                int chunkSize = rangeSize / calculatorNames.size();
                int remainder = rangeSize % calculatorNames.size();

                int currentStart = start;
                for (String calculatorName : calculatorNames) {
                    int currentEnd = currentStart + chunkSize - 1;
                    if (remainder > 0) {
                        currentEnd++;
                        remainder--;
                    }

                    ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
                    message.addReceiver(getAID(calculatorName));
                    message.setLanguage("sum");
                    message.setContent(currentStart + ", " + currentEnd);
                    send(message);

                    logger.info("Отправлен REQUEST к " + calculatorName + ": " +
                            currentStart + " до " + currentEnd);

                    currentStart = currentEnd + 1;
                }
            } catch (Exception e) {
                logger.severe("Ошибка при разбиении задачи: " + e.getMessage());

                ACLMessage failure = request.createReply();
                failure.setPerformative(ACLMessage.FAILURE);
                failure.setContent("Ошибка: " + e.getMessage());
                send(failure);
            }
        }

        private void processResponse(ACLMessage response) {
            logger.info("Получен ответ от " + response.getSender().getLocalName());
            String senderName = response.getSender().getLocalName();

            try {
                int sum = Integer.parseInt(response.getContent());
                responses.put(senderName, sum);
                totalSum += sum;
                logger.info("Сумма от " + senderName + ": " + sum);

                if (responses.size() == expectedResponses) {
                    sendFinalResult(originalRequest);
                }
            } catch (NumberFormatException e) {
                logger.warning("Некорректный формат ответа от " + senderName);
            }
        }
    }

    private void sendFinalResult(ACLMessage originalRequest) {
        ACLMessage inform = originalRequest.createReply();
        inform.setPerformative(ACLMessage.INFORM);
        inform.setContent("Итоговая сумма: " + totalSum);
        send(inform);

        logger.info("Отправлен итог: " + totalSum);
    }
}