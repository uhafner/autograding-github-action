package de.tobiasmichael.me.Util;

public class JacocoCounter {
    private float missed;
    private float covered;
    private String type;

    JacocoCounter() {
    }

    JacocoCounter(String type, int missed, int covered) {
    }

    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public float getMissed() {
        return missed;
    }

    public void setMissed(float missed) {
        this.missed = missed;
    }

    public float getCovered() {
        return covered;
    }

    public void setCovered(float covered) {
        this.covered = covered;
    }

    @Override
    public String toString() {
        return "JacocoCounter{" +
                "missed=" + missed +
                ", covered=" + covered +
                ", type='" + type + '\'' +
                '}';
    }
}
