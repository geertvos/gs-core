package net.geertvos.gvm.program;

import net.geertvos.gvm.core.GVMThread;

public class GVMContext {

	private final GVMProgram program;
	private final GVMHeap heap;
	private final GVMThread thread;
	
	public GVMContext(GVMProgram program, GVMHeap heap, GVMThread thread) {
		super();
		this.program = program;
		this.heap = heap;
		this.thread = thread;
	}

	public GVMProgram getProgram() {
		return program;
	}

	public GVMHeap getHeap() {
		return heap;
	}

	public GVMThread getThread() {
		return thread;
	}
	
	
}
