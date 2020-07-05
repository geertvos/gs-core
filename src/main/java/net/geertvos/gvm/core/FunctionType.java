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
		if(op.equals(Operations.EQL)) {
			return true;
		}
		return false;
	}

	@Override
	public Value perform(GVMContext context, Operations op, Value thisValue, Value otherValue) {
		if(op.equals(Operations.EQL)) {
			if(thisValue.getValue() == otherValue.getValue()) {
				return new Value(1, new BooleanType());
			} else {
				return new Value(0, new BooleanType());
			}
		}
		return new Value(0,new Undefined());
	}

	@Override
	public boolean isInstance(Type otherType) {
		if(otherType.getName().equals(getName())) {
			return true;
		}
		return false;
	}


}
