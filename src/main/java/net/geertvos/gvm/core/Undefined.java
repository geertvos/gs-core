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
				context.getThread().getStack().push(new Value(1, new BooleanType()));
			} else {
				context.getThread().getStack().push(new Value(0, new BooleanType()));
			}
		}
		return null;
	}

	@Override
	public Value perform(GVMContext context, Operations op, Value thisValue, String parameter) {
		// TODO Auto-generated method stub
		return null;
	}

}
