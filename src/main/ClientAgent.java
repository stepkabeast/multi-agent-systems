package main;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class ClientAgent extends Agent {
    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args.length < 2) {
            System.err.println("❌ Ошибка: укажите получателя и содержимое");
            return;
        }

        String receiverName = (String) args[0];
        String content = (String) args[1];

        System.out.println("Клиент " + getLocalName() + " отправил: " + content + " → " + receiverName);

        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                AID receiverAID = new AID(receiverName, AID.ISLOCALNAME);

                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.addReceiver(receiverAID);
                msg.setContent(content);
                send(msg);

                MessageTemplate template = MessageTemplate.or(
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                        MessageTemplate.MatchPerformative(ACLMessage.REFUSE)
                );

                ACLMessage reply = myAgent.blockingReceive(template, 10000);
                if (reply != null) {
                    if (reply.getPerformative() == ACLMessage.INFORM) {
                        System.out.println("📩 Ответ для " + getLocalName() + ": " + reply.getContent());
                    } else if (reply.getPerformative() == ACLMessage.REFUSE) {
                        System.out.println("🚫 Запрос отклонён: " + reply.getContent());
                    }
                } else {
                    System.out.println("❌ Ответ не получен (таймаут)");
                }
            }
        });
    }
}