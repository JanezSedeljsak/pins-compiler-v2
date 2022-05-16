package pins.phase.imcgen;

import java.util.*;

import pins.data.ast.*;
import pins.data.ast.visitor.*;
import pins.data.mem.*;
import pins.phase.memory.*;

public class CodeGenerator extends AstFullVisitor<Object, Stack<MemFrame>> {

	@Override
	public Object visit(AstFunDecl funDecl, Stack<MemFrame> arg) {
		if (arg == null) {
			arg = new Stack<>();
		}

		MemFrame frame = Memory.frames.get(funDecl);
		arg.add(frame);
		funDecl.expr.accept(new ExprGenerator(), arg);
		arg.pop();
		
		return null;
	}
}
