package net.geertvos.gvm.bridge;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NativeRegistry {

    private final Map<String, NativeModule> modules = new HashMap<>();

    public void register(NativeModule module) {
        modules.put(module.className(), module);
    }

    public NativeValue dispatch(String className, String method, List<NativeValue> args) throws NativeError {
        NativeModule module = modules.get(className);
        if (module == null) {
            throw new NativeError("Native module not found: " + className);
        }
        String simpleName = className.contains(".")
                ? className.substring(className.lastIndexOf('.') + 1)
                : className;
        if (method.equals(simpleName)) {
            return module.constructor(args);
        } else {
            return module.callStatic(method, args);
        }
    }

    public Set<String> registeredClasses() {
        return modules.keySet();
    }
}
