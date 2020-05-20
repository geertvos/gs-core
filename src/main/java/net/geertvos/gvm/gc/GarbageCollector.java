package net.geertvos.gvm.gc;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import net.geertvos.gvm.core.GVMObject;
import net.geertvos.gvm.core.GVMThread;
import net.geertvos.gvm.core.Value;
import net.geertvos.gvm.program.GVMHeap;


public interface GarbageCollector {

	void collect(GVMHeap heap, List<GVMThread> threads);

}
