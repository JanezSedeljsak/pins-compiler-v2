package pins.phase.memory;

import java.util.Vector;

import pins.data.ast.*;
import pins.data.ast.visitor.*;
import pins.data.mem.*;
import pins.data.typ.*;
import pins.phase.seman.*;

/**
 * Computing memory layout: frames and accesses.
 */
public class MemEvaluator extends AstFullVisitor<Object, MemEvaluator.FunContext> {

	/**
	 * Functional context, i.e., used when traversing function and building a new
	 * frame, parameter acceses and variable acceses.
	 */
	protected class FunContext {
		public int depth = 0;
		public long locsSize = 0;
		public long argsSize = 0;
		public long parsSize = new SemPtr(new SemVoid()).size();
	}
	
	@Override
	public Object visit(AstFunDecl funDecl, FunContext ctx) {
		boolean isGlobalFunction = ctx == null;

		if (isGlobalFunction) {
			ctx = new FunContext();
			ctx.depth = 1;
		} else {
			int newDepth = ctx.depth + 1;
			ctx = new FunContext();
			ctx.depth = newDepth;
		}

		if (funDecl.pars != null) {
			for (AstParDecl parDecl : funDecl.pars.asts()) {
				long parSize = SemAn.describesType.get(parDecl.type).size();
				Memory.parAccesses.put(parDecl, new MemRelAccess(parSize, ctx.parsSize, ctx.depth + 1));
				ctx.parsSize += parSize;
			}
		}

		if (funDecl.expr != null)
			funDecl.expr.accept(this, ctx); 

		MemLabel funLabel = isGlobalFunction ? new MemLabel(funDecl.name) : new MemLabel();
		Memory.frames.put(funDecl, new MemFrame(funLabel, ctx.depth, ctx.locsSize, ctx.argsSize));
		return null;
	}


	@Override
	public Object visit(AstCallExpr callExpr, FunContext ctx) {
		if (callExpr.args != null) {
            callExpr.args.accept(this, ctx);

            Vector<AstExpr> sentParams = callExpr.args.asts();
			long argSum = new SemPtr(new SemVoid()).size();
            for (int i = 0; i < sentParams.size(); i++) {
                long argSize = SemAn.exprOfType.get(sentParams.get(i)).size();  
				argSum += argSize;
            }

			ctx.argsSize = Math.max(ctx.argsSize, argSum);
        }

		return null;
	}

	@Override
	public Object visit(AstVarDecl varDecl, FunContext ctx) {
		SemType varType = SemAn.describesType.get(varDecl.type);
		long varSize = varType.size();
		boolean isGlobalVar = ctx == null;

		if (isGlobalVar) {
			Memory.varAccesses.put(varDecl, new MemAbsAccess(varSize, new MemLabel(varDecl.name)));
		} else {
			ctx.locsSize += varSize;
			Memory.varAccesses.put(varDecl, new MemRelAccess(varSize, -ctx.locsSize, ctx.depth + 1));
		}
		
		return null;
	}
}
