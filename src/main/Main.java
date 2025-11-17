package main;

import main.agents.calculator.AgentCalculator;
import main.agents.client.AgentClient;
import main.agents.coordinator.AgentCoordinator;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import jade.util.Logger;

public class Main {
    private static final Logger logger = Logger.getMyLogger(Main.class.getName());

    public static void main(String[] args) {
        try {
            logger.info("Starting Distributed Calculation System...");
            Runtime runtime = Runtime.instance();
            Profile profile = new ProfileImpl();
            profile.setParameter(Profile.GUI, "true");

            AgentContainer mainContainer = runtime.createMainContainer(profile);
            AgentController client = mainContainer.createNewAgent("client", AgentClient.class.getName(), null);
            client.start();
            logger.info("Client agent started");

            for (int i = 1; i <= 3; i++) {
                String agentName = "coordinator" + i;
                AgentController coordinator = mainContainer.createNewAgent(
                        agentName, AgentCoordinator.class.getName(), null
                );
                coordinator.start();
                logger.info("Coordinator agent started: " + agentName);
            }

            for (int i = 1; i <= 3; i++) {
                String agentName = "calculator" + i;
                AgentController calculator = mainContainer.createNewAgent(
                        agentName, AgentCalculator.class.getName(), null
                );
                calculator.start();
                logger.info("Calculator agent started: " + agentName);
            }

            logger.info("All agents started successfully!");
            System.out.println("Distributed Calculation System is running!");

        } catch (StaleProxyException e) {
            logger.log(Logger.SEVERE, "Failed to start agents", e);
            System.err.println("Error starting agents: " + e.getMessage());
        } catch (Exception e) {
            logger.log(Logger.SEVERE, "System initialization failed", e);
            System.err.println("System initialization failed: " + e.getMessage());
        }
    }
}