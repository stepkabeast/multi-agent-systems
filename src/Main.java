import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import main.CalculatorAgent;
import main.Coordinator;
import main.ClientAgent;

public class Main {
    public static void main(String[] args) throws ControllerException, InterruptedException {
        Runtime rt = Runtime.instance();

        Profile p = new ProfileImpl();
        p.setParameter(Profile.MAIN_PORT, "1199");

        AgentContainer container = rt.createMainContainer(p);
        if (container == null) {
            System.err.println("Ошибка: не удалось создать контейнер.");
            return;
        }

        container.start();

        AgentController coordinator = container.createNewAgent("coordinator", Coordinator.class.getName(), new Object[]{});
        AgentController calc1 = container.createNewAgent("calc1", CalculatorAgent.class.getName(), new Object[]{});
        AgentController calc2 = container.createNewAgent("calc2", CalculatorAgent.class.getName(), new Object[]{});
        AgentController calc3 = container.createNewAgent("calc3", CalculatorAgent.class.getName(), new Object[]{});

        coordinator.start();
        calc1.start();
        calc2.start();
        calc3.start();

        Thread.sleep(5000);

        sendRequest(container, "coordinator", "1, 100", "client1");
        Thread.sleep(100);
        sendRequest(container, "coordinator", "1, 50", "client2");

        Thread.sleep(15000);
        System.exit(0);
    }

    private static void sendRequest(AgentContainer container, String receiver, String content, String clientName) throws ControllerException {
        Object[] args = {receiver, content};
        AgentController client = container.createNewAgent(clientName, ClientAgent.class.getName(), args);
        client.start();
    }
}