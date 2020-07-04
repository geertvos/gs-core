package net.geertvos.gvm.core;

import net.geertvos.gvm.program.GVMContext;

public class BooleanType implements Type {

	@Override
	public String getName() {
		return "Boolean";
	}

	@Override
	public boolean supportsOperation(Operations op) {
		if (op.equals(Operations.NOT)) {
			return true;
		}
		if (op.equals(Operations.AND)) {
			return true;
		}
		if (op.equals(Operations.OR)) {
			return true;
		}
		if (op.equals(Operations.EQL)) {
			return true;
		}
		return false;
	}

	@Override
	public Value perform(GVMContext context, Operations op, Value thisValue, Value otherValue) {
		if (op.equals(Operations.NOT)) {
			if (thisValue.getValue() > 0) {
				return new Value(0, new BooleanType());
			} else {
				return new Value(1, new BooleanType());
			}
		} else if (op.equals(Operations.AND)) {
			return new Value((thisValue.getValue()>0 && otherValue.getValue()>0)?1:0, new BooleanType());
		} else if (op.equals(Operations.OR)) {
			return new Value((thisValue.getValue()>0 || otherValue.getValue()>0)?1:0, new BooleanType());
		} else if (op.equals(Operations.EQL)) {
			//TODO: Type checking required
			return new Value((thisValue.getValue()>0 == otherValue.getValue()>0)?1:0, new BooleanType());
		} else {
			throw new IllegalArgumentException("Operation " + op + " is not supported by type " + getName());
		}
	}

	@Override
	public boolean isInstance(Type otherType) {
		if(otherType.getName().equals(getName())) {
			return true;
		}
		return false;
	}

}
