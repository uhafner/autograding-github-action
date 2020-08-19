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

    public int getInstructionPercentage() {
        return Math.round(instruction);
    }

    public int getBranchPercentage() {
        return Math.round(branch);
    }

    public int getLinePercentage() {
        return Math.round(line);
    }

    public int getComplexityPercentage() {
        return Math.round(complexity);
    }

    public int getMethodPercentage() {
        return Math.round(method);
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