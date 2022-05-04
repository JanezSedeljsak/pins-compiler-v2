package pins.phase.seman;

import pins.data.typ.*;

import java.util.Collection;

import pins.common.report.Report;
import pins.data.ast.*;
import pins.data.ast.AstConstExpr.Kind;
import pins.data.ast.visitor.AstFullVisitor;

public class TypeChecker_Old extends AstFullVisitor<SemType, Object> {

    private boolean isRoot = true;

    @SuppressWarnings("unchecked")
    private void visitDeclarations(ASTs<? extends AST> decls, Object arg) {
        for (AstDecl decl : (Collection<AstDecl>)decls.asts()) {
            if (decl == null || !(decl instanceof AstTypDecl)) continue;
            
            AstTypDecl typDecl = (AstTypDecl)decl;
            SemAn.declaresType.put(typDecl, new SemName(typDecl.name));
        }
    }

    @Override
    public SemType visit(ASTs<? extends AST> trees, Object arg) {
        if (isRoot) {
            isRoot = false;
            visitDeclarations(trees, arg);
            SemType res = super.visit(trees, arg);
            return res;
        }

        return super.visit(trees, arg);
    }

    @Override
	public SemType visit(AstWhereExpr whereExpr, Object arg) {
        visitDeclarations(whereExpr.decls, arg);
		SemType res = super.visit(whereExpr, arg);
		return res;
	}

    @Override
	public SemType visit(AstTypDecl typDecl, Object arg) {
		if (typDecl.type != null)
			typDecl.type.accept(this, arg);

        SemType type = typDecl.type.accept(this, arg);
        SemAn.declaresType.get(typDecl).define(type);
		return type;
	}

    @Override
	public SemType visit(AstVarDecl varDecl, Object arg) {
		if (varDecl.type != null)
			return varDecl.type.accept(this, arg);

		return null;
	}

    @Override
	public SemType visit(AstAtomType atomType, Object arg) {
        SemType type = null;

		switch (atomType.kind) {
            case INT:
                type = new SemInt();
                break;
            case VOID:
                type = new SemVoid();
                break;
            case CHAR:
                type = new SemChar();
                break;
        }

        SemAn.describesType.put(atomType, type);
        return type;
	}

    @Override
	public SemType visit(AstTypeName typeName, Object arg) {
		AstTypDecl decl = (AstTypDecl) SemAn.declaredAt.get(typeName);
        SemType type = SemAn.declaresType.get(decl);
        SemAn.describesType.put(typeName, type);
		return type;
	}

    @Override
	public SemType visit(AstFunDecl funDecl, Object arg) {
        SemType type = null;

		if (funDecl.pars != null)
			funDecl.pars.accept(this, arg);
		if (funDecl.type != null)
			type = funDecl.type.accept(this, arg);
		if (funDecl.expr != null)
			funDecl.expr.accept(this, arg);

		return type;
	}

    @Override
	public SemType visit(AstCallExpr callExpr, Object arg) {
        AstFunDecl decl = (AstFunDecl) SemAn.declaredAt.get(callExpr);
        SemType type = decl.type.accept(this, arg).actualType();

		if (callExpr.args != null)
			callExpr.args.accept(this, arg);

        SemAn.exprOfType.put(callExpr, type);
		return type;
	}

    @Override
	public SemType visit(AstArrType arrType, Object arg) {
        SemType elemType = null;
        String constExprError = "Array must be defined with size - [INT_CONST > 0]!";

		if (arrType.elemType != null)
			elemType = arrType.elemType.accept(this, arg);
		if (arrType.size != null)
            arrType.size.accept(this, arg);

        if (!(arrType.size instanceof AstConstExpr)) {
            throw new Report.Error(arrType.location, constExprError);
        }

        AstConstExpr expr = (AstConstExpr)arrType.size;
        if (expr.kind != Kind.INT || Long.parseLong(expr.name) <= 0) {
            throw new Report.Error(expr.location, constExprError);
        }

		return new SemArr(elemType, Long.parseLong(expr.name));
	}
}
