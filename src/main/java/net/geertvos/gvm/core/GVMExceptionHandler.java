package net.geertvos.gvm.core;

import net.geertvos.gvm.debug.DebugInfo;
import net.geertvos.gvm.program.GVMContext;

public interface GVMExceptionHandler {

	/**
	 * Adds a hook to convert a error message into an Exception object
	 */
	public Value convert(String exceptionMessage, GVMContext context, int line, int location);
	
	public Value convert(Value value, GVMContext context, int line, int location);
}
