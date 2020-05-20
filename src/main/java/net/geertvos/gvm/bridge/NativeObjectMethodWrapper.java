package net.geertvos.gvm.bridge;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import net.geertvos.gvm.core.GVMObject;
import net.geertvos.gvm.core.Value;
import net.geertvos.gvm.core.Value.TYPE;
import net.geertvos.gvm.program.GVMProgram;
import net.geertvos.gvm.program.GVMHeap;

public class NativeObjectMethodWrapper extends NativeMethodWrapper {

	private Object parent;
	private String methodName;
	private int paramCount;
	
	public NativeObjectMethodWrapper(String methodName, Object parent, int paramCount) {
		this.parent = parent;
		this.methodName = methodName;
		this.paramCount = paramCount;
	}

	@Override
	public Value invoke(List<Value> arguments, GVMHeap heap, GVMProgram program) {
		ValueConverter converter = new ValueConverter(heap, program);
		try {
			Object[] wrappedArgs = new Object[arguments.size()];
			Class[] wrappedTypes = new Class[arguments.size()];
			for(int i=0;i<arguments.size();i++) {
				Object converted = converter.convertFromGVM(arguments.get(i));
				wrappedArgs[i] = converted;
				wrappedTypes[i] = converted.getClass();
			}

			//TODO: Add object support
			Method theMethod = null;
			int count = 0;
			for(Method m : parent.getClass().getMethods()) {
				if(m.getName().equals(methodName)) {
					theMethod = m;
					count++;
				}
			}
			if(count > 1) {
				//Check arguments
				theMethod = parent.getClass().getMethod(methodName, wrappedTypes);
			}
			theMethod.setAccessible(true);
			Object returnValue = theMethod.invoke(parent, wrappedArgs);
			return converter.convertToGVM(returnValue);

		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	@Override
	public int argumentCount() {
		return paramCount;
	}
	
}
