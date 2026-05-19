package net.geertvos.gvm.bridge;

import java.util.List;

public interface NativeModule {

    String className();

    NativeValue constructor(List<NativeValue> args) throws NativeError;

    NativeValue callStatic(String method, List<NativeValue> args) throws NativeError;

    List<MethodDescriptor> staticMethods();
}
