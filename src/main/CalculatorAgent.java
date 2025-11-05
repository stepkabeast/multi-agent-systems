package main;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CalculatorAgent extends Agent {
    private Logger logger = Logger.getMyLogger(getClass().getName());
    private ExecutorService executorService;

    @Override
    protected void setup() {
        logger.info("Агент-вычислитель " + getLocalName() + " создан.");
        System.out.println("Hello! Calculator Agent " + getAID().getName() + " is ready.");

        // Создаем пул потоков для параллельных вычислений
        executorService = Executors.newCachedThreadPool();

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
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null && msg.getPerformative() == ACLMessage.REQUEST) {
                logger.info("📥 Получен запрос от " + msg.getSender().getLocalName() +
                        ": " + msg.getContent());

                // Запускаем вычисление в отдельном потоке
                executorService.execute(new CalculationTask(msg));
            } else {
                block();
            }
        }
    }

    private class CalculationTask implements Runnable {
        private final ACLMessage request;

        public CalculationTask(ACLMessage request) {
            this.request = request;
        }

        @Override
        public void run() {
            try {
                logger.info("⏳ " + getLocalName() + " начал вычисление в отдельном потоке...");

                String[] parts = request.getContent().split(",");
                int start = Integer.parseInt(parts[0].trim());
                int end = Integer.parseInt(parts[1].trim());

                // Имитация длительного вычисления
                Thread.sleep(5000);

                int sum = 0;
                for (int i = Math.min(start, end); i <= Math.max(start, end); i++) {
                    sum += i;
                }

                ACLMessage reply = request.createReply();
                reply.setPerformative(ACLMessage.CONFIRM);
                reply.setContent(String.valueOf(sum));
                send(reply);

                logger.info("📤 Ответ отправлен координатору " +
                        request.getSender().getLocalName() + ": " + sum);

            } catch (Exception e) {
                ACLMessage reply = request.createReply();
                reply.setPerformative(ACLMessage.FAILURE);
                reply.setContent("Ошибка: " + e.getMessage());
                send(reply);
                logger.warning("❌ Ошибка обработки: " + e.getMessage());
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
        // Завершаем пул потоков при остановке агента
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}