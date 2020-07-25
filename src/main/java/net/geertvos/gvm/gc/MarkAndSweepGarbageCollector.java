package net.geertvos.gvm.gc;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.geertvos.gvm.core.GVMObject;
import net.geertvos.gvm.core.GVMThread;
import net.geertvos.gvm.core.Type.Operations;
import net.geertvos.gvm.core.Value;
import net.geertvos.gvm.program.GVMHeap;

public class MarkAndSweepGarbageCollector implements GarbageCollector {

	private int currentHeapSizeTreshHold = 200;
	
	@Override
	public void collect(GVMHeap heap, Collection<GVMThread> threads) {
		if( heap.size() < currentHeapSizeTreshHold )
			return;
		Set<GVMObject> alive = new HashSet<GVMObject>();
		for(GVMThread thread : threads) {
			for( Value v : thread.getStack() )
			{
				if( v.getType().supportsOperation(Operations.GET))
				{
					//TODO: fix, Strings are not on the heap yet
					GVMObject child = heap.getObject(v.getValue());
					if(child != null) {
						search( child , alive, heap);
					}
				}
			}
		}
		heap.retain(alive);
		while( heap.size() > currentHeapSizeTreshHold )
			currentHeapSizeTreshHold = currentHeapSizeTreshHold*2;
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
