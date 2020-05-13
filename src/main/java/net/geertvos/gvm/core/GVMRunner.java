package net.geertvos.gvm.core;

import net.geertvos.gvm.program.GVMProgram;

public class GVMRunner {

	public static void main(String[] args) {
		GVMProgram program = new GVMProgram("test");
		GVM gvm = new GVM(program);
		
	}
	
}
