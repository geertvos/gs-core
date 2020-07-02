package net.geertvos.gvm.gc;
import java.util.Collection;

import net.geertvos.gvm.core.GVMThread;
import net.geertvos.gvm.program.GVMHeap;


public interface GarbageCollector {

	void collect(GVMHeap heap, Collection<GVMThread> threads);

}
