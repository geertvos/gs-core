package net.geertvos.gvm.program;

import net.geertvos.gvm.core.GVM;
import net.geertvos.gvm.core.GVMThread;

public class GVMContext {

	private final GVM gvm;
	private final GVMThread thread;
	
	public GVMContext(GVM gvm, GVMThread thread) {
		super();
		this.gvm = gvm;
		this.thread = thread;
	}

	public GVMProgram getProgram() {
		return gvm.getProgram();
	}

	public GVMHeap getHeap() {
		return gvm.getHeap();
	}

	public GVMThread getThread() {
		return thread;
	}
	
	public GVM getGVM() {
		return gvm;
	}
	
	
}
