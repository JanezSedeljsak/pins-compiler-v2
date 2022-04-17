package pins.phase.seman;

import java.util.Vector;
import pins.data.ast.visitor.*;
import pins.phase.seman.SymbTable.CannotFndNameException;
import pins.common.report.Report;
import pins.data.ast.*;

public class NameResolver<Result, Arg> extends AstFullVisitor<Result, Arg> {

    private final SymbTable symbTable = new SymbTable();
    private boolean isRoot = true;

    @Override
    @SuppressWarnings("unchecked")
    public Result visit(ASTs<? extends AST> trees, Arg arg) {
        if (isRoot) {
            isRoot = false;
            rootVisit((Vector<AstDecl>) trees.asts(), arg);
            return null;
        }

        return super.visit(trees, arg);
    }

    //region insert into SymbTable
    private Result rootVisit(Vector<AstDecl> decls, Arg arg) {
        symbTable.newScope();
        for (AstDecl decl : decls) {
            if (decl == null) continue;

            try {
                symbTable.ins(decl.name, decl);
            } catch (Exception e) {
                throw new Report.Error(decl.location, String.format("%s is already declared in scope!", decl.name));
            }
        }

        for (AstDecl decl : decls) {
            if (decl == null) continue;
            decl.accept(this, arg);
        }

        symbTable.oldScope();
        return null;
    }

    @Override
	public Result visit(AstFunDecl funDecl, Arg arg) {
        symbTable.newScope();
        for (AstParDecl decl : funDecl.pars.asts()) {
            if (decl == null) continue;

            try {
                symbTable.ins(decl.name, decl);
            } catch (Exception e) {
                throw new Report.Error(decl.location, String.format("%s is already declared in scope!", decl.name));
            }
        }

		Result res = super.visit(funDecl, arg);
        symbTable.oldScope();
		return res;
	}

    @Override
	public Result visit(AstWhereExpr whereExpr, Arg arg) {
        symbTable.newScope();
        for (AstDecl decl : whereExpr.decls.asts()) {
            if (decl == null) continue;

            try {
                symbTable.ins(decl.name, decl);
            } catch (Exception e) {
                throw new Report.Error(decl.location, String.format("%s is already declared in scope!", decl.name));
            }
        }

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
            throw new Report.Error(callExpr.location, String.format("function -> %s is not declared!", callExpr.name));
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
            throw new Report.Error(nameExpr.location, String.format("name -> %s is not declared!", nameExpr.name));
        }
        return null;
    }

    @Override
    public Result visit(AstTypeName typeName, Arg arg) {
        try {
            SemAn.declaredAt.put(typeName, symbTable.fnd(typeName.name));
        } catch (CannotFndNameException e) {
            throw new Report.Error(typeName.location, String.format("type -> %s is not declared!", typeName.name));
        }
        return null;
    }
    //endregion
}
