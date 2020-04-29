package net.geertvos.gvm.program;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.geertvos.gvm.core.GVMObject;
import net.geertvos.gvm.core.Value;
import net.geertvos.gvm.core.Value.TYPE;

public class NativeMethodAutoWrapper extends NativeMethodWrapper{

	private final int args;
	
	public NativeMethodAutoWrapper(int size) {
		this.args = size;
	}

	@Override
	public Value invoke(List<Value> arguments, Map<Integer, GVMObject> heap, List<String> strings) {
		Collections.reverse(arguments);
		String classname = getStringArgument(0, arguments, strings, heap);
		String method = getStringArgument(1, arguments, strings, heap);
			try {
				Class theClass = Class.forName(classname);
				Object[] wrappedArgs = null;
				Class[] wrappedTypes = null;
				if(args>2) {
					int argcount = args-2;
					wrappedArgs = new Object[argcount];
					wrappedTypes = new Class[argcount];
					for(int i=2;i<args;i++) {
						TYPE type = arguments.get(i).getType();
						if(type == TYPE.STRING) {
							String val = getStringArgument(i, arguments, strings, heap);
							wrappedArgs[i-2] = val;
							wrappedTypes[i-2] = String.class;
						} else if(type == TYPE.NUMBER) {
							Integer val = arguments.get(i).getValue();
							wrappedArgs[i-2] = val;
							wrappedTypes[i-2] = Integer.class;
						} else if(type == TYPE.BOOLEAN) {
							Boolean val = arguments.get(i).getValue() > 0;
							wrappedArgs[i-2] = val;
							wrappedTypes[i-2] = Boolean.class;
						} else {
							throw new RuntimeException("Argument type "+type+" not supported.");
						}
					}
				}
				Method m = theClass.getMethod(method, wrappedTypes);
				Object returnValue = m.invoke(null, wrappedArgs );
				if(returnValue instanceof String) {
					String strVal = (String)returnValue;
					if(!strings.contains(returnValue)) {
						strings.add(strVal);
					}
					int index = strings.indexOf(strVal);
					return new Value(index, TYPE.STRING);
				}
				else if(returnValue instanceof Integer) {
					return new Value(((Integer)returnValue), TYPE.NUMBER);
				}
				else if(returnValue instanceof Boolean) {
					return new Value(((Boolean)returnValue)?1:0, TYPE.BOOLEAN);
				}
				else if(returnValue instanceof GVMObject) {
					int index = heap.size();
					heap.put(index, (GVMObject)returnValue);
					return new Value(index, TYPE.OBJECT);
				}
			} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		return new Value(0,  Value.TYPE.UNDEFINED);
	}
	
	public String getStringArgument(int index, List<Value> arguments, List<String> table, Map<Integer, GVMObject> heap) {
		Value v = arguments.get(index);
		if( v.getType() == Value.TYPE.STRING ) {
			return table.get(v.getValue());
		}
		if( v.getType() == Value.TYPE.NUMBER ) {
			return String.valueOf(v.getValue());
		}
		if( v.getType() == Value.TYPE.OBJECT ) {
			GVMObject o = heap.get(v.getValue());
			if( o != null) {
				return o.toString();
			}
			return String.valueOf(null);
		}
		throw new IllegalArgumentException("Value is not a STRING, but a "+v.getType());
	}

	@Override
	public int argumentCount() {
		return args;
	}

}
