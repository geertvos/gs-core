package net.geertvos.gvm.bridge;

import java.lang.reflect.Proxy;

import net.geertvos.gvm.core.GVMObject;
import net.geertvos.gvm.core.Value;
import net.geertvos.gvm.core.Value.TYPE;
import net.geertvos.gvm.program.GVMContext;

public class ValueConverter {

	private GVMContext context;
	
	public ValueConverter(GVMContext context) {
		this.context = context;
	}
	
	public Object convertFromGVM(Value value) {
		TYPE type = value.getType();
		if(type == TYPE.STRING) {
			return context.getProgram().getString(value.getValue());
		} else if(type == TYPE.NUMBER) {
			return value.getValue();
		} else if(type == TYPE.BOOLEAN) {
			return value.getValue() > 0;
		} else if(type == TYPE.OBJECT) {
			int objectId = value.getValue();
			Object backingObject = context.getHeap().getObject(objectId);
			if(backingObject instanceof NativeObjectWrapper) {
				return ((NativeObjectWrapper)backingObject).getObject();
			}
			return backingObject;
		} else {
			throw new RuntimeException("Argument type "+type+" not supported.");
		}
	}

	public Object convertFromGVM(Value value, Class convertTo) {
		TYPE type = value.getType();
		if(type == TYPE.STRING && convertTo == String.class) {
			return context.getProgram().getString(value.getValue());
		} else if(type == TYPE.NUMBER && convertTo == Integer.class || convertTo == int.class) {
			return value.getValue();
		} else if(type == TYPE.BOOLEAN && convertTo == Boolean.class || convertTo == boolean.class) {
			return value.getValue() > 0;
		} else if(type == TYPE.OBJECT) {
			int objectId = value.getValue();
			Object backingObject = context.getHeap().getObject(objectId);
			if(backingObject instanceof NativeObjectWrapper) {
				Object backing = ((NativeObjectWrapper)backingObject).getObject();
				if(backing.getClass() == convertTo) {
					return backing;
				} else {
					throw new RuntimeException("Request type conversino to "+convertTo+" not supported, backing type is: "+backing.getClass());
				}
			}
			Object proxyInstance = createProxyObject(value, convertTo);
			return proxyInstance;
		} else {
			throw new RuntimeException("Argument type "+type+" not supported.");
		}
	}

	private Object createProxyObject(Value value, Class convertTo) {
		Object proxyInstance = Proxy.newProxyInstance(
				  convertTo.getClassLoader(), 
				  new Class[] { convertTo }, 
				  new GvmToNativeOjectWrapper(context, value));
		return proxyInstance;
	}
	

	
	public Value convertToGVM(Object returnValue) {
		if(returnValue == null) {
			return new Value(0,  Value.TYPE.UNDEFINED);
		}
		else if(returnValue instanceof String) {
			String strVal = (String)returnValue;
			int index = context.getProgram().addString(strVal);
			return new Value(index, TYPE.STRING);
		}
		else if(returnValue instanceof Integer) {
			return new Value(((Integer)returnValue), TYPE.NUMBER);
		}
		else if(returnValue instanceof Boolean) {
			return new Value(((Boolean)returnValue)?1:0, TYPE.BOOLEAN);
		}
		else if(returnValue instanceof GVMObject) {
			int index = context.getHeap().addObject((GVMObject)returnValue);
			return new Value(index, TYPE.OBJECT);
		} else {
			NativeObjectWrapper wrapper = new NativeObjectWrapper(returnValue, context);
			int index = context.getHeap().addObject(wrapper);
			return new Value(index, TYPE.OBJECT);
		}

	}
	
}
