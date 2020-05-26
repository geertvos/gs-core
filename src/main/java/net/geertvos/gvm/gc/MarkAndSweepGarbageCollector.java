package net.geertvos.gvm.gc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.geertvos.gvm.core.GVMObject;
import net.geertvos.gvm.core.GVMThread;
import net.geertvos.gvm.core.Value;
import net.geertvos.gvm.program.GVMHeap;

public class MarkAndSweepGarbageCollector implements GarbageCollector {

	private int currentHeapSizeTreshHold = 20;
	
	@Override
	public void collect(GVMHeap heap, List<GVMThread> threads) {
		if( heap.size() < currentHeapSizeTreshHold )
			return;
		
		//Build list of live objects
		Set<GVMObject> alive = new HashSet<GVMObject>();
		for(GVMThread thread : threads) {
			for( Value v : thread.getStack() )
			{
				if( v.getType().equals(Value.TYPE.OBJECT))
				{
					GVMObject child = heap.getObject(v.getValue());
					search( child , alive, heap);
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
				if( v.getType().equals(Value.TYPE.OBJECT))
				{
					GVMObject child = heap.getObject(v.getValue());
					search( child , alive, heap);
				}
			}
		}
	}

}
