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
    private static final String HOST = "localhost";
    private static final int PORT = 1099;

    public static void main(String[] args) {
        try {
            logger.info("Starting Distributed Calculation System...");

            // Main container
            Runtime runtime = Runtime.instance();
            Profile mainProfile = new ProfileImpl();
            mainProfile.setParameter(Profile.MAIN, "true");
            mainProfile.setParameter(Profile.MAIN_HOST, HOST);
            mainProfile.setParameter(Profile.MAIN_PORT, String.valueOf(PORT));
            mainProfile.setParameter(Profile.GUI, "true");

            AgentContainer mainContainer = runtime.createMainContainer(mainProfile);
            logger.info("Main container started on " + HOST + ":" + PORT);

            // Agent container
            Profile agentProfile = new ProfileImpl();
            agentProfile.setParameter(Profile.MAIN_HOST, HOST);
            agentProfile.setParameter(Profile.MAIN_PORT, String.valueOf(PORT));
            agentProfile.setParameter(Profile.CONTAINER_NAME, "AgentContainer");

            AgentContainer agentContainer = runtime.createAgentContainer(agentProfile);
            logger.info("Agent container created and connected to main platform");

            AgentController client = agentContainer.createNewAgent("client", AgentClient.class.getName(), null);
            client.start();
            logger.info("Client agent started in agent container");

            for (int i = 1; i <= 3; i++) {
                String agentName = "coordinator" + i;
                AgentController coordinator = agentContainer.createNewAgent(
                        agentName, AgentCoordinator.class.getName(), null
                );
                coordinator.start();
                logger.info("Coordinator agent started: " + agentName);
            }

            for (int i = 1; i <= 3; i++) {
                String agentName = "calculator" + i;
                AgentController calculator = agentContainer.createNewAgent(
                        agentName, AgentCalculator.class.getName(), null
                );
                calculator.start();
                logger.info("Calculator agent started: " + agentName);
            }

            logger.info("All agents started successfully in agent container!");
            System.out.println("Distributed Calculation System is running!");

            synchronized (Main.class) {
                Main.class.wait();
            }

        } catch (StaleProxyException e) {
            logger.log(Logger.SEVERE, "Failed to start agents", e);
            System.err.println("Error starting agents: " + e.getMessage());
        } catch (Exception e) {
            logger.log(Logger.SEVERE, "System initialization failed", e);
            System.err.println("System initialization failed: " + e.getMessage());
        }
    }
}