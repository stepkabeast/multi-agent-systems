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
        p.setParameter(Profile.GUI, "true");

        AgentContainer container = rt.createMainContainer(p);
        if (container == null) {
            System.err.println("Ошибка: не удалось создать контейнер.");
            return;
        }

        container.start();

        // Создаем несколько координаторов
        AgentController coordinator1 = container.createNewAgent("coordinator1", Coordinator.class.getName(), new Object[]{});
        AgentController coordinator2 = container.createNewAgent("coordinator2", Coordinator.class.getName(), new Object[]{});

        // Создаем несколько вычислителей
        AgentController calc1 = container.createNewAgent("calc1", CalculatorAgent.class.getName(), new Object[]{});
        AgentController calc2 = container.createNewAgent("calc2", CalculatorAgent.class.getName(), new Object[]{});
        AgentController calc3 = container.createNewAgent("calc3", CalculatorAgent.class.getName(), new Object[]{});

        // Запускаем агентов
        coordinator1.start();
        coordinator2.start();
        calc1.start();
        calc2.start();
        calc3.start();

        Thread.sleep(5000); // Ждем регистрации в DF

        // Тестируем работу с несколькими координаторами
        System.out.println("\n=== Тест 1: Один координатор ===");
        sendRequest(container, "coordinator1", "1, 100", "client1");

        Thread.sleep(2000);

        System.out.println("\n=== Тест 2: Параллельные запросы к разным координаторам ===");
        sendRequest(container, "coordinator1", "101, 200", "client2");
        sendRequest(container, "coordinator2", "201, 300", "client3");

        Thread.sleep(5000);

        System.out.println("\n=== Тест 3: Еще запросы для демонстрации параллелизма ===");
        sendRequest(container, "coordinator1", "301, 400", "client4");
        sendRequest(container, "coordinator2", "401, 500", "client5");

        // Ждем завершения всех вычислений
        Thread.sleep(20000);
        //System.out.println("\n=== Завершение работы ===");
        //System.exit(0);
    }

    private static void sendRequest(AgentContainer container, String receiver, String content, String clientName) throws ControllerException {
        Object[] args = {receiver, content};
        AgentController client = container.createNewAgent(clientName, ClientAgent.class.getName(), args);
        client.start();
    }
}