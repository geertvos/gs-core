package net.geertvos.gvm.core;

import net.geertvos.gvm.program.GVMContext;

public class FunctionType implements Type {

	@Override
	public String getName() {
		return "Function";
	}

	@Override
	public boolean supportsOperation(Operations op) {
		if(op.equals(Operations.INVOKE)) {
			return true;
		}
		return false;
	}

	@Override
	public Value perform(GVMContext context, Operations op, Value thisValue, Value otherValue) {
		return new Value(0,new Undefined());
	}

	@Override
	public Value perform(GVMContext context, Operations op, Value thisValue, Object parameter) {
		throw new IllegalArgumentException("Operation " + op + " is not supported by type " + getName());
	}

}
