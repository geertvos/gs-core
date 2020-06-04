package net.geertvos.gvm.core;

import net.geertvos.gvm.program.GVMContext;

/**
 * The basic interface that allows the developer to plug types into the GVM
 * @author Geert Vos
 *
 */
public interface Type {

	String getName();
	
	boolean supportsOperation(Operations op);
	
	Value perform(GVMContext context, Operations op, Value thisValue, Value otherValue);

	Value perform(GVMContext context, Operations op, Value thisValue, String parameter);
	
	//TODO: Merge the Operations enum with the OP codes
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
		GET,
		INVOKE, 
		NEW,
	}
	
}
