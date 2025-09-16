package main;
import jade.core.Agent;
import jade.util.Logger;


public class SampleAgent extends Agent {

    Logger logger = Logger.getMyLogger(getClass().getName());

    @Override
    protected void setup() {
        System.out.println("Hello! Agent " + getAID().getName() + " is ready.");
    }


}