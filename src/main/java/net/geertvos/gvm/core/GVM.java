package net.geertvos.gvm.core;


import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import net.geertvos.gvm.bridge.NativeMethodWrapper;
import net.geertvos.gvm.core.Type.Operations;
import net.geertvos.gvm.gc.GarbageCollector;
import net.geertvos.gvm.gc.MarkAndSweepGarbageCollector;
import net.geertvos.gvm.program.GVMContext;
import net.geertvos.gvm.program.GVMFunction;
import net.geertvos.gvm.program.GVMHeap;
import net.geertvos.gvm.program.GVMProgram;
import net.geertvos.gvm.streams.RandomAccessByteStream;

/**
 * Geert Virtual Machine main class. 
 * The GVM is a stack based virtual machine that implements a simple instruction set for dynamic object oriented scripting languages. 
 * 
 * @author Geert Vos
 */
public class GVM {
	
	//The garbage collector, by default a simple depth first search trough the object references on the stack
	private GarbageCollector gc = new MarkAndSweepGarbageCollector();
	
	//The heap contains the objects
	private final GVMHeap heap;
	
	//Current program
	private GVMProgram program;

	private Collection<GVMThread> threads = new ConcurrentLinkedDeque<GVMThread>();
	
	public GVM( GVMProgram program )
	{
		this(program, new GVMHeap());
	}
	
	public GVM( GVMProgram program, GVMHeap heap )
	{
		this.program = program;
		this.heap = heap;
	}
	
	public void run()
	{
		GVMThread main = new GVMThread(program, heap);
		threads.add(main);
		heap.clear();
		
		//The following bytecode loads function#0 from the program and invokes it without parameters.
		RandomAccessByteStream bytecode = new RandomAccessByteStream();
		bytecode.write( NEW );
		bytecode.writeString("Object");
		bytecode.write( LDC_D );
		bytecode.writeInt(0);
		bytecode.writeString(new FunctionType().getName());
		bytecode.write( INVOKE );
		bytecode.writeInt(0);
		bytecode.write( HALT );
		bytecode.seek(0);

		main.setBytecode(bytecode.clone());
		fetchAndDecodeAll();
		System.out.println("> VM exited normal");
	}
	
	/**
	 * The run method executes the supplied program. Once the VM is finished this method returns.
	 * @param program
	 */
	private void fetchAndDecodeAll()
	{
		while( !threads.isEmpty() )
		{
			Iterator<GVMThread> threadIterator = threads.iterator();
			while(threadIterator.hasNext()) {
				GVMThread thread = threadIterator.next();
				boolean continues = fetchAndDecode(thread);
				if(!continues) {
					threads.remove(thread);
				}
			}
		}
	}
	
	
	public void inject(GVMThread thread)
	{
		if(threads.isEmpty()) {
			//VM was already finished
			threads.add(thread);
			fetchAndDecodeAll();
		} else {
			//TODO: Race condition here
			threads.add(thread);
		}
		thread.awaitFinished();
	}


