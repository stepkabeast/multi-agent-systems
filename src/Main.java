//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import main.CalculatorAgent;
import main.NodeAgent;
import main.SampleAgent;
import main.Coordinator;


public class Main {
    public static void main(String[] args) throws StaleProxyException {
        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();
        //p.setParameter(Profile.GUI, "true");
        AgentContainer mainContainer = rt.createMainContainer(p);


        //String[] nodes = {"node1", "node2", "node3"};

        AgentController node1 = mainContainer.createNewAgent("node1", NodeAgent.class.getName(), new String[]{"node2"});
        node1.start();
        AgentController node2 = mainContainer.createNewAgent("node2", NodeAgent.class.getName(), new String[]{"node1", "node3"});
        node2.start();
        AgentController node3 = mainContainer.createNewAgent("node3", NodeAgent.class.getName(), new String[]{"node2"});
        node3.start();

        AgentController agent = mainContainer.createNewAgent("sample-agent", SampleAgent.class.getName(), null);
        agent.start();
//
//        String[] calculatorNames = {"calc1", "calc2", "calc3"};
//        for (String name : calculatorNames) {
//            AgentController calculator = mainContainer.createNewAgent(name, CalculatorAgent.class.getName(), null);
//            calculator.start();
//        }
//
//        Object[] argsForCoordinator = {calculatorNames};
//        AgentController coordinator = mainContainer.createNewAgent("coordinator", Coordinator.class.getName(), argsForCoordinator);
//        coordinator.start();
    }
}