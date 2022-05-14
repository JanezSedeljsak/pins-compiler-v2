package pins.phase.imcgen;

import java.util.*;

import pins.data.ast.*;
import pins.data.ast.visitor.*;
import pins.data.imc.code.stmt.*;
import pins.data.mem.*;

public class StmtGenerator implements AstVisitor<ImcStmt, Stack<MemFrame>> {

	@Override
	public ImcStmt visit(AstAssignStmt assignStmt, Stack<MemFrame> frames) {
		ImcStmt code = new ImcMOVE(assignStmt.fstSubExpr.accept(new ExprGenerator(), frames),
				assignStmt.sndSubExpr.accept(new ExprGenerator(), frames));
		ImcGen.stmtImc.put(assignStmt, code);
		return code;
	}

}