	public boolean fetchAndDecode(GVMThread thread) {
		GVMContext context = new GVMContext(program, heap, thread);
		int instruction= GVM.HALT;
		//Fetch
		instruction = thread.getBytecode().read();
		//Decode
		switch (instruction) {
		case NEW:
		{
			String typeName = thread.getBytecode().readString();
			Type type = program.getType(typeName);
			if(type.supportsOperation(Operations.NEW)) {
				Value v = type.perform(context, Operations.NEW, null, (Value)null);
				context.getThread().getStack().push(v);
			} else {
				thread.handleException("Type "+typeName+" does not support NEW.");
			}
		}
		break;
		case LDS:
		{
			int pos = thread.getBytecode().readInt();
			if(pos >= 0) {
				int arg = thread.getFramepointer() + pos;
				thread.getStack().push(thread.getStack().get(arg));
			} else {
				thread.getStack().push(thread.getStack().get(thread.getStack().size()+pos-1));
			}
		}
		break;
		case DUP:
		{
			thread.getStack().push(thread.getStack().get(thread.getStack().size()-1));
		}
		break;
		case LDC_D:
		{
			int arg = thread.getBytecode().readInt();
			String typeName = thread.getBytecode().readString();
			Type type = program.getType(typeName);
			thread.getStack().push( new Value(arg, type) );
		}
		break;
		case INVOKE:
			{
				//Pop the function reference
				int argCount = thread.getBytecode().readInt();
				Value calleeFunction = thread.getStack().pop();
				if( !calleeFunction.getType().supportsOperation(Operations.INVOKE) ){
					thread.handleException( "Invoking a type that does not support invocation: "+calleeFunction.getType());
					break;
				}

				//Set the current function pointer
				int callerFunction = thread.getFunctionPointer();
				thread.setFunctionPointer(calleeFunction.getValue());
				GVMFunction functionDescription = program.getFunction(calleeFunction.getValue());
				//Obtain the number of parameters
				int paramCount = functionDescription.getParameters().size() ;
				if(argCount != paramCount) {
					thread.handleException( "Argument count for function "+calleeFunction.getValue()+" is "+paramCount+", but "+argCount+" provided.");
					break;
				}
				
				//Store them for now
				Value[] params = new Value[paramCount];
				for( int i=paramCount-1; i >= 0; i--)
					params[i] = thread.getStack().pop();
				
				Value thisval = thread.getStack().peek();;
				//Push state on the stack
				//TODO: create a method for this in the thread class
				thread.getCallStack().push(new StackFrame(thread.getBytecode().getPointerPosition(), thread.getFramepointer(), callerFunction, thread.getDebugLineNumber(), thread.getLocation(), thisval));
				thread.setFramepointer(thread.getStack().size()-1);
				for( int i=0;i<paramCount;i++)
				{
					thread.getStack().push(params[i]);
					params[i].setComment("Function parameter "+i);
				}
				for( int i=0;i<functionDescription.getLocals().size();i++)
				{
					thread.getStack().push(new Value(0,new Undefined(),"Local variable "+i));
				}					
				thread.setBytecode(functionDescription.getBytecode().clone());
				thread.getBytecode().seek(0);
			}
			break;
		case RETURN:
			{
				//Pop return value
				Value v = thread.getStack().pop();
				
				//Pop locals
				GVMFunction function = program.getFunction(thread.getFunctionPointer());
				int localCount = function.getLocals().size() ;
				for( int i=0;i<localCount;i++)
				{
					thread.getStack().pop();
				}
				//Pop arguments, TODO: issue.. caller can supply different number of arguments. We need to support variable arguments...
				int paramCount = function.getParameters().size() ;
				for( int i=0;i<paramCount;i++) {
					thread.getStack().pop();
				}
				thread.getStack().pop(); // this
				StackFrame frame = thread.getCallStack().pop();
				thread.setDebugLineNumber(frame.getLineNumber());
				thread.setFunctionPointer(frame.getCallingFunction());
				thread.setFramepointer(frame.getFramePointer());
				thread.setLocation(frame.getLocation());
				int pc = frame.getProgramCounter(); //PC
				thread.setBytecode(program.getFunction(thread.getFunctionPointer()).getBytecode().clone());
				thread.getBytecode().seek(pc);
				thread.getStack().push(v);
				gc.collect(heap, threads);
			}
			break;
		case PUT:
			{
				Value toSet = thread.getStack().pop();
				Value value = thread.getStack().peek();
				toSet.setValue(value.getValue());
				toSet.setType(value.getType());
			}
			break;
		case GET:
		{
			Value variableName = thread.getStack().pop();
			Value reference = thread.getStack().pop();	//pop value which must be a reference to object
			if(!reference.getType().supportsOperation(Operations.GET)) {
				thread.handleException( "Type does not support get operation: "+reference+" pc: "+thread.getBytecode().getPointerPosition()+" f:"+thread.getFunctionPointer());
				break;
			}
			Value value = reference.getType().perform(context, Operations.GET, reference, variableName);
			thread.getStack().push(value);
		}
		break;
		case GETDYNAMIC:
		{
			Value variable = thread.getStack().pop();
			//TODO: Replace this and move to types
			String variableName = program.getString(variable.getValue());
			Value theValue = null;
			for(StackFrame frame : thread.getCallStack()) {
				Value scope = frame.getScope();
				GVMObject object = heap.getObject(scope.getValue());
				if(object.hasValue(variableName)) {
					theValue = object.getValue(variableName);
					break;
				}
			}
			if(theValue == null) {
				GVMObject vo = heap.getObject(thread.getCallStack().peek().getScope().getValue());
				theValue = vo.getValue(variableName);
			}
			thread.getStack().push(theValue);
			break;
		}
		case HALT:
			{
				thread.markThreadFinished();
				return false;
			}
		case ADD: 
		{
			Value arg2 = thread.getStack().pop();
			Value arg1 = thread.getStack().pop();
			if(arg1.getType().supportsOperation(Operations.ADD)) {
				Value result = arg1.getType().perform(context, Operations.ADD, arg1, arg2);
				thread.getStack().push(result);
			}
			else thread.handleException("Type "+arg1.getType().getName()+" does not support addition.");
			break;
		}
		case SUB: 
		{
			Value arg2 = thread.getStack().pop();
			Value arg1 = thread.getStack().pop();
			if(arg1.getType().supportsOperation(Operations.SUB)) {
				Value result = arg1.getType().perform(context, Operations.SUB, arg1, arg2);
				thread.getStack().push(result);
			}
			else thread.handleException("Type "+arg1.getType().getName()+" does not support substraction.");
			break;
		}
		case MULT: 
		{
			Value arg2 = thread.getStack().pop();
			Value arg1 = thread.getStack().pop();
			if(arg1.getType().supportsOperation(Operations.MULT)) {
				Value result = arg1.getType().perform(context, Operations.MULT, arg1, arg2);
				thread.getStack().push(result);
			}
			else thread.handleException("Type "+arg1.getType().getName()+" does not support multiplication.");
			break;
		}
		case DIV: 
		{
			Value arg2 = thread.getStack().pop();
			Value arg1 = thread.getStack().pop();
			if(arg1.getType().supportsOperation(Operations.DIV)) {
				Value result = arg1.getType().perform(context, Operations.DIV, arg1, arg2);
				thread.getStack().push(result);
			}
			else thread.handleException("Type "+arg1.getType().getName()+" does not support division.");
			break;
		}
		case MOD: 
		{
			Value arg2 = thread.getStack().pop();
			Value arg1 = thread.getStack().pop();
			if(arg1.getType().supportsOperation(Operations.MOD)) {
				Value result = arg1.getType().perform(context, Operations.MOD, arg1, arg2);
				thread.getStack().push(result);
			}
			else thread.handleException( "Type "+arg1.getType().getName()+" does not support modulo.");
			break;
		}
		case AND: 
		{
			Value arg2 = thread.getStack().pop();
			Value arg1 = thread.getStack().pop();
			if(arg1.getType().supportsOperation(Operations.AND)) {
				Value result = arg1.getType().perform(context, Operations.AND, arg1, arg2);
				thread.getStack().push(result);
			}
			else thread.handleException( "Type "+arg1.getType().getName()+" does not support AND.");
			break;
		}
		case OR: 
		{
			Value arg2 = thread.getStack().pop();
			Value arg1 = thread.getStack().pop();
			if(arg1.getType().supportsOperation(Operations.OR)) {
				Value result = arg1.getType().perform(context, Operations.OR, arg1, arg2);
				thread.getStack().push(result);
			}
			else thread.handleException( "Type "+arg1.getType().getName()+" does not support OR.");
			break;
		}
		case NOT: 
		{
			Value arg1 = thread.getStack().pop();
			if(arg1.getType().supportsOperation(Operations.NOT)) {
				Value result = arg1.getType().perform(context, Operations.NOT, arg1, (Value)null);
				thread.getStack().push(result);
			}
			else thread.handleException( "Type "+arg1.getType().getName()+" does not support NOT.");
			break;
		}
		case EQL: 
		{
			Value arg2 = thread.getStack().pop();
			Value arg1 = thread.getStack().pop();
			if(arg1.getType().supportsOperation(Operations.EQL)) {
				Value result = arg1.getType().perform(context, Operations.EQL, arg1, arg2);
				thread.getStack().push(result);
			}
			else thread.handleException( "Type "+arg1.getType().getName()+" does not support EQL.");
			break;
		}
		case LT: 
		{
			Value arg2 = thread.getStack().pop();
			Value arg1 = thread.getStack().pop();
			if(arg1.getType().supportsOperation(Operations.LT)) {
				Value result = arg1.getType().perform(context, Operations.LT, arg1, arg2);
				thread.getStack().push(result);
			}
			else thread.handleException( "Type "+arg1.getType().getName()+" does not support LT.");
			break;
		}
		case GT: 
		{
			Value arg2 = thread.getStack().pop();
			Value arg1 = thread.getStack().pop();
			if(arg1.getType().supportsOperation(Operations.GT)) {
				Value result = arg1.getType().perform(context, Operations.GT, arg1, arg2);
				thread.getStack().push(result);
			}
			else thread.handleException( "Type "+arg1.getType().getName()+" does not support GT.");
			break;
		}
		case JMP: 
		{
			int pc = thread.getBytecode().readInt();
			thread.getBytecode().seek(pc);
			break;
		}
		case CJMP: 
		{
			Value cond = thread.getStack().pop();
			int jump = thread.getBytecode().readInt();
			//TODO: check if we want to make the positive check an operation
			if( cond.getValue() > 0)
			{
				thread.getBytecode().seek(jump);
			} 
			break;
		}			
		case POP:
		{
			thread.getStack().pop();
			break;
		}
		case NATIVE: {
			Value arg = thread.getStack().pop();
			if (!arg.getType().supportsOperation(Operations.INVOKE))
			{
				thread.handleException( "Type: "+arg.getType().getName()+" does not support invocation.");
				break;
			}
			//TODO: Can we link what to invoke to the type somehow?
			NativeMethodWrapper wrapper = program.getNativeWrappers().get( arg.getValue() );
			List<Value> args = new ArrayList<Value>();
			for(int i=0; i <wrapper.argumentCount() ; i++)
				args.add( thread.getStack().pop() );
			
			try {
				Value returnVal = wrapper.invoke(args , new GVMContext(program, heap, thread));
				thread.getStack().push(returnVal);
			} catch(Exception e) {
				if(e instanceof InvocationTargetException) {
					Throwable cause = ((InvocationTargetException)e).getCause();
					thread.handleException( cause.getMessage());
					break;
				}
				thread.handleException( e.getMessage());
			}
			this.gc.collect(heap, threads);
			break;
		}	
		case THROW: {
			Value arg = thread.getStack().pop();
			Value exception = thread.getProgram().getExceptionHandler().convert(arg, context, thread.getDebugLineNumber(), thread.getLocation());
			thread.handleExceptionObject(exception);
			break;
		}
		case DEBUG: {
			//TODO: Allow language developer to inject arbitrary debug info that will be mapped to an object.
			int line = thread.getBytecode().readInt();
			thread.setDebugLineNumber(line);
			thread.setLocation(thread.getBytecode().readInt());
			break;
		}
		case BREAKPOINT: {
			System.out.println("Breakpoint current line: "+thread.getDebugLineNumber());
			break;
		}
		case FORK: {
			GVMThread newThread = thread.fork();
			threads.add(newThread);
			break;
		}
		default:
			break;
		}
		return true;
	}
	//TODO: Convert to enum and merge with Operations

