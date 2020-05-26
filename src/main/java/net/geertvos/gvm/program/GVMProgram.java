package net.geertvos.gvm.program;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import net.geertvos.gvm.bridge.NativeMethodWrapper;

/**
 * Represents a program that can be executed by the GVM. A program contains
 * functions and string constants.
 * 
 * @author geertvos
 *
 */
public class GVMProgram {

	private final Map<Integer,GVMFunction> functions = new HashMap<Integer,GVMFunction>();
	private List<String> stringConstants = new ArrayList<String>();
	private List<NativeMethodWrapper> nativeWrappers = new ArrayList<NativeMethodWrapper>();
	private final String name;
	private AtomicInteger functionCounter = new AtomicInteger();
	
	public GVMProgram(String name) {
		this.name = name;
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

}
