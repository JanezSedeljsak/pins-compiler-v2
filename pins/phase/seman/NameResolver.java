package pins.phase.seman;

import java.util.Collection;
import pins.data.ast.visitor.*;
import pins.phase.seman.SymbTable.CannotFndNameException;
import pins.common.report.Report;
import pins.data.ast.*;

public class NameResolver<Result, Arg> extends AstFullVisitor<Result, Arg> {

    private final SymbTable symbTable = new SymbTable();
    private boolean isRoot = true;
    
    @SuppressWarnings("unchecked")
    private void visitDeclarations(ASTs<? extends AST> decls, Arg arg) {
        for (AstDecl decl : (Collection<AstDecl>)decls.asts()) {
            if (decl == null) continue;

            try {
                symbTable.ins(decl.name, decl);
            } catch (Exception e) {
                throw new Report.Error(decl.location, String.format("%s is already declared in this scope", decl.name));
            }
        }
    }

    //region insert into SymbTable
    @Override
    public Result visit(ASTs<? extends AST> trees, Arg arg) {
        if (isRoot) {
            isRoot = false;
            symbTable.newScope();
            visitDeclarations(trees, arg);
            Result res = super.visit(trees, arg);
            symbTable.oldScope();
            return res;
        }

        return super.visit(trees, arg);
    }

    @Override
	public Result visit(AstFunDecl funDecl, Arg arg) {
        symbTable.newScope();
        visitDeclarations(funDecl.pars, arg);
		Result res = super.visit(funDecl, arg);
        symbTable.oldScope();
		return res;
	}

    @Override
	public Result visit(AstWhereExpr whereExpr, Arg arg) {
        symbTable.newScope();
        visitDeclarations(whereExpr.decls, arg);
		Result res = super.visit(whereExpr, arg);
        symbTable.oldScope();
		return res;
	}
    //endregion

    //region insert into DeclaredAt
    @Override
    public Result visit(AstCallExpr callExpr, Arg arg) {
        try {
            SemAn.declaredAt.put(callExpr, symbTable.fnd(callExpr.name));
        } catch (CannotFndNameException e) {
            throw new Report.Error(callExpr.location, String.format("function %s was not declared in this scope!", callExpr.name));
        }

        if (callExpr.args != null)
            callExpr.args.accept(this, arg);
        return null;
    }

    @Override
    public Result visit(AstNameExpr nameExpr, Arg arg) {
        try {
            SemAn.declaredAt.put(nameExpr, symbTable.fnd(nameExpr.name));
        } catch (CannotFndNameException e) {
            throw new Report.Error(nameExpr.location, String.format("variable %s was not declared in this scope!", nameExpr.name));
        }
        return null;
    }

    @Override
    public Result visit(AstTypeName typeName, Arg arg) {
        try {
            SemAn.declaredAt.put(typeName, symbTable.fnd(typeName.name));
        } catch (CannotFndNameException e) {
            throw new Report.Error(typeName.location, String.format("type %s was not declared in this scope!", typeName.name));
        }
        return null;
    }
    //endregion
}
