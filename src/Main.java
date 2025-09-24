//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import main.CalculatorAgent;
import main.SampleAgent;
import main.Coordinator;


public class Main {
    public static void main(String[] args) throws StaleProxyException {
        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        // to see how IntelliJ IDEA suggests fixing it.
        //System.out.printf("Hello and welcome!");
        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();
        // p.setParameter(Profile.MAIN_HOST, "localhost");
        // p.setParameter(Profile.MAIN_PORT, "10099");
        p.setParameter(Profile.GUI, "true");
        AgentContainer mainContainer = rt.createMainContainer(p);


        AgentController agent = mainContainer.createNewAgent("sample-agent", SampleAgent.class.getName(), null);
        agent.start();

        /*String[] dummyNames = {"dummy1", "dummy2"};
        for (String name : dummyNames) {
            AgentController dummyAgent = mainContainer.createNewAgent(name, SampleAgent.class.getName(), null);
            dummyAgent.start();
        }*/
        // Создание вычислителей
        String[] calculatorNames = {"calc1", "calc2", "calc3"};
        for (String name : calculatorNames) {
            AgentController calculator = mainContainer.createNewAgent(name, CalculatorAgent.class.getName(), null);
            calculator.start();
        }

        // Создание координатора с передачей имен вычислителей
        Object[] argsForCoordinator = {calculatorNames};
        AgentController coordinator = mainContainer.createNewAgent("coordinator", Coordinator.class.getName(), argsForCoordinator);
        coordinator.start();
    }
}