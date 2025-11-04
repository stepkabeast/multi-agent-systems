package main;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.util.*;

public class Coordinator extends Agent {
    private Logger logger = Logger.getMyLogger(getClass().getName());
    private List<String> calculatorNames = new ArrayList<>();
    private boolean isProcessing = false;

    @Override
    protected void setup() {
        logger.info("Координатор " + getLocalName() + " создан.");
        System.out.println("Hello! Coordinator Agent " + getAID().getName() + " is ready.");

        addBehaviour(new UpdateCalculatorListBehaviour());
        addBehaviour(new MainBehaviour());
    }

    private class MainBehaviour extends Behaviour {
        private final MessageTemplate requestTemplate = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
        private final MessageTemplate confirmTemplate = MessageTemplate.MatchPerformative(ACLMessage.CONFIRM);

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

                logger.info("✅ Начинаем обработку запроса");
                isProcessing = true;

                try {
                    String[] parts = msg.getContent().split(",");
                    int start = Integer.parseInt(parts[0].trim());
                    int end = Integer.parseInt(parts[1].trim());

                    logger.info("🔢 Диапазон: " + start + " - " + end);

                    int rangeSize = end - start + 1;
                    int numAgents = calculatorNames.size();

                    if (numAgents == 0) {
                        ACLMessage failure = msg.createReply();
                        failure.setPerformative(ACLMessage.FAILURE);
                        failure.setContent("Нет доступных вычислителей");
                        send(failure);
                        logger.severe("❌ Ошибка: нет доступных вычислителей");
                        isProcessing = false;
                        return;
                    }

                    logger.info("👥 Найдено вычислителей: " + numAgents);

                    int chunkSize = rangeSize / numAgents;
                    int remainder = rangeSize % numAgents;

                    int currentStart = start;
                    List<String> assigned = new ArrayList<>();

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

                        String assignment = name + ": " + currentStart + " → " + currentEnd;
                        assigned.add(assignment);
                        logger.info("📤 Задача отправлена: " + assignment);

                        currentStart = currentEnd + 1;
                    }

                    logger.info("⏳ Ожидание ответов от " + numAgents + " вычислителей...");

                    int totalSum = 0;
                    int received = 0;

                    for (int i = 0; i < numAgents; i++) {
                        ACLMessage reply = myAgent.blockingReceive(confirmTemplate, 5000);
                        if (reply != null) {
                            try {
                                int sum = Integer.parseInt(reply.getContent());
                                totalSum += sum;
                                received++;
                                logger.info("✅ Получен ответ от " + reply.getSender().getLocalName() +
                                        ": " + sum + " (накоплено: " + totalSum + ")");
                            } catch (Exception e) {
                                logger.warning("⚠️ Ошибка парсинга ответа от " +
                                        reply.getSender().getLocalName() + ": " + e.getMessage());
                            }
                        } else {
                            logger.warning("❌ Таймаут ожидания ответа от одного из вычислителей");
                        }
                    }

                    logger.info("📊 Всего получено ответов: " + received + " из " + numAgents);

                    ACLMessage result = msg.createReply();
                    result.setPerformative(ACLMessage.INFORM);
                    result.setContent("Итоговая сумма: " + totalSum);
                    send(result);
                    logger.info("📤 Итог отправлен клиенту: " + totalSum);

                } catch (Exception e) {
                    logger.severe("❌ Ошибка при обработке: " + e.getMessage());
                    ACLMessage failure = msg.createReply();
                    failure.setPerformative(ACLMessage.FAILURE);
                    failure.setContent("Ошибка: " + e.getMessage());
                    send(failure);
                } finally {
                    isProcessing = false;
                    logger.info("🔄 Координатор освобождён. Готов к новым запросам.");
                }
            } else {
                block();
            }
        }

        @Override
        public boolean done() {
            return false;
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