package net.geertvos.gvm.core;


import java.util.Collection;

public interface GVMObject {

	void setValue(String id, Value v);

	Value getValue(String id);

	Collection<Value> getValues();

}
