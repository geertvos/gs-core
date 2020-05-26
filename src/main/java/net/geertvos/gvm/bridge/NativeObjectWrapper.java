package net.geertvos.gvm.bridge;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.geertvos.gvm.core.GVM;
import net.geertvos.gvm.core.GVMObject;
import net.geertvos.gvm.core.Value;
import net.geertvos.gvm.core.Value.TYPE;
import net.geertvos.gvm.program.GVMContext;
import net.geertvos.gvm.program.GVMFunction;
import net.geertvos.gvm.streams.RandomAccessByteStream;

public class NativeObjectWrapper implements GVMObject {

	private Object object;
	private Map<String,Value> methods = new HashMap<String, Value>();
	private ValueConverter converter;
	private List<Integer> generatedFunctions = new LinkedList<Integer>();
	private final GVMContext context;
	
	public NativeObjectWrapper(Object object,GVMContext context) {
		this.object = object;
		this.context = context;
		this.converter = new ValueConverter(context);
		for(Method m : object.getClass().getMethods()) {
			//Generate wrapper function
			NativeObjectMethodWrapper wrapperMethod = new NativeObjectMethodWrapper(m.getName(), object, m.getParameterCount());
			int nativeFunction = context.getProgram().add(wrapperMethod);
			RandomAccessByteStream code = new RandomAccessByteStream();
			List<String> paramNames = new LinkedList<String>();
			int i=1;
			for(Parameter parameter : m.getParameters()) {
				paramNames.add(parameter.getName());
				code.add(GVM.LDS);
				code.writeInt(i);
				i++;
			}
			//Generate a function to call the native method
			code.add(GVM.LDC_F);
			code.writeInt(nativeFunction);
			code.add(GVM.NATIVE);
			code.add(GVM.RETURN);
			GVMFunction function = new GVMFunction(code, paramNames);
			int index = context.getProgram().addFunction(function);
			generatedFunctions.add(index);
			generatedFunctions.add(nativeFunction);
			methods.put(m.getName(), new Value(index, TYPE.FUNCTION, "Generated function to call "+m.getName()+" on "+object.getClass().getName()));
			//TODO: invent a mechanism to GC unused functions
		}
	}


	@Override
	public void setValue(String id, Value v) {
		//do later
	}

	@Override
	public Value getValue(String id) {
		try {
			Field field = object.getClass().getField(id);
			Object returnValue = field.get(object);
			return converter.convertToGVM(returnValue);
		} catch(NoSuchFieldException e) {
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		if(methods.containsKey(id)) {
			return methods.get(id);
		} else {
			return new Value(0, TYPE.UNDEFINED);
		}
	}

	public Object getObject() {
		return object;
	}
	
	@Override
	public Collection<Value> getValues() {
		return methods.values();
	}


	@Override
	public void preDestroy() {
		for(Integer functionId : generatedFunctions) {
			this.context.getProgram().deleteFunction(functionId);
		}
		
	}
	
}
