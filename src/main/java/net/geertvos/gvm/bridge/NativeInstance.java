package net.geertvos.gvm.bridge;

import java.util.List;

public interface NativeInstance {

    String typeName();

    List<MethodDescriptor> instanceMethods();

    NativeValue callMethod(String method, List<NativeValue> args) throws NativeError;

    void destroy();

    NativeInstance cloneInstance();
}
