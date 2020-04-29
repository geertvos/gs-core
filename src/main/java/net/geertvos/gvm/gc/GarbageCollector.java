package net.geertvos.gvm.gc;
import java.util.Map;
import java.util.Stack;

import net.geertvos.gvm.core.GVMObject;
import net.geertvos.gvm.core.Value;


public interface GarbageCollector {

	void collect(Map<Integer, GVMObject> heap, Stack<Value> stack);

}
