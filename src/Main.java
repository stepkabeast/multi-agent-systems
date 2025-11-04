    import jade.core.AID;
    import jade.core.Runtime;
    import jade.lang.acl.ACLMessage;
    import jade.wrapper.AgentContainer;
    import jade.core.Profile;
    import jade.core.ProfileImpl;
    import jade.wrapper.AgentController;
    import jade.wrapper.ControllerException;
    import main.NodeAgent;
    import main.SampleAgent;

    public class Main {
        public static void main(String[] args) throws ControllerException, InterruptedException {
            Runtime rt = Runtime.instance();
            Profile p = new ProfileImpl();
            p.setParameter(Profile.GUI, "true");
            AgentContainer container = rt.createMainContainer(p);
            container.start();

            AgentController node1 = container.createNewAgent("a", NodeAgent.class.getName(), new Object[]{"b"});
            node1.start();
            AgentController node2 = container.createNewAgent("b", NodeAgent.class.getName(), new Object[]{"a", "c"});
            node2.start();
            AgentController node3 = container.createNewAgent("c", NodeAgent.class.getName(), new Object[]{"b","d"});
            node3.start();
            AgentController node4 = container.createNewAgent("d", NodeAgent.class.getName(), new Object[]{"c"});
            node4.start();


//            AgentController sampleAgent = container.createNewAgent("sampleAgent", SampleAgent.class.getName(), new Object[]{});
//            sampleAgent.start();
        }
    }