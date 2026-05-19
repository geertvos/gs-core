package net.geertvos.gvm.bridge;

public class MethodDescriptor {

    private final String name;
    private final int argCount;

    public MethodDescriptor(String name, int argCount) {
        this.name = name;
        this.argCount = argCount;
    }

    public String getName() {
        return name;
    }

    public int getArgCount() {
        return argCount;
    }
}
