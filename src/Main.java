    //TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
    // click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
    import jade.core.Runtime;
    import jade.wrapper.AgentContainer;
    import jade.core.Profile;
    import jade.core.ProfileImpl;
    import jade.wrapper.AgentController;
    import jade.wrapper.StaleProxyException;
    import main.NodeAgent;

    public class Main {
        public static void main(String[] args) throws StaleProxyException, InterruptedException {
            Runtime rt = Runtime.instance();
            Profile p = new ProfileImpl();
            p.setParameter(Profile.GUI, "true");
            AgentContainer mainContainer = rt.createMainContainer(p);

//            AgentController node1 = mainContainer.createNewAgent("node1", NodeAgent.class.getName(), new String[]{"node2"});
//            node1.start();
//            AgentController node2 = mainContainer.createNewAgent("node2", NodeAgent.class.getName(), new String[]{"node1", "node4"});
//            node2.start();
//            AgentController node3 = mainContainer.createNewAgent("node3", NodeAgent.class.getName(), new String[]{"node4"});
//            node3.start();
//            AgentController node4 = mainContainer.createNewAgent("node4", NodeAgent.class.getName(), new String[]{"node3", "node2"});
//            node4.start();

        }
    }