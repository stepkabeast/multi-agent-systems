package main;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;

import java.util.ArrayList;
import java.util.List;

public class NodeAgent extends Agent {
    private List<String> neighbors = new ArrayList<>();
    Logger logger = Logger.getMyLogger(getClass().getName());
    String name = "";

    @Override
    public void setup() {
        name = getLocalName();
        addBehaviour(new NodeBehaviour());
        Object[] args = getArguments();
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof String) {
                    neighbors.add((String) arg);
                }
            }
        }

        logger.log(Logger.INFO, "Agent <" + name + "> started.");
        logger.log(Logger.INFO, "ID: " + getAID());
        logger.log(Logger.INFO, "His neighbors are: " + neighbors);
    }

    private class NodeBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = receive();
            if (msg != null) {
                String receivedContent = msg.getContent();
                String performativeName = getPerformativeName(msg.getPerformative());

                logger.log(Logger.INFO, "Agent " + name + " received " + performativeName +
                        " from " + msg.getSender().getLocalName() + ": " + receivedContent);

                // Обработка REQUEST (исходный запрос)
                if (msg.getPerformative() == ACLMessage.REQUEST) {
                    String targetName = extractTarget(receivedContent);

                    // ПЕРВОЕ: проверяем, не является ли текущий агент целевым
                    if (name.equals(targetName)) {
                        // Найден целевой агент - формируем цепочку и отправляем CONFIRM обратно
                        logger.log(Logger.INFO, "Agent " + name + " is the target! Sending CONFIRM back.");

                        String chain = extractChain(receivedContent);
                        chain += (chain.isEmpty() ? name : " -> " + name);

                        ACLMessage confirmMsg = new ACLMessage(ACLMessage.CONFIRM);
                        confirmMsg.setContent("FOUND|" + chain + "|Target " + name + " found");
                        confirmMsg.addReceiver(msg.getSender());
                        send(confirmMsg);
                    } else {
                        // Не целевой агент - пытаемся переслать дальше
                        boolean forwarded = false;
                        for (String neighbor : neighbors) {
                            if (!neighbor.equals(msg.getSender().getLocalName())) {
                                ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);

                                // Добавляем текущего агента в цепочку
                                String newContent = addToChain(receivedContent, name);
                                newMsg.setContent(newContent);
                                newMsg.addReceiver(new AID(neighbor, AID.ISLOCALNAME));
                                send(newMsg);
                                forwarded = true;
                                logger.log(Logger.INFO, "Agent " + name + " forwarded REQUEST to " + neighbor);
                            }
                        }

                        // ВТОРОЕ: если некуда перенаправлять, проверяем еще раз - может мы и есть цель?
                        if (!forwarded) {
                            if (name.equals(targetName)) {
                                // Этот случай уже обработан выше, но для надежности
                                String chain = extractChain(receivedContent);
                                chain += (chain.isEmpty() ? name : " -> " + name);

                                ACLMessage confirmMsg = new ACLMessage(ACLMessage.CONFIRM);
                                confirmMsg.setContent("FOUND|" + chain + "|Target " + name + " found");
                                confirmMsg.addReceiver(msg.getSender());
                                send(confirmMsg);
                                logger.log(Logger.INFO, "Agent " + name + " is the target (dead end)! Sending CONFIRM back.");
                            } else {
                                // Действительно тупик и мы не цель - отправляем FAILURE
                                String chain = extractChain(receivedContent);
                                chain += (chain.isEmpty() ? name : " -> " + name);

                                ACLMessage failureMsg = new ACLMessage(ACLMessage.FAILURE);
                                failureMsg.setContent("FAILED|" + chain + "|Target " + targetName + " not found - dead end at " + name);
                                failureMsg.addReceiver(msg.getSender());
                                send(failureMsg);
                                logger.log(Logger.INFO, "Agent " + name + " reached dead end. Sending FAILURE back.");
                            }
                        }
                    }
                }
                // Обработка CONFIRM (ответ найден)
                else if (msg.getPerformative() == ACLMessage.CONFIRM) {
                    // Пересылаем CONFIRM обратно по цепочке
                    String[] parts = msg.getContent().split("\\|", 3);
                    if (parts.length >= 3) {
                        String chain = parts[1];
                        String message = parts[2];

                        // Находим предыдущего агента в цепочке
                        String previousAgent = findPreviousAgent(chain, name);
                        if (previousAgent != null && !previousAgent.equals(name)) {
                            ACLMessage confirmMsg = new ACLMessage(ACLMessage.CONFIRM);
                            confirmMsg.setContent(msg.getContent());
                            confirmMsg.addReceiver(new AID(previousAgent, AID.ISLOCALNAME));
                            send(confirmMsg);
                            logger.log(Logger.INFO, "Agent " + name + " forwarded CONFIRM to " + previousAgent);
                        } else {
                            // Достигли исходного отправителя
                            logger.log(Logger.INFO, "Agent " + name + " received final CONFIRM. Full chain: " + chain);
                            logger.log(Logger.INFO, "Message: " + message);
                        }
                    }
                }
                // Обработка FAILURE (цель не найдена)
                else if (msg.getPerformative() == ACLMessage.FAILURE) {
                    // Пересылаем FAILURE обратно по цепочке
                    String[] parts = msg.getContent().split("\\|", 3);
                    if (parts.length >= 3) {
                        String chain = parts[1];
                        String message = parts[2];

                        // Находим предыдущего агента в цепочке
                        String previousAgent = findPreviousAgent(chain, name);
                        if (previousAgent != null && !previousAgent.equals(name)) {
                            ACLMessage failureMsg = new ACLMessage(ACLMessage.FAILURE);
                            failureMsg.setContent(msg.getContent());
                            failureMsg.addReceiver(new AID(previousAgent, AID.ISLOCALNAME));
                            send(failureMsg);
                            logger.log(Logger.INFO, "Agent " + name + " forwarded FAILURE to " + previousAgent);
                        } else {
                            // Достигли исходного отправителя
                            logger.log(Logger.INFO, "Agent " + name + " received final FAILURE. Chain: " + chain);
                            logger.log(Logger.INFO, "Error: " + message);
                        }
                    }
                }
            } else {
                block();
            }
        }

        /**
         * Извлекает имя целевого агента из содержимого
         */
        private String extractTarget(String content) {
            String[] parts = content.split("\\|", 3);
            return parts[0]; // Первая часть - всегда имя цели
        }

        /**
         * Добавляет текущего агента в цепочку
         */
        private String addToChain(String content, String currentNode) {
            String[] parts = content.split("\\|", 3);
            if (parts.length >= 2) {
                String target = parts[0];
                String chain = parts[1];
                String newChain = chain.isEmpty() ? currentNode : chain + " -> " + currentNode;
                return target + "|" + newChain + "|" + (parts.length > 2 ? parts[2] : "");
            } else {
                // Первый узел в цепочке
                return content + "|" + currentNode + "|";
            }
        }

        /**
         * Извлекает цепочку из содержимого сообщения
         */
        private String extractChain(String content) {
            String[] parts = content.split("\\|", 3);
            return parts.length >= 2 ? parts[1] : "";
        }

        /**
         * Находит предыдущего агента в цепочке
         */
        private String findPreviousAgent(String chain, String currentNode) {
            String[] nodes = chain.split(" -> ");
            for (int i = 0; i < nodes.length; i++) {
                if (nodes[i].equals(currentNode) && i > 0) {
                    return nodes[i - 1];
                }
            }
            return null;
        }

        private String getPerformativeName(int performative) {
            return switch (performative) {
                case ACLMessage.INFORM -> "INFORM";
                case ACLMessage.REQUEST -> "REQUEST";
                case ACLMessage.CONFIRM -> "CONFIRM";
                case ACLMessage.QUERY_REF -> "QUERY_REF";
                case ACLMessage.AGREE -> "AGREE";
                case ACLMessage.CANCEL -> "CANCEL";
                case ACLMessage.FAILURE -> "FAILURE";
                case ACLMessage.REFUSE -> "REFUSE";
                case ACLMessage.PROPOSE -> "PROPOSE";
                default -> "UNKNOWN(" + performative + ")";
            };
        }
    }
}