package net.geertvos.gvm.core;


import java.util.Collection;

public interface GVMObject {

	void setValue(String id, Value v);

	Value getValue(String id);

	boolean hasValue(String id);
	
	Collection<Value> getValues();

	/**
	 * The GC will call this method before it will be removed from the heap.
	 */
	void preDestroy();
}