	//Stack manipulation
	public static final byte NEW=1;     //Create an empty object and put reference on the stack
	public static final byte LDS=2;		//Load value from the stack <pos> and put on top
	public static final byte LDG=34;	//Load value from the stack <pos> and put on top, without using the framepointer.
	public static final byte DUP=29;	//Duplicate the current top of the stack
	public static final byte LDC_D=34;	//Push a value of the specific type on the stack
	public static final byte PUT=10;		//Pop variable to set from the stack, then pop the new value from the stack. Copies the values from the latter to the first.
	public static final byte POP=27;		//Just pop a value from the stack
	public static final byte GET=11;		//Pop reference from the stack, load value <ID> from reference and push on stack
	public static final byte GETDYNAMIC = 35; //Get a field from the current scope. If it does not exists, check parent scope.. etc.. until nothing found. Then a new field is created in the current scope.
	
	//Arithmetic
	public static final byte ADD=14;		//Pop two values and add them
	public static final byte SUB=15;		//Pop two values and sub them
	public static final byte MULT=16;	//Pop two values and mult them
	public static final byte DIV=17;		//Pop two values and div them
	public static final byte MOD=30;		//Pop two values and mod them
	
	//Logic
	public static final byte AND=18;		//Pop two values and AND them
	public static final byte OR=19;		//Pop two values and OR them
	public static final byte NOT=20;		//Pop value and invert it
	public static final byte EQL=21;		//Pop two values and return true if values are equal
	public static final byte GT=22;		//Pop two values and return true if x>y
	public static final byte LT=23;		//Pop two values and return true if x<y

	//Control flow
	public static final byte NATIVE=28;
	public static final byte INVOKE=8; 	//PUT program counter on stack and set PC to location of function
	public static final byte RETURN=9;	//POP PC from the stack and set PC to old PC, leave return values on the stack
	public static final byte CJMP=24;	//Pop value, if true set PC to argument
	public static final byte JMP=25;		//Set PC to argument
	public static final byte THROW=31;		//Pop value from the stack and throw as Exception
	public static final byte HALT=12;	//End machine
	public static final byte FORK=37;  //Branch of new thread
	
	//Debug
	public static final byte DEBUG=32;      //Tell the VM about the code that is being executed. For deubgging purposes.
	public static final byte BREAKPOINT=33; //Tell the VM to pause and allow for inspection of heap and stack.

}
