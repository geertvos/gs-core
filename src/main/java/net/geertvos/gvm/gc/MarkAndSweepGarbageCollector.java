package net.geertvos.gvm.gc;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.geertvos.gvm.core.GVMObject;
import net.geertvos.gvm.core.GVMThread;
import net.geertvos.gvm.core.StackFrame;
import net.geertvos.gvm.core.Type.Operations;
import net.geertvos.gvm.core.Value;
import net.geertvos.gvm.program.GVMHeap;

public class MarkAndSweepGarbageCollector implements GarbageCollector {

	private int currentHeapSizeTreshHold = 200;
	
	@Override
	public void collect(GVMHeap heap, Collection<GVMThread> threads) {
		if( heap.size() < currentHeapSizeTreshHold )
			return;
		long start = System.currentTimeMillis();
		Set<GVMObject> alive = new HashSet<GVMObject>();
		for(GVMThread thread : threads) {
			for(StackFrame frame : thread.getCallStack()) {
				Value v = frame.getScope();
				if( v.getType().supportsOperation(Operations.GET))
				{
					GVMObject child = heap.getObject(v.getValue());
					if(child != null) {
						search( child , alive, heap);
					}
				}
			}
			for( Value v : thread.getStack() )
			{
				if(!alive.contains(v)) {
					if( v.getType().supportsOperation(Operations.GET))
					{
						GVMObject child = heap.getObject(v.getValue());
						if(child != null) {
							search( child , alive, heap);
						}
					}
				}
			}
		}
		heap.retain(alive);
		while( heap.size() > currentHeapSizeTreshHold )
			currentHeapSizeTreshHold = currentHeapSizeTreshHold*2;
		System.err.println("Running GC completed");
		long end = System.currentTimeMillis();
		System.err.println("Took "+(end-start)+"ms");
	}
	
	private void search( GVMObject o , Set<GVMObject> alive, GVMHeap heap )
	{
		if( !alive.contains(o) )
		{
			alive.add(o);
			for( Value v : o.getValues() )
			{
				if( v.getType().supportsOperation(Operations.GET))
				{
					GVMObject child = heap.getObject(v.getValue());
					if(child != null) {
						search( child , alive, heap);
					}
				}
			}
		}
	}

}
