package main;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;
import java.util.Date;

public class ClientAgent extends Agent {
    private Logger logger = Logger.getMyLogger(getClass().getName());
    private String coordinatorName;
    private String content;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length >= 2) {
            coordinatorName = (String) args[0];
            content = (String) args[1];
        } else {
            logger.severe("❌ Не указаны параметры: coordinatorName и content");
            doDelete();
            return;
        }

        logger.info("Клиент " + getLocalName() + " создан. Координатор: " + coordinatorName);
        System.out.println("Hello! Client Agent " + getAID().getName() + " is ready.");

        addBehaviour(new RequestBehaviour());
    }

    private class RequestBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            try {
                ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                request.addReceiver(getAID(coordinatorName));
                request.setContent(content);
                request.setReplyByDate(new Date(System.currentTimeMillis() + 30000));

                logger.info("📤 Отправка запроса координатору " + coordinatorName +
                        ": " + content);
                send(request);

                // Ждем ответ
                ACLMessage reply = myAgent.blockingReceive(30000);
                if (reply != null) {
                    switch (reply.getPerformative()) {
                        case ACLMessage.INFORM:
                            logger.info("✅ Получен результат: " + reply.getContent());
                            System.out.println("🎯 Результат для клиента " + getLocalName() +
                                    ": " + reply.getContent());
                            break;
                        case ACLMessage.REFUSE:
                            logger.warning("❌ Координатор отказал: " + reply.getContent());
                            break;
                        case ACLMessage.FAILURE:
                            logger.severe("❌ Ошибка координатора: " + reply.getContent());
                            break;
                        default:
                            logger.warning("⚠️ Неизвестный ответ: " + reply.getContent());
                    }
                } else {
                    logger.severe("⏰ Таймаут ожидания ответа");
                }
            } catch (Exception e) {
                logger.severe("❌ Ошибка при отправке запроса: " + e.getMessage());
            }
        }
    }
}