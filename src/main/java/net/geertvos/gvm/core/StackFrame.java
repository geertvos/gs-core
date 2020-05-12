package net.geertvos.gvm.core;

public class StackFrame {

	private int programCounter;
	private int framePointer;
	private int callingFunction;
	private int lineNumber;  //TODO: Replace with DebugInfo object
	private int location;  //TODO: Replace with DebugInfo object
	private Value scope;

	public StackFrame(int programCounter, int framePointer, int callingFunction, int lineNumber, int location, Value scope) {
		super();
		this.programCounter = programCounter;
		this.framePointer = framePointer;
		this.callingFunction = callingFunction;
		this.lineNumber = lineNumber;
		this.scope = scope;
		this.location = location;
	}

	public int getProgramCounter() {
		return programCounter;
	}

	public void setProgramCounter(int programCounter) {
		this.programCounter = programCounter;
	}

	public int getFramePointer() {
		return framePointer;
	}

	public void setFramePointer(int framePointer) {
		this.framePointer = framePointer;
	}

	public int getCallingFunction() {
		return callingFunction;
	}

	public int getLocation() {
		return location;
	}

	public void setModuleName(int moduleName) {
		this.location = moduleName;
	}

	public void setCallingFunction(int callingFunction) {
		this.callingFunction = callingFunction;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	public Value getScope() {
		return scope;
	}

	public void setScope(Value scope) {
		this.scope = scope;
	}

}
