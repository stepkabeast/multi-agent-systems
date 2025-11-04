package main;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;
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

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("calculator");
        sd.setName("calculator-service");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            logger.severe("❌ Ошибка регистрации в DF: " + e.getMessage());
        }

        addBehaviour(new RequestHandler());
    }

    private class RequestHandler extends CyclicBehaviour {
        private boolean isBusy = false;

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null && msg.getPerformative() == ACLMessage.REQUEST) {
                logger.info("📥 Получен запрос: " + msg.getContent());

                if (isBusy) {
                    ACLMessage refuse = msg.createReply();
                    refuse.setPerformative(ACLMessage.REFUSE);
                    refuse.setContent("Calculator is busy");
                    send(refuse);
                    logger.info("❌ Отклонён: агент занят");
                    return;
                }

                isBusy = true;
                logger.info("⏳ " + getLocalName() + " начал вычисление... (имитация загрузки)");

                try {
                    String[] parts = msg.getContent().split(",");
                    int start = Integer.parseInt(parts[0].trim());
                    int end = Integer.parseInt(parts[1].trim());

                    Thread.sleep(5000);

                    int sum = 0;
                    for (int i = Math.min(start, end); i <= Math.max(start, end); i++) {
                        sum += i;
                    }

                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.CONFIRM);
                    reply.setContent(String.valueOf(sum));
                    send(reply);

                    logger.info("📤 Ответ отправлен: " + sum);
                } catch (Exception e) {
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("Ошибка: " + e.getMessage());
                    send(reply);
                    logger.warning("❌ Ошибка обработки: " + e.getMessage());
                } finally {
                    isBusy = false;
                }
            } else {
                block();
            }
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            logger.severe("❌ Ошибка при выходе: " + e.getMessage());
        }
    }
}