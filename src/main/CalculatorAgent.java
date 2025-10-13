package main;

import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class CalculatorAgent extends Agent {
    private Logger logger = Logger.getMyLogger(getClass().getName());

    @Override
    protected void setup() {
        logger.info("Агент-вычислитель " + getLocalName() + " создан.");
        System.out.println("Hello! Calculator Agent " + getAID().getName() + " is ready.");

        // Регистрация в DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("calculator");
        sd.setName("calculator-service");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            logger.severe("Ошибка регистрации в DF: " + e.getMessage());
        }

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