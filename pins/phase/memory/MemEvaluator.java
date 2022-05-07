package pins.phase.memory;

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
		} else {
			int newDepth = ctx.depth + 1;
			ctx = new FunContext();
			ctx.depth = newDepth;
		}

		if (funDecl.pars != null) {
			for (AstParDecl parDecl : funDecl.pars.asts()) {
				long parSize = SemAn.describesType.get(parDecl.type).size();
				Memory.parAccesses.put(parDecl, new MemRelAccess(parSize, ctx.parsSize, ctx.depth));
				ctx.parsSize += parSize;
			}
		}

		if (funDecl.expr != null)
			funDecl.expr.accept(this, ctx); 

		MemLabel funLabel = new MemLabel(funDecl.name);
		Memory.frames.put(funDecl, new MemFrame(funLabel, ctx.depth, ctx.locsSize, ctx.parsSize));
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
			if (varType.actualType() instanceof SemArr) {
				SemArr arrayDecl = (SemArr)(varType.actualType());

				Memory.varAccesses.put(varDecl, new MemAbsAccess(varSize, new MemLabel(varDecl.name)));
				ctx.locsSize += new SemPtr(arrayDecl).size();
			} else {
				Memory.varAccesses.put(varDecl, new MemRelAccess(varSize, -ctx.locsSize, ctx.depth));
				ctx.locsSize += varSize;
			}
		}
		
		return null;
	}
}
