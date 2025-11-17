import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import main.NodeAgent;
import jade.util.Logger;

public class Main {
    private static final Logger logger = Logger.getMyLogger(Main.class.getName());
    private static final String HOST = "localhost";
    private static final int PORT = 1099;

    public static void main(String[] args) throws ControllerException, InterruptedException {
        Runtime runtime = Runtime.instance();

        try {
            logger.info("Starting main container...");

            // Main container
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
            logger.info("Agent container created and connected to platform");

            AgentController node1 = agentContainer.createNewAgent("a", NodeAgent.class.getName(), new Object[]{"b"});
            node1.start();
            logger.info("Agent a started");

            AgentController node2 = agentContainer.createNewAgent("b", NodeAgent.class.getName(), new Object[]{"a", "c"});
            node2.start();
            logger.info("Agent b started");

            AgentController node3 = agentContainer.createNewAgent("c", NodeAgent.class.getName(), new Object[]{"b", "d"});
            node3.start();
            logger.info("Agent c started");

            AgentController node4 = agentContainer.createNewAgent("d", NodeAgent.class.getName(), new Object[]{"c", "a"});
            node4.start();
            logger.info("Agent d started");

            logger.info("All agents successfully started in the agent container!");

            synchronized (Main.class) {
                Main.class.wait();
            }

        } catch (Exception e) {
            logger.severe("Error starting the system: " + e.getMessage());
            e.printStackTrace();
        }
    }
}