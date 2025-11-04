package main;

import jade.core.Agent;
import jade.lang.acl.ACLMessage;

public class ClientAgent extends Agent {
    private String receiver;
    private String content;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length == 2) {
            receiver = (String) args[0];
            content = (String) args[1];
        } else {
            System.err.println("ClientAgent: требуется имя получателя и содержимое");
            doDelete();
            return;
        }

        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
        request.addReceiver(getAID(receiver));
        request.setContent(content);
        send(request);

        System.out.println("Клиент " + getLocalName() + " отправил: " + content + " → " + receiver);

        doDelete();
    }
}