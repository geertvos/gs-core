package net.geertvos.gvm.core;

import net.geertvos.gvm.program.GVMContext;

public class Undefined implements Type {

	@Override
	public String getName() {
		return "Undefined";
	}

	@Override
	public boolean supportsOperation(Operations op) {
		if(op.equals(Operations.EQL)) {
			return true;
		}
		return false;
	}

	@Override
	public Value perform(GVMContext context, Operations op, Value thisValue, Value otherValue) {
		if(op.equals(Operations.EQL)) {
			if(otherValue.getType() instanceof Undefined) { 
				return new Value(1, new BooleanType());
			} else {
				return new Value(0, new BooleanType());
			}
		}
		throw new IllegalArgumentException("Operation "+op+" not supported on Undefined.");
	}

	@Override
	public Value perform(GVMContext context, Operations op, Value thisValue, Object parameter) {
		throw new IllegalArgumentException("Operation " + op + " is not supported by type " + getName());
	}

	@Override
	public boolean isInstance(Type otherType) {
		if(otherType.getName().equals(getName())) {
			return true;
		}
		return false;
	}

}
