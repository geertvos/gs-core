package net.geertvos.gvm.core;

public interface Type {

	String getName();
	
	boolean supportsOperation(Operations op);
	
	Value perform( Operations op, Value thisValue, Value otherValue);
	
	enum Operations {
		ADD,
		SUB,
		MULT,
		DIV,
		MOD,
		AND,
		OR,
		NOT,
		EQL,
		LT,
		GT,
		GET
	}
	
}
