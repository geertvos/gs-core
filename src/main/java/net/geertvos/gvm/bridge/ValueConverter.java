package net.geertvos.gvm.bridge;

import net.geertvos.gvm.core.Value;
import net.geertvos.gvm.program.GVMContext;

public interface ValueConverter {

	
	public Object convertFromGVM(GVMContext context ,Value value);

	public Object convertFromGVM(GVMContext context ,Value value, Class convertTo);

	public Value convertToGVM(GVMContext context ,Object returnValue);
	
}
