package net.geertvos.gvm.program;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.geertvos.gvm.core.GVMObject;

public class GVMHeap {

	private Map<Object, GVMObject> heap = new HashMap<Object, GVMObject>(); 
	private AtomicInteger objectIdCounter = new AtomicInteger(-1);
	
	public Integer addObject(GVMObject object) {
		int id = objectIdCounter.incrementAndGet();
		heap.put(id, object);
		return id;
	}
	
	public GVMObject getObject(Object key) {
		return heap.get(key);
	}

	public void clear() {
		this.heap.clear();
	}
	
	public int size() {
		return heap.size();
	}
	
	public void retain(Collection<GVMObject> objects) {
		for(GVMObject object : heap.values()) {
			if(!objects.contains(object)) {
				object.preDestroy();
			}
		}
		this.heap.values().retainAll(objects);
	}
}
