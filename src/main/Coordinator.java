package main;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.util.ArrayList;
import java.util.List;

public class Coordinator extends Agent {
    private Logger logger = Logger.getMyLogger(getClass().getName());
    private List<String> calculatorNames = new ArrayList<>();

    @Override
    protected void setup() {
        logger.info("Координатор " + getLocalName() + " создан.");
        System.out.println("Hello! Coordinator Agent " + getAID().getName() + " is ready.");

        addBehaviour(new UpdateCalculatorListBehaviour());
        addBehaviour(new MainBehaviour());
    }

    private class MainBehaviour extends CyclicBehaviour {
        private final MessageTemplate requestTemplate = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
        private final MessageTemplate confirmTemplate = MessageTemplate.MatchPerformative(ACLMessage.CONFIRM);

        private boolean isProcessing = false;
        private int expectedReplies = 0;
        private int receivedReplies = 0;
        private int totalSum = 0;
        private ACLMessage originalRequest = null;

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(requestTemplate);
            if (msg != null) {
                logger.info("📥 Получен запрос от " + msg.getSender().getLocalName() +
                        ": \"" + msg.getContent() + "\"");

                if (isProcessing) {
                    ACLMessage refuse = msg.createReply();
                    refuse.setPerformative(ACLMessage.REFUSE);
                    refuse.setContent("Coordinator is busy");
                    send(refuse);
                    logger.info("❌ Запрос отклонён: координатор занят");
                    return;
                }

                startProcessing(msg);
                return;
            }

            ACLMessage reply = myAgent.receive(confirmTemplate);
            if (reply != null) {
                handleReply(reply);
            }

            if (!isProcessing) {
                block();
            }
        }

        private void startProcessing(ACLMessage msg) {
            logger.info("✅ Начинаем обработку запроса");
            isProcessing = true;
            originalRequest = msg;
            receivedReplies = 0;
            totalSum = 0;

            try {
                String[] parts = msg.getContent().split(",");
                int start = Integer.parseInt(parts[0].trim());
                int end = Integer.parseInt(parts[1].trim());

                logger.info("🔢 Диапазон: " + start + " - " + end);
                int numAgents = calculatorNames.size();
                expectedReplies = numAgents;

                if (numAgents == 0) {
                    sendFailure("Нет доступных вычислителей");
                    isProcessing = false;
                    return;
                }

                logger.info("👥 Найдено вычислителей: " + numAgents);

                int rangeSize = end - start + 1;
                int chunkSize = rangeSize / numAgents;
                int remainder = rangeSize % numAgents;

                int currentStart = start;
                for (String name : calculatorNames) {
                    int currentEnd = currentStart + chunkSize - 1;
                    if (remainder > 0) {
                        currentEnd++;
                        remainder--;
                    }

                    ACLMessage task = new ACLMessage(ACLMessage.REQUEST);
                    task.addReceiver(getAID(name));
                    task.setContent(currentStart + ", " + currentEnd);
                    send(task);

                    logger.info("📤 Задача отправлена: " + name + ": " + currentStart + " → " + currentEnd);
                    currentStart = currentEnd + 1;
                }

                logger.info("⏳ Ожидание ответов от " + numAgents + " вычислителей...");
            } catch (Exception e) {
                sendFailure("Ошибка: " + e.getMessage());
                isProcessing = false;
            }
        }

        private void handleReply(ACLMessage reply) {
            try {
                int sum = Integer.parseInt(reply.getContent());
                totalSum += sum;
                receivedReplies++;
                logger.info("✅ Получен ответ от " + reply.getSender().getLocalName() +
                        ": " + sum + " (накоплено: " + totalSum + ")");
            } catch (Exception e) {
                logger.warning("⚠️ Ошибка парсинга ответа: " + e.getMessage());
            }

            if (receivedReplies >= expectedReplies) {
                ACLMessage result = originalRequest.createReply();
                result.setPerformative(ACLMessage.INFORM);
                result.setContent("Итоговая сумма: " + totalSum);
                send(result);
                logger.info("📤 Итог отправлен клиенту: " + totalSum);

                isProcessing = false;
                originalRequest = null;
                logger.info("🔄 Координатор освобождён. Готов к новым запросам.");
            }
        }

        private void sendFailure(String content) {
            ACLMessage failure = originalRequest.createReply();
            failure.setPerformative(ACLMessage.FAILURE);
            failure.setContent(content);
            send(failure);
            logger.severe("❌ Ошибка: " + content);
        }
    }

    private class UpdateCalculatorListBehaviour extends TickerBehaviour {
        public UpdateCalculatorListBehaviour() {
            super(Coordinator.this, 2000);
        }

        @Override
        protected void onTick() {
            try {
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("calculator");
                template.addServices(sd);

                DFAgentDescription[] agents = DFService.search(myAgent, template);
                List<String> newNames = new ArrayList<>();
                for (DFAgentDescription a : agents) {
                    newNames.add(a.getName().getLocalName());
                }

                if (!newNames.equals(calculatorNames)) {
                    logger.info("🔄 Обновлён список вычислителей: " + newNames);
                    calculatorNames = newNames;
                }
            } catch (FIPAException e) {
                logger.severe("❌ Ошибка поиска вычислителей: " + e.getMessage());
            }
        }
    }
}