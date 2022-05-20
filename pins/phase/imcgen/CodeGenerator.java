package pins.phase.imcgen;

import java.util.*;

import pins.data.ast.*;
import pins.data.ast.visitor.*;
import pins.data.imc.code.expr.*;
import pins.data.imc.code.stmt.ImcMOVE;
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
		ImcExpr expr = funDecl.expr.accept(new ExprGenerator(), arg);
		ImcTEMP rvTemp = new ImcTEMP(arg.peek().RV);
		ImcMOVE writeRV = new ImcMOVE(rvTemp, expr);
		//ImcGen.exprImc.put(funDecl.expr, new ImcSEXPR(writeRV, rvTemp));
		arg.pop();
		
		return null;
	}
}