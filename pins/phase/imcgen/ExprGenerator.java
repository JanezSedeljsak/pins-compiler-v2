package pins.phase.imcgen;

import java.util.*;

import pins.data.ast.*;
import pins.data.ast.visitor.*;
import pins.data.imc.code.expr.*;
//import pins.data.imc.code.stmt.*;
import pins.data.mem.*;
//import pins.data.typ.*;
//import pins.phase.memory.*;
//import pins.phase.seman.*;

public class ExprGenerator implements AstVisitor<ImcExpr, Stack<MemFrame>> {

	@Override
	public ImcExpr visit(AstWhereExpr whereExpr, Stack<MemFrame> frames) {
		whereExpr.decls.accept(new CodeGenerator(), frames);
		ImcExpr code = whereExpr.subExpr.accept(this, frames);
		ImcGen.exprImc.put(whereExpr, code);
		return code;
	}

}
