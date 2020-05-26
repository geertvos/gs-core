package net.geertvos.gvm.debug;


import java.io.PrintStream;
import java.util.Map.Entry;

import net.geertvos.gvm.core.GVM;
import net.geertvos.gvm.program.GVMFunction;
import net.geertvos.gvm.program.GVMProgram;
import net.geertvos.gvm.streams.RandomAccessByteStream;

/**
 * Geert Virtual Machine main class. The GVM is a stack based virtual machine
 * that implements a simple instruction set for dynamic object oriented
 * scripting languages.
 * 
 * It supports the following basic types: - undefined - boolean - number -
 * string - object
 * 
 * @author geertvos
 */
public class DebugInfo {

	
	public static void displayProgram( GVMProgram p )
	{
		for( Entry<Integer,GVMFunction> entry : p.getFunctions().entrySet() )
		{
			System.out.println("Function: "+entry.getKey());
			System.out.println("Parameters:");
			System.out.println(entry.getValue().getParameters());
			System.out.println("Locals:");
			System.out.println(entry.getValue().getLocals());
			System.out.println("Bytecode:");
			displayFunction(System.out, entry.getValue().getBytecode());
			System.out.println();
		}		
	}

	/**
	 * The run method executes the supplied program. Once the VM is finished
	 * this method returns.
	 * 
	 * @param program
	 */
	public static void displayFunction(PrintStream out, RandomAccessByteStream bytecode) {
		bytecode.seek(0);
		while (bytecode.getPointerPosition() < bytecode.size()) {

			// Fetch
			byte instruction = bytecode.read();
			switch (instruction) {
			case GVM.NEW: {
				out.println("NEW");
			}
				break;
			case GVM.LDS: {
				int arg = bytecode.readInt();
				out.println("LDS " + arg);
			}
				break;
			case GVM.DUP: {
				out.println("DUP");
			}
				break;
			case GVM.LDC_N: {
				int arg = bytecode.readInt();
				out.println("LDC_N " + arg);
			}
				break;
			case GVM.LDC_S: {
				int arg = bytecode.readInt();
				out.println("LDC_S " + arg);
			}
				break;
			case GVM.LDC_B: {
				int arg = bytecode.read();
				out.println("LDC_B " + arg);
			}
				break;
			case GVM.LDC_U: {
				out.println("LDC_U");
			}
				break;
			case GVM.LDC_F: {
				int arg = bytecode.readInt();
				out.println("LDC_F " + arg);
			}
				break;
			case GVM.INVOKE: {
				out.println("INVOKE");
			}
				break;
			case GVM.RETURN: {
				out.println("RETURN");
			}
				break;
			case GVM.PUT: {
				out.println("PUT");
			}
				break;
			case GVM.GET: {
				String fieldName = bytecode.readString();
				out.println("GET "+fieldName);
			}
				break;
			case GVM.HALT: {
				out.println("HALT");
				break;
			}
			case GVM.ADD: {
				out.println("ADD");
			}
				break;
			case GVM.SUB: {
				out.println("SUB");
				break;
			}
			case GVM.MULT: {
				out.println("MULT");
				break;
			}
			case GVM.DIV: {
				out.println("DIV");
				break;
			}
			case GVM.AND: {
				out.println("AND");
				break;
			}
			case GVM.OR: {
				out.println("OR");
				break;
			}
			case GVM.NOT: {
				out.println("NOT");
				break;
			}
			case GVM.EQL: {
				out.println("EQL");
				break;
			}
			case GVM.LT: {
				out.println("LT");
				break;
			}
			case GVM.GT: {
				out.println("GT");
				break;
			}
			case GVM.JMP: {
				int pc = bytecode.readInt();
				out.println("JMP " + pc);
				break;
			}
			case GVM.CJMP: {
				int pc = bytecode.readInt();
				out.println("CJMP " + pc);
			}
			case GVM.POP: {
				out.println("POP");
				break;
			}
			case GVM.NATIVE: {
				out.println("NATIVE");
				break;
			}
			default:
				break;
			}
		}
	}
}
