package main;

import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;
import jade.core.behaviours.CyclicBehaviour;

public class CalculatorAgent extends Agent {
    private Logger logger = Logger.getMyLogger(getClass().getName());

    @Override
    protected void setup() {
        logger.info("Агент-вычислитель " + getLocalName() + " создан.");
        System.out.println("Hello! Calculator Agent " + getAID().getName() + " is ready.");
        addBehaviour(new RequestReceiverBehaviour());
    }

    private class RequestReceiverBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null) {
                if (msg.getPerformative() == ACLMessage.REQUEST) {
                    processRequest(msg);
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

                int sum = calculateSum(start, end);

                ACLMessage confirm = request.createReply();
                confirm.setPerformative(ACLMessage.CONFIRM);
                confirm.setContent(String.valueOf(sum));
                send(confirm);

                logger.info("Отправлен CONFIRM с суммой: " + sum);
            } catch (Exception e) {
                logger.severe("Ошибка при обработке запроса: " + e.getMessage());

                ACLMessage failure = request.createReply();
                failure.setPerformative(ACLMessage.FAILURE);
                failure.setContent("Ошибка: " + e.getMessage());
                send(failure);
            }
        }

        private int calculateSum(int start, int end) {
            if (start > end) {
                int temp = start;
                start = end;
                end = temp;
            }

            int sum = 0;
            for (int i = start; i <= end; i++) {
                sum += i;
            }
            return sum;
        }
    }
}