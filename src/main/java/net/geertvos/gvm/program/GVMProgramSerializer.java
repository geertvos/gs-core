package net.geertvos.gvm.program;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.geertvos.gvm.bridge.NativeMethodWrapper;
import net.geertvos.gvm.bridge.ValueConverter;
import net.geertvos.gvm.core.GVMExceptionHandler;
import net.geertvos.gvm.streams.RandomAccessByteStream;

public class GVMProgramSerializer {

	public interface NativeMethodFactory {
		NativeMethodWrapper create(int argumentCount);
	}

	private static final byte[] MAGIC = new byte[]{'G', 'S', 'V', 'M'};
	private static final int VERSION = 2;

	private static int intern(List<String> table, Map<String, Integer> index, String s) {
		Integer existing = index.get(s);
		if (existing != null) return existing;
		int idx = table.size();
		table.add(s);
		index.put(s, idx);
		return idx;
	}

	public static void writeTo(GVMProgram program, OutputStream out) throws IOException {
		List<String> table = new ArrayList<>();
		Map<String, Integer> tableIndex = new HashMap<>();

		intern(table, tableIndex, program.getName());
		for (String s : program.getStringConstants()) {
			intern(table, tableIndex, s);
		}
		for (Map.Entry<Integer, GVMFunction> entry : program.getFunctions().entrySet()) {
			GVMFunction f = entry.getValue();
			if (f.getDebugName() != null) intern(table, tableIndex, f.getDebugName());
			for (String p : f.getParameters()) intern(table, tableIndex, p);
			for (String l : f.getLocals()) intern(table, tableIndex, l);
		}

		RandomAccessByteStream buf = new RandomAccessByteStream();

		buf.write(MAGIC);
		buf.writeInt(VERSION);

		buf.writeInt(table.size());
		for (String s : table) {
			buf.writeString(s);
		}

		buf.writeInt(intern(table, tableIndex, program.getName()));

		List<String> strings = program.getStringConstants();
		buf.writeInt(strings.size());
		for (String s : strings) {
			buf.writeInt(intern(table, tableIndex, s));
		}

		List<NativeMethodWrapper> natives = program.getNativeWrappers();
		buf.writeInt(natives.size());
		for (NativeMethodWrapper n : natives) {
			buf.writeInt(n.argumentCount());
		}

		Map<Integer, GVMFunction> functions = program.getFunctions();
		buf.writeInt(functions.size());
		for (Map.Entry<Integer, GVMFunction> entry : functions.entrySet()) {
			buf.writeInt(entry.getKey());
			GVMFunction f = entry.getValue();
			buf.writeInt(f.getIndex());

			if (f.getDebugName() != null) {
				buf.writeInt(intern(table, tableIndex, f.getDebugName()));
			} else {
				buf.writeInt(-1);
			}

			List<String> params = f.getParameters();
			buf.writeInt(params.size());
			for (String p : params) {
				buf.writeInt(intern(table, tableIndex, p));
			}

			List<String> locals = f.getLocals();
			buf.writeInt(locals.size());
			for (String l : locals) {
				buf.writeInt(intern(table, tableIndex, l));
			}

			List<int[]> handlers = f.getExceptionHandlers();
			buf.writeInt(handlers.size());
			for (int[] h : handlers) {
				buf.writeInt(h[0]);
				buf.writeInt(h[1]);
				buf.writeInt(h[2]);
			}

			byte[] bytecode = f.getBytecode().getBytes();
			buf.writeInt(bytecode.length);
			buf.write(bytecode);
		}

		buf.writeTo(out);
	}

	public static GVMProgram readFrom(InputStream in, GVMExceptionHandler exceptionHandler, ValueConverter converter, NativeMethodFactory nativeFactory) throws IOException {
		RandomAccessByteStream buf = new RandomAccessByteStream();
		buf.readFrom(in);
		buf.seek(0);

		byte[] magic = buf.read(4);
		if (magic[0] != MAGIC[0] || magic[1] != MAGIC[1] || magic[2] != MAGIC[2] || magic[3] != MAGIC[3]) {
			throw new IOException("Invalid GSVM binary: bad magic bytes");
		}
		int version = buf.readInt();
		if (version != VERSION) {
			throw new IOException("Unsupported GSVM binary version: " + version + " (expected " + VERSION + ")");
		}
		return readV2(buf, exceptionHandler, converter, nativeFactory);
	}

	private static GVMProgram readV2(RandomAccessByteStream buf, GVMExceptionHandler exceptionHandler, ValueConverter converter, NativeMethodFactory nativeFactory) throws IOException {
		int tableSize = buf.readInt();
		List<String> stringTable = new ArrayList<>(tableSize);
		for (int i = 0; i < tableSize; i++) {
			stringTable.add(buf.readString());
		}

		int nameIdx = buf.readInt();
		String name = stringTable.get(nameIdx);
		GVMProgram program = new GVMProgram(name, exceptionHandler, converter);

		int stringCount = buf.readInt();
		for (int i = 0; i < stringCount; i++) {
			int idx = buf.readInt();
			program.addString(stringTable.get(idx), i);
		}

		int nativeCount = buf.readInt();
		List<NativeMethodWrapper> natives = new ArrayList<>();
		for (int i = 0; i < nativeCount; i++) {
			int argCount = buf.readInt();
			natives.add(nativeFactory.create(argCount));
		}
		program.setNatives(natives);

		int funcCount = buf.readInt();
		for (int i = 0; i < funcCount; i++) {
			int id = buf.readInt();
			int index = buf.readInt();

			int debugNameIdx = buf.readInt();
			String debugName = debugNameIdx >= 0 ? stringTable.get(debugNameIdx) : null;

			int paramCount = buf.readInt();
			List<String> params = new ArrayList<>();
			for (int p = 0; p < paramCount; p++) {
				params.add(stringTable.get(buf.readInt()));
			}

			int localCount = buf.readInt();
			List<String> locals = new ArrayList<>();
			for (int l = 0; l < localCount; l++) {
				locals.add(stringTable.get(buf.readInt()));
			}

			int handlerCount = buf.readInt();
			List<int[]> handlers = new ArrayList<>();
			for (int h = 0; h < handlerCount; h++) {
				handlers.add(new int[]{buf.readInt(), buf.readInt(), buf.readInt()});
			}

			int bytecodeSize = buf.readInt();
			byte[] bytecode = buf.read(bytecodeSize);

			RandomAccessByteStream code = new RandomAccessByteStream();
			code.write(bytecode);

			GVMFunction function = new GVMFunction(code, params);
			function.setIndex(index);
			if (debugName != null) function.setDebugName(debugName);
			for (String local : locals) {
				function.registerLocalVariable(local);
			}
			for (int[] h : handlers) {
				function.registerCatchBlock(h[0], h[1], h[2]);
			}
			program.addFunction(id, function);
		}

		return program;
	}

}
