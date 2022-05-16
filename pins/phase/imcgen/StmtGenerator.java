package pins.phase.imcgen;

import java.util.*;

import pins.data.ast.*;
import pins.data.ast.visitor.*;
import pins.data.imc.code.stmt.*;
import pins.data.mem.*;

public class StmtGenerator implements AstVisitor<ImcStmt, Stack<MemFrame>> {

	@Override
	@SuppressWarnings("unchecked")
	public ImcStmt visit(ASTs<? extends AST> trees, Stack<MemFrame> frames) {
		Vector<ImcStmt> stmts = new Vector<ImcStmt>();
		for(AstStmt tree : (Collection<AstStmt>)trees.asts()) {
			stmts.add(tree.accept(this, frames));
		}
		return new ImcSTMTS(stmts);
	}

	@Override
	public ImcStmt visit(AstAssignStmt assignStmt, Stack<MemFrame> frames) {
		ImcStmt code = new ImcMOVE(assignStmt.fstSubExpr.accept(new ExprGenerator(), frames), assignStmt.sndSubExpr.accept(new ExprGenerator(), frames));
		ImcGen.stmtImc.put(assignStmt, code);
		return code;
	}

	@Override
	public ImcStmt visit(AstExprStmt exprStmt, Stack<MemFrame> frames) {
		ImcStmt code = new ImcESTMT(exprStmt.expr.accept(new ExprGenerator(), frames));
		ImcGen.stmtImc.put(exprStmt, code);
		return code;
	}

	@Override
	public ImcStmt visit(AstIfStmt ifStmt, Stack<MemFrame> frames) {
		if (ifStmt.condExpr != null)
			ifStmt.condExpr.accept(this, frames);
		if (ifStmt.thenBodyStmt != null)
			ifStmt.thenBodyStmt.accept(this, frames);
		if (ifStmt.elseBodyStmt != null)
			ifStmt.elseBodyStmt.accept(this, frames);
		return null;
	}

	@Override
	public ImcStmt visit(AstWhileStmt whileStmt, Stack<MemFrame> frames) {
		if (whileStmt.condExpr != null)
			whileStmt.condExpr.accept(this, frames);
		if (whileStmt.bodyStmt != null)
			whileStmt.bodyStmt.accept(this, frames);
		return null;
	}

}
