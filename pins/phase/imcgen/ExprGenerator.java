package pins.phase.imcgen;

import java.util.*;

import pins.data.ast.*;
import pins.data.ast.visitor.*;
import pins.data.imc.code.expr.*;
import pins.data.imc.code.stmt.*;
import pins.data.mem.*;
import pins.data.typ.*;
import pins.phase.memory.Memory;
import pins.phase.seman.SemAn;

public class ExprGenerator implements AstVisitor<ImcExpr, Stack<MemFrame>> {

	@Override
	public ImcExpr visit(ASTs<? extends AST> trees, Stack<MemFrame> frames) {
		for (AST t : trees.asts())
			if (t != null)
				t.accept(this, frames);
		return null;
	}

	@Override
	public ImcExpr visit(AstWhereExpr whereExpr, Stack<MemFrame> frames) {
		whereExpr.decls.accept(new CodeGenerator(), frames);
		ImcExpr code = whereExpr.subExpr.accept(this, frames);
		ImcGen.exprImc.put(whereExpr, code);
		return code;
	}

	@Override
	public ImcExpr visit(AstBinExpr binExpr, Stack<MemFrame> frames) {
		ImcExpr left = binExpr.fstSubExpr.accept(this, frames);
        ImcExpr right = binExpr.sndSubExpr.accept(this, frames);

		if (binExpr.oper == AstBinExpr.Oper.ARR) {
			ImcCONST itemSize = new ImcCONST(SemAn.exprOfType.get(binExpr).size());

			ImcBINOP add = new ImcBINOP(ImcBINOP.Oper.ADD, left, new ImcBINOP(ImcBINOP.Oper.MUL, right, itemSize));
			ImcMEM expr = new ImcMEM(add);
			ImcGen.exprImc.put(binExpr, expr);
			return expr;
		}
		
		ImcExpr expr = new ImcBINOP(ImcBINOP.Oper.valueOf(binExpr.oper.toString()), left, right);
        ImcGen.exprImc.put(binExpr, expr);
        return expr;
	}

	@Override
	public ImcExpr visit(AstCallExpr callExpr, Stack<MemFrame> frames) {
		if (callExpr.args != null)
			callExpr.args.accept(this, frames);

		AstFunDecl decl = (AstFunDecl)SemAn.declaredAt.get(callExpr);
		MemFrame funcFrame = Memory.frames.get(decl);

		Vector<ImcExpr> parsVector = new Vector<>();
		Vector<Long> offsets = new Vector<>();
		long offset = new SemPtr(new SemVoid()).size();

		// parent FP
		ImcExpr slArg = new ImcTEMP(frames.peek().FP);
		int diff = frames.peek().depth - funcFrame.depth;
		for (int i = 0; i <= diff; i++) {
			slArg = new ImcMEM(slArg);
		}

		parsVector.add(slArg);
		offsets.add(offset);
		
		for (AST arg: callExpr.args.asts()) {
			ImcExpr argExpr = ImcGen.exprImc.get(arg);
			parsVector.add(argExpr);
			offset += SemAn.exprOfType.get(arg).size();
		}

		ImcCALL expr = new ImcCALL(funcFrame.label, offsets, parsVector);
		ImcGen.exprImc.put(callExpr, expr);
		return expr;
	}

	@Override
	public ImcExpr visit(AstCastExpr castExpr, Stack<MemFrame> frames) {
		ImcExpr expr = castExpr.subExpr.accept(this, frames);
		ImcGen.exprImc.put(castExpr, expr);
		return expr;
	}

	@Override
	public ImcExpr visit(AstConstExpr constExpr, Stack<MemFrame> frames) {
		ImcExpr expr;
		switch (constExpr.kind) {
			case CHAR:
				String charWrapper = constExpr.name;
				expr = new ImcCONST((long) charWrapper.charAt(charWrapper.length() == 3 ? 1 : 2));
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

	@Override
	public ImcExpr visit(AstNameExpr nameExpr, Stack<MemFrame> arg) {
		MemFrame tempFrame = arg.peek();
		AstDecl decl = SemAn.declaredAt.get(nameExpr);

		if (decl instanceof AstParDecl) {
			MemRelAccess access = Memory.parAccesses.get(decl);
			int diff = tempFrame.depth - access.depth;
			ImcExpr temp = new ImcTEMP(tempFrame.FP);
			while (diff >= 0) {
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
			while (diff >= 0) {
				temp = new ImcMEM(temp);
				diff--;
			}
			ImcExpr expr = new ImcMEM(new ImcBINOP(ImcBINOP.Oper.ADD, temp, new ImcCONST(relAccess.offset)));
			ImcGen.exprImc.put(nameExpr, expr);
			return expr;
		}

		MemAbsAccess absAccess = (MemAbsAccess) access;
		ImcExpr expr = new ImcMEM(new ImcNAME(absAccess.label));
		ImcGen.exprImc.put(nameExpr, expr);
		return expr;
	}

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
				ImcExpr[] newPars = new ImcExpr[] { new ImcTEMP(frames.peek().FP), expr };
				Long[] offsets = new Long[] { 0L, 8L };

				Vector<ImcExpr> parsVector = new Vector<>(Arrays.asList(newPars));
				res = new ImcCALL(new MemLabel("new"), new Vector<>(Arrays.asList(offsets)), parsVector);
				break;
			case DEL:
				ImcExpr[] delPars = new ImcExpr[] { new ImcTEMP(frames.peek().FP), expr };
				Vector<ImcExpr> delVector = new Vector<>(Arrays.asList(delPars));
				offsets = new Long[] { 0L, 8L };

				res = new ImcCALL(new MemLabel("del"), new Vector<>(Arrays.asList(offsets)), delVector);
				break;
			case PTR:
				ImcMEM mem = (ImcMEM)expr;
				ImcGen.exprImc.put(preExpr, mem.addr);
				return mem.addr;
		}

		ImcGen.exprImc.put(preExpr, res);
		return res;
	}

	@Override
	public ImcExpr visit(AstPstExpr pstExpr, Stack<MemFrame> frames) {
		ImcExpr mem = pstExpr.subExpr.accept(this, frames);
		ImcExpr res = new ImcMEM(mem);
		ImcGen.exprImc.put(pstExpr, res);
		return res;
	}

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
