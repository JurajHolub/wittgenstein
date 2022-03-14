package net.consensys.wittgenstein.protocols.harmony.output;

import java.util.ArrayList;
import java.util.List;

public class OutputInfo {
    public String leaders;
    public List<String> epochs = new ArrayList<>();
    public List<String> stake = new ArrayList<>();

    public String getLeaders() {
        return leaders;
    }

    public void setLeaders(String leaders) {
        this.leaders = leaders;
    }

    public List<String> getEpochs() {
        return epochs;
    }

    public void setEpochs(List<String> epochs) {
        this.epochs = epochs;
    }

    public List<String> getStake() {
        return stake;
    }

    public void setStake(List<String> stake) {
        this.stake = stake;
    }
}
