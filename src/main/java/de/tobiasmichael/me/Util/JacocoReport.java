package de.tobiasmichael.me.Util;


/**
 * Java Bean class for a errors collection of the Jacoco format.
 *
 * @author Tobias Effner
 */
public class JacocoReport {
    private float instruction;
    private float branch;
    private float line;
    private float complexity;
    private float method;

    JacocoReport() {
    }

    JacocoReport(float branch, float line) {
        this.branch = branch;
        this.line = line;
    }

    JacocoReport(float instruction, float branch, float line, float complexity, float method) {
        this(branch, line);
        this.instruction = instruction;
        this.complexity = complexity;
        this.method = method;
    }

    public float getInstruction() {
        return instruction;
    }

    public float getBranch() {
        return branch;
    }

    public float getLine() {
        return line;
    }

    public float getComplexity() {
        return complexity;
    }

    public float getMethod() {
        return method;
    }

    @Override
    public String toString() {
        return "JacocoReport{" +
                "instruction=" + instruction +
                ", branch=" + branch +
                ", line=" + line +
                ", complexity=" + complexity +
                ", method=" + method +
                '}';
    }
}