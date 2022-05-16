package pins.phase.imcgen;

import java.util.*;

import pins.data.ast.*;
import pins.data.ast.visitor.*;
import pins.data.imc.code.expr.*;
import pins.data.imc.code.stmt.*;
import pins.data.mem.*;
import pins.phase.memory.Memory;
import pins.phase.seman.SemAn;

public class ExprGenerator implements AstVisitor<ImcExpr, Stack<MemFrame>> {

	// done
	@Override
	public ImcExpr visit(AstWhereExpr whereExpr, Stack<MemFrame> frames) {
		whereExpr.decls.accept(new CodeGenerator(), frames);
		ImcExpr code = whereExpr.subExpr.accept(this, frames);
		ImcGen.exprImc.put(whereExpr, code);
		return code;
	}

	// done
	@Override
	public ImcExpr visit(AstBinExpr binExpr, Stack<MemFrame> frames) {
		ImcExpr left = binExpr.fstSubExpr.accept(this, frames);
		ImcExpr right = binExpr.sndSubExpr.accept(this, frames);
		if (binExpr.oper == AstBinExpr.Oper.ARR) {
			ImcExpr expr = new ImcBINOP(ImcBINOP.Oper.ADD, left,
					new ImcBINOP(ImcBINOP.Oper.MUL, new ImcCONST(8), right));
			ImcGen.exprImc.put(binExpr, expr);
			return expr;
		}

		ImcExpr expr = new ImcBINOP(ImcBINOP.Oper.valueOf(binExpr.oper.toString()), left, right);
		ImcGen.exprImc.put(binExpr, expr);
		return expr;
	}

	// todo
	@Override
	public ImcExpr visit(AstCallExpr callExpr, Stack<MemFrame> frames) {
		if (callExpr.args != null)
			callExpr.args.accept(this, frames);
		return null;
	}

	// done hopefully
	@Override
	public ImcExpr visit(AstCastExpr castExpr, Stack<MemFrame> frames) {
		ImcExpr expr = castExpr.subExpr.accept(this, frames);
		ImcGen.exprImc.put(castExpr, expr);
		return expr;
	}

	// done
	@Override
	public ImcExpr visit(AstConstExpr constExpr, Stack<MemFrame> frames) {
		ImcExpr expr;
		switch (constExpr.kind) {
			case CHAR:
				expr = new ImcCONST((long) constExpr.name.charAt(0));
				break;
			case INT:
				expr = new ImcCONST(Long.parseLong(constExpr.name));
				break;
			default:
				expr = new ImcCONST(0);
				break;
		}

		ImcGen.exprImc.put(constExpr, expr);
		return expr;
	}

	// done hopefully
	@Override
	public ImcExpr visit(AstNameExpr nameExpr, Stack<MemFrame> arg) {
		MemFrame tempFrame = arg.peek();
		AstDecl decl = SemAn.declaredAt.get(nameExpr);

		if (decl instanceof AstParDecl) {
			MemRelAccess access = Memory.parAccesses.get(decl);
			int diff = tempFrame.depth - access.depth;
			ImcExpr temp = new ImcTEMP(tempFrame.FP);
			while (diff > 0) {
				temp = new ImcMEM(temp);
				diff--;
			}

			ImcExpr expr = new ImcMEM(new ImcBINOP(ImcBINOP.Oper.ADD, temp, new ImcCONST(access.offset)));
			ImcGen.exprImc.put(nameExpr, expr);
			return expr;
		}

		MemAccess access = Memory.varAccesses.get(decl);
		if (access instanceof MemRelAccess) {
			MemRelAccess relAccess = (MemRelAccess) access;
			int diff = tempFrame.depth - relAccess.depth;
			ImcExpr temp = new ImcTEMP(tempFrame.FP);
			while (diff > 0) {
				temp = new ImcMEM(temp);
				diff--;
			}
			ImcExpr expr = new ImcMEM(new ImcBINOP(ImcBINOP.Oper.ADD, temp, new ImcCONST(-relAccess.offset)));
			ImcGen.exprImc.put(nameExpr, expr);
			return expr;
		}

		MemAbsAccess absAccess = (MemAbsAccess) access;
		ImcExpr expr = new ImcMEM(new ImcNAME(absAccess.label));
		ImcGen.exprImc.put(nameExpr, expr);
		return expr;
	}

	// todo
	@Override
	public ImcExpr visit(AstPreExpr preExpr, Stack<MemFrame> frames) {
		ImcExpr expr = preExpr.subExpr.accept(this, frames);
		ImcExpr res = null;
		switch (preExpr.oper) {
			case NOT:
				res = new ImcUNOP(ImcUNOP.Oper.NOT, expr);
				break;
			case SUB:
				res = new ImcUNOP(ImcUNOP.Oper.NEG, expr);
				break;
			case ADD:
				res = expr;
				break;
			case NEW:
				ImcExpr[] newPars = new ImcExpr[] { new ImcCONST(0), new ImcCONST(8) };
				Vector<ImcExpr> parsVector = new Vector<>(Arrays.asList(newPars));
				res = new ImcCALL(new MemLabel("new"), new Vector<>(), parsVector);
				break;
			case DEL:
				ImcExpr[] delPars = new ImcExpr[] { new ImcCONST(0) };
				Vector<ImcExpr> delVector = new Vector<>(Arrays.asList(delPars));
				res = new ImcCALL(new MemLabel("new"), new Vector<>(), delVector);
				break;
			case PTR:
				ImcMEM mem = (ImcMEM) expr;
				res = mem.addr;
				break;
		}

		ImcGen.exprImc.put(preExpr, res);
		return res;
	}

	// done hopefully
	@Override
	public ImcExpr visit(AstPstExpr pstExpr, Stack<MemFrame> frames) {
		ImcMEM mem = (ImcMEM) pstExpr.subExpr.accept(this, frames);
		ImcExpr res = new ImcMEM(mem.addr);
		ImcGen.exprImc.put(pstExpr, res);
		return res;
	}

	// done hopefully
	@Override
	public ImcExpr visit(AstStmtExpr stmtExpr, Stack<MemFrame> frames) {
		ImcStmt stmt = stmtExpr.stmts.accept(new StmtGenerator(), frames);
		if (stmt instanceof ImcSTMTS) {
			ImcSTMTS stmts = (ImcSTMTS) stmt;
			ImcStmt lastStmt = stmts.stmts.get(stmts.stmts.size() - 1);
			if (lastStmt instanceof ImcESTMT) {
				ImcExpr res = new ImcSEXPR(stmt, ((ImcESTMT)lastStmt).expr);
				ImcGen.exprImc.put(stmtExpr, res);
				return res;
			}
		}
		ImcExpr res = new ImcSEXPR(stmt, new ImcCONST(0));
		ImcGen.exprImc.put(stmtExpr, res);
		return res;
	}

}
