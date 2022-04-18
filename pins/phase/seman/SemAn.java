package pins.phase.seman;

import java.util.*;

import pins.common.report.Location;
import pins.data.ast.*;

public class SemAn implements AutoCloseable {

	/** Maps names to declarations. */
	public static final HashMap<AstName, AstDecl> declaredAt = new HashMap<AstName, AstDecl>();

	public SemAn() {
	}
	
	public void close() {
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		Vector<String> output = new Vector<>();

		for (AstName astName : SemAn.declaredAt.keySet()) {
			Location loc = new Location(-1, -1);
			String name = "";

			if (astName instanceof AstCallExpr) {
				AstCallExpr temp = (AstCallExpr)astName;
				loc = temp.location;
				name = temp.name;
			} else if (astName instanceof AstNameExpr) {
				AstNameExpr temp = (AstNameExpr)astName;
				loc = temp.location;
				name = temp.name;
			} else if (astName instanceof  AstTypeName) {
				AstTypeName temp = (AstTypeName)astName;
				loc = temp.location;
				name = temp.name;
			}

			AstDecl decl = SemAn.declaredAt.get(astName);
			output.add(String.format("\033[1m(%-5s)\033[0m @(%-9s) => \033[1m(%-5s)\033[0m @(%-9s)\n", name, loc, decl.name, decl.location));
		}

		Collections.sort(output);
		output.forEach(str -> sb.append(str));
		return sb.toString();
	}

}
