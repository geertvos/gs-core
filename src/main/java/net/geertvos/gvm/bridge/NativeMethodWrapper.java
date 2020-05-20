package net.geertvos.gvm.bridge;

import java.util.List;

import net.geertvos.gvm.core.Value;
import net.geertvos.gvm.program.GVMProgram;
import net.geertvos.gvm.program.GVMHeap;

/**
 * Wrapper to allow the GVM to execute a 'native' method
 * @author geert
 *
 */
public abstract class NativeMethodWrapper {

	public abstract Value invoke( List<Value> arguments, GVMHeap heap, GVMProgram program );
	
	public abstract int argumentCount();
	
}
