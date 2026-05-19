package net.geertvos.gvm.bridge;

public abstract class NativeValue {

    public static final NativeValue UNDEFINED = new NativeUndefined();

    public static NativeValue number(int value) {
        return new NativeNumber(value);
    }

    public static NativeValue bool(boolean value) {
        return new NativeBoolean(value);
    }

    public static NativeValue string(String value) {
        return new NativeString(value);
    }

    public static NativeValue instance(NativeInstance instance) {
        return new NativeInstanceValue(instance);
    }

    public static NativeValue bytes(byte[] data) {
        return new NativeBytes(data);
    }

    public boolean isUndefined() { return this instanceof NativeUndefined; }
    public boolean isNumber() { return this instanceof NativeNumber; }
    public boolean isBoolean() { return this instanceof NativeBoolean; }
    public boolean isString() { return this instanceof NativeString; }
    public boolean isInstance() { return this instanceof NativeInstanceValue; }
    public boolean isBytes() { return this instanceof NativeBytes; }

    public int asNumber() { return ((NativeNumber) this).value; }
    public boolean asBoolean() { return ((NativeBoolean) this).value; }
    public String asString() { return ((NativeString) this).value; }
    public NativeInstance asInstance() { return ((NativeInstanceValue) this).instance; }
    public byte[] asBytes() { return ((NativeBytes) this).data; }

    public static class NativeUndefined extends NativeValue {
        @Override public String toString() { return "Undefined"; }
    }

    public static class NativeNumber extends NativeValue {
        public final int value;
        NativeNumber(int value) { this.value = value; }
        @Override public String toString() { return "Number(" + value + ")"; }
    }

    public static class NativeBoolean extends NativeValue {
        public final boolean value;
        NativeBoolean(boolean value) { this.value = value; }
        @Override public String toString() { return "Boolean(" + value + ")"; }
    }

    public static class NativeString extends NativeValue {
        public final String value;
        NativeString(String value) { this.value = value; }
        @Override public String toString() { return "String(" + value + ")"; }
    }

    public static class NativeInstanceValue extends NativeValue {
        public final NativeInstance instance;
        NativeInstanceValue(NativeInstance instance) { this.instance = instance; }
        @Override public String toString() { return "Instance(" + instance.typeName() + ")"; }
    }

    public static class NativeBytes extends NativeValue {
        public final byte[] data;
        NativeBytes(byte[] data) { this.data = data; }
        @Override public String toString() { return "Bytes(len=" + data.length + ")"; }
    }
}
