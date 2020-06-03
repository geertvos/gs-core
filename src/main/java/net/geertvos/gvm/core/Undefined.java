package net.geertvos.gvm.core;

public class Undefined implements Type {

	@Override
	public String getName() {
		return "Undefined";
	}

	@Override
	public boolean supportsOperation(Operations op) {
		//For now not support any operation
		return false;
	}

	@Override
	public Value perform(Operations op, Value thisValue, Value otherValue) {
		// TODO Auto-generated method stub
		return null;
	}

}
