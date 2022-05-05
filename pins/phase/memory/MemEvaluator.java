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

}
