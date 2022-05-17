package pins.phase.imcgen;

import java.util.*;

import pins.data.ast.*;
import pins.data.ast.visitor.*;
import pins.data.imc.code.expr.ImcExpr;
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
		Vector<ImcStmt> stmts = new Vector<ImcStmt>();
		ImcExpr cond = ifStmt.condExpr.accept(new ExprGenerator(), frames);

		MemLabel posLbl = new MemLabel();
		MemLabel negLbl = new MemLabel();

		ImcCJUMP cjump = new ImcCJUMP(cond, posLbl, negLbl);
		ImcStmt thenBody = ifStmt.thenBodyStmt.accept(new StmtGenerator(), frames);

		// only IF -> CJUMP("POS", "NEG") - "POS" - BODY - "NEG"
		if (ifStmt.elseBodyStmt == null) {
			stmts.add(cjump);
            stmts.add(new ImcLABEL(posLbl));
            stmts.add(thenBody);
            stmts.add(new ImcLABEL(negLbl));
		} 
		// IF - ELSE -> CJUMP("POS", "NEG") - "POS" - BODY - JUMP("END") - "NEG" - ELSE_BODY - "END"
		else {
			MemLabel endLbl = new MemLabel();
			ImcStmt elseBody = ifStmt.elseBodyStmt.accept(new StmtGenerator(), frames);

			stmts.add(cjump);
            stmts.add(new ImcLABEL(posLbl));
            stmts.add(thenBody);
            stmts.add(new ImcJUMP(endLbl));
            stmts.add(new ImcLABEL(negLbl));
            stmts.add(elseBody);
            stmts.add(new ImcLABEL(endLbl));
		}

		ImcSTMTS result = new ImcSTMTS(stmts);
		ImcGen.stmtImc.put(ifStmt, result);
		return result;
	}

	@Override
	public ImcStmt visit(AstWhileStmt whileStmt, Stack<MemFrame> frames) {
		Vector<ImcStmt> stmts = new Vector<ImcStmt>();
		ImcExpr cond = whileStmt.condExpr.accept(new ExprGenerator(), frames);

		MemLabel condLbl = new MemLabel();
		MemLabel posLbl = new MemLabel();
		MemLabel endLbl = new MemLabel();

		ImcStmt body = whileStmt.bodyStmt.accept(new StmtGenerator(), frames);
		ImcCJUMP cjump = new ImcCJUMP(cond, posLbl, endLbl);
        ImcJUMP jump = new ImcJUMP(condLbl);

		stmts.add(new ImcLABEL(condLbl));
        stmts.add(cjump);
        stmts.add(new ImcLABEL(posLbl));
        stmts.add(body);
        stmts.add(jump);
        stmts.add(new ImcLABEL(endLbl));

		ImcSTMTS result = new ImcSTMTS(stmts);
		ImcGen.stmtImc.put(whileStmt, result);
		return result;
	}

}
