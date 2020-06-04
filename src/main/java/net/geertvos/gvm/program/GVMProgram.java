package net.geertvos.gvm.program;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.geertvos.gvm.bridge.NativeMethodWrapper;
import net.geertvos.gvm.bridge.ValueConverter;
import net.geertvos.gvm.core.BooleanType;
import net.geertvos.gvm.core.FunctionType;
import net.geertvos.gvm.core.GVMExceptionHandler;
import net.geertvos.gvm.core.Type;
import net.geertvos.gvm.core.Undefined;

/**
 * Represents a program that can be executed by the GVM. A program contains
 * functions and string constants.
 * 
 * @author geertvos
 *
 */
public class GVMProgram {

	private final String name;
	private final Map<Integer,GVMFunction> functions = new HashMap<Integer,GVMFunction>();
	private final List<String> stringConstants = new ArrayList<String>();
	private final Map<String,Type> types = new HashMap<String,Type>();
	private final GVMExceptionHandler exceptionHandler;
	private final ValueConverter converter;
	private final AtomicInteger functionCounter = new AtomicInteger();
	
	private List<NativeMethodWrapper> nativeWrappers = new ArrayList<NativeMethodWrapper>();
	
	public GVMProgram(String name, GVMExceptionHandler exceptionHandler, ValueConverter converter) {
		this.name = name;
		this.converter = converter;
		this.exceptionHandler = exceptionHandler;
		registerType(new BooleanType());
		registerType(new Undefined());
		registerType(new FunctionType());
	}

	public void addString(String s, int index) {
		stringConstants.add(index, s);
	}

	public int addString(String s) {
		if (!stringConstants.contains(s))
			stringConstants.add(s);
		return stringConstants.indexOf(s);
	}

	public int findString(String s) {
		return stringConstants.indexOf(s);
	}
	
	public String getString(int i) {
		return stringConstants.get(i);
	}

	public String getName() {
		return name;
	}

	/**
	 * Returns the main function for this program.
	 * 
	 * @return
	 */
	public GVMFunction getMain() {
		return functions.get(0);
	}

	public GVMFunction getFunction(int i) {
		return functions.get(i);
	}

	public List<NativeMethodWrapper> getNativeWrappers() {
		return nativeWrappers;
	}

	public int add(NativeMethodWrapper method) {
		nativeWrappers.add(method);
		return nativeWrappers.indexOf(method);
	}

	public List<String> getStringConstants() {
		return stringConstants;
	}

	public int addFunction(GVMFunction function) {
		int id = functionCounter.getAndIncrement();
		functions.put(id, function);
		return id;
	}

	public void deleteFunction(int id) {
		functions.remove(id);
	}

	public void setNatives(List<NativeMethodWrapper> natives) {
		this.nativeWrappers = natives;
	}

	public Map<Integer,GVMFunction> getFunctions() {
		return functions;
	}

	public Type getType(String typeName) {
		if(types.containsKey(typeName)) {
			return types .get(typeName);
		} else {
			throw new IllegalArgumentException("Type: "+typeName+" is not a known type.");
		}
	}
	
	public void registerType(Type type) {
		this.types.put(type.getName(), type);
	}
	
	public GVMExceptionHandler getExceptionHandler() {
		return exceptionHandler;
	}
	
	public ValueConverter getConverter() {
		return converter;
	}

}
