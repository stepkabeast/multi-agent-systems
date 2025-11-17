package main.agents.coordinator;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.Set;

public class CalculationTask {
    private final ACLMessage originalAccept;
    private final Set<AID> calculators;

    public CalculationTask(ACLMessage originalAccept, Set<AID> calculators) {
        this.originalAccept = originalAccept;
        this.calculators = calculators;
    }

    public ACLMessage getOriginalAccept() {
        return originalAccept;
    }

    public Set<AID> getCalculators() {
        return calculators;
    }

    public int getCalculatorCount() {
        return calculators.size();
    }
}