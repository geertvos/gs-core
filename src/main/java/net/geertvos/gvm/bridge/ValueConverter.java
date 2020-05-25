package net.geertvos.gvm.bridge;

import net.geertvos.gvm.core.GVMObject;
import net.geertvos.gvm.core.Value;
import net.geertvos.gvm.core.Value.TYPE;
import net.geertvos.gvm.program.GVMProgram;
import net.geertvos.gvm.program.GVMHeap;

public class ValueConverter {

	private GVMHeap heap; 
	private GVMProgram program;
	
	public ValueConverter(GVMHeap heap, GVMProgram program) {
		this.heap = heap;
		this.program = program;
	}
	
	public Object convertFromGVM(Value value) {
		TYPE type = value.getType();
		if(type == TYPE.STRING) {
			return program.getString(value.getValue());
		} else if(type == TYPE.NUMBER) {
			return value.getValue();
		} else if(type == TYPE.BOOLEAN) {
			return value.getValue() > 0;
		} else if(type == TYPE.OBJECT) {
			int objectId = value.getValue();
			Object backingObject = heap.getObject(objectId);
			if(backingObject instanceof NativeObjectWrapper) {
				return ((NativeObjectWrapper)backingObject).getObject();
			}
			return backingObject;
		} else {
			throw new RuntimeException("Argument type "+type+" not supported.");
		}
	}
	
	public Value convertToGVM(Object returnValue) {
		if(returnValue == null) {
			return new Value(0,  Value.TYPE.UNDEFINED);
		}
		else if(returnValue instanceof String) {
			String strVal = (String)returnValue;
			int index = program.addString(strVal);
			return new Value(index, TYPE.STRING);
		}
		else if(returnValue instanceof Integer) {
			return new Value(((Integer)returnValue), TYPE.NUMBER);
		}
		else if(returnValue instanceof Boolean) {
			return new Value(((Boolean)returnValue)?1:0, TYPE.BOOLEAN);
		}
		else if(returnValue instanceof GVMObject) {
			int index = heap.addObject((GVMObject)returnValue);
			return new Value(index, TYPE.OBJECT);
		} else {
			NativeObjectWrapper wrapper = new NativeObjectWrapper(returnValue, heap, program);
			int index = heap.addObject(wrapper);
			return new Value(index, TYPE.OBJECT);
		}

	}
	
}
