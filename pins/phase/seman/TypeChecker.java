package pins.phase.seman;

import pins.data.typ.*;

import java.util.Collection;

import pins.common.report.Report;
import pins.data.ast.*;
import pins.data.ast.AstConstExpr.Kind;
import pins.data.ast.visitor.AstFullVisitor;

public class TypeChecker extends AstFullVisitor<SemType, Object> {

    private boolean isRoot = true;

    // mostly
    private static boolean validateTwoTypes(SemType t1, SemType t2) {
        if (t1 == null || t2 == null) {
            throw new Report.InternalError();
        }

        if (t1 instanceof SemName) {
            t1 = t1.actualType();
        }

        if (t2 instanceof SemName) {
            t2 = t2.actualType();
        }

        return t1.getClass().equals(t2.getClass());
    }

    // done
    @SuppressWarnings("unchecked")
    private void visitDeclarations(ASTs<? extends AST> decls, Object arg) {
        for (AstDecl decl : (Collection<AstDecl>)decls.asts()) {
            if (decl == null || !(decl instanceof AstTypDecl)) continue;
            
            AstTypDecl typDecl = (AstTypDecl)decl;
            SemAn.declaresType.put(typDecl, new SemName(typDecl.name));
        }
    }

    // done
    private SemType visitASTs(ASTs<? extends AST> trees, Object arg) {
        SemType lastAstType = null;
        for (AST t : trees.asts()) {
            if (t != null) {
                lastAstType = t.accept(this, arg);
            }
        }
           
        return lastAstType;
    }
    

    // done
    @Override
    public SemType visit(ASTs<? extends AST> trees, Object arg) {
        if (isRoot) {
            isRoot = false;
            visitDeclarations(trees, arg);
            SemType res = visitASTs(trees, arg);
            return res;
        }

        return visitASTs(trees, arg);
    }

    // done
    @Override
	public SemType visit(AstWhereExpr whereExpr, Object arg) {
        visitDeclarations(whereExpr.decls, arg);
        SemType type = null;

		if (whereExpr.subExpr != null)
			type = whereExpr.subExpr.accept(this, arg);
		if (whereExpr.decls != null)
			whereExpr.decls.accept(this, arg);

		return type;
	}


    // mostly (nvm nek piše da bi funkcija lahka vračala sam [char, int, void, ptr] tk da morm še checkat)
    @Override
	public SemType visit(AstFunDecl funDecl, Object arg) {
        SemType exprType = null, definedType = null;

		if (funDecl.pars != null)
			funDecl.pars.accept(this, arg);
		if (funDecl.type != null)
			definedType = funDecl.type.accept(this, arg);
		if (funDecl.expr != null)
            exprType = funDecl.expr.accept(this, arg);

        if (!validateTwoTypes(definedType, exprType)) {
            throw new Report.Error(funDecl.location, String.format("Function return type (%s) does not match the return type of expression (%s)", definedType.getClass(), exprType.getClass()));
        }

		return definedType;
	}

    // mostly (sicer nvm a bi mogu to kam insertat)
	@Override
	public SemType visit(AstParDecl parDecl, Object arg) {
        SemType type = null;

		if (parDecl.type != null)
			type = parDecl.type.accept(this, arg);

		return type;
	}

    // done
	@Override
	public SemType visit(AstTypDecl typDecl, Object arg) {
		if (typDecl.type != null)
			typDecl.type.accept(this, arg);

        SemType type = typDecl.type.accept(this, arg);
        SemAn.declaresType.get(typDecl).define(type);
		return type;
	}

    // mostly (sicer nvm a bi mogu to kam insertat)
	@Override
	public SemType visit(AstVarDecl varDecl, Object arg) {
		if (varDecl.type != null)
			return varDecl.type.accept(this, arg);

		return null;
	}

	// EXPRESSIONS

    // done
	@Override
	public SemType visit(AstBinExpr binExpr, Object arg) {
        SemType t1 = null, t2 = null;

		if (binExpr.fstSubExpr != null)
			t1 = binExpr.fstSubExpr.accept(this, arg);
		if (binExpr.sndSubExpr != null)
			t2 = binExpr.sndSubExpr.accept(this, arg);

        if (!validateTwoTypes(t1, t2)) {
            throw new Report.Error(binExpr.location, String.format("Cannot use %s operator %s on type %s and %s", binExpr.oper, t1.getClass(), t2.getClass()));
        }

        SemAn.exprOfType.put(binExpr, t1);
		return t1;
	}

    // mostly (preveri št. parametrov pa mogoč še kak type idk)
	@Override
	public SemType visit(AstCallExpr callExpr, Object arg) {
        AstFunDecl decl = (AstFunDecl) SemAn.declaredAt.get(callExpr);
        SemType type = decl.type.accept(this, arg).actualType();

		if (callExpr.args != null)
			callExpr.args.accept(this, arg);

        SemAn.exprOfType.put(callExpr, type);
		return type;
	}

    // todo (tt cast je men tk čudn pač wtf so casts pr nas, če itak napou ni pointa da so tam - strukturna enakost)
	@Override
	public SemType visit(AstCastExpr castExpr, Object arg) {
        SemType t1 = null, t2 = null;

		if (castExpr.subExpr != null)
			t1 = castExpr.subExpr.accept(this, arg);
		if (castExpr.type != null)
			t2 = castExpr.type.accept(this, arg);

        if (!validateTwoTypes(t1, t2)) {
            throw new Report.Error(castExpr.location, String.format("Cannot cast variable of (type) %s to type (%s)", t1.getClass(), t2.getClass()));
        }

        SemAn.exprOfType.put(castExpr, t2);
		return null;
	}

    // done
	@Override
	public SemType visit(AstConstExpr constExpr, Object arg) {
		SemType type = null;

        switch (constExpr.kind) {
            case INT:
                type = new SemInt();
                break;
            case CHAR:
                type = new SemChar();
                break;
            case PTR:
                type = new SemPtr(new SemVoid());
                break;
            case VOID:
                type = new SemVoid();
                break;
        }

        SemAn.exprOfType.put(constExpr, type);
        return type;
	}

    // done
	@Override
	public SemType visit(AstNameExpr nameExpr, Object arg) {
		AstDecl decl = SemAn.declaredAt.get(nameExpr);
        SemType type = SemAn.declaresType.get(decl);
        if (type == null) {
            type = decl.type.accept(this, arg);
        }

        SemAn.exprOfType.put(nameExpr, type);
		return type;
	}

    // todo [TOLE JE TOTALNO NE OKEJ] sam neki sm napisu ker se mi ni dalo do konca
	@Override
	public SemType visit(AstPreExpr preExpr, Object arg) {
        SemType type = null, resultType = null;

		if (preExpr.subExpr != null)
			type = preExpr.subExpr.accept(this, arg);
        
        switch (preExpr.oper) {
            case NEW:
                resultType = new SemPtr(new SemVoid());
                break;
            case DEL:
                resultType = new SemVoid();
                break;
            case NOT:
                resultType = type;
                break;    
            case ADD:
                resultType = type;
                break;
            case SUB:
                resultType = type;
                break;
            case PTR:   
                resultType = new SemPtr(new SemVoid());
                break;
        }

        SemAn.exprOfType.put(preExpr, resultType);
		return resultType;
	}

    // mostly (lahka bi blo če mamo nek expression ko je actualtype pointer pol se mamo fajn)
	@Override
	public SemType visit(AstPstExpr pstExpr, Object arg) {
        SemType type = null;

		if (pstExpr.subExpr != null)
			type = pstExpr.subExpr.accept(this, arg);

        SemType actualType = type.actualType();
        if (!(actualType instanceof SemPtr)) {
            throw new Report.Error(pstExpr.location, String.format("Cannot get value at address because type is %s", actualType.getClass()));
        }

        SemType pointerOfType = ((SemPtr)actualType).baseType;
        SemAn.exprOfType.put(pstExpr, pointerOfType);
		return pointerOfType;
	}

    // done
	@Override
	public SemType visit(AstStmtExpr stmtExpr, Object arg) {
        SemType type = null;

		if (stmtExpr.stmts != null)
			type = stmtExpr.stmts.accept(this, arg);

        SemAn.exprOfType.put(stmtExpr, type);
		return type;
	}

	// STATEMENTS

    // mostly (nvm če je čist okej assign ker pač deluje prek actualtype)
	@Override
	public SemType visit(AstAssignStmt assignStmt, Object arg) {
        SemType t1 = null, t2 = null;

		if (assignStmt.fstSubExpr != null)
			t1 = assignStmt.fstSubExpr.accept(this, arg);
		if (assignStmt.sndSubExpr != null)
			t2 = assignStmt.sndSubExpr.accept(this, arg);

        if (!validateTwoTypes(t1, t2)) {
            throw new Report.Error(assignStmt.location, String.format("Cannot assign type %s to variable of type %s", t2.getClass(), t1.getClass()));
        }

        SemAn.stmtOfType.put(assignStmt, new SemVoid());
		return new SemVoid();
	}

    // done
	@Override
	public SemType visit(AstExprStmt exprStmt, Object arg) {
        SemType type = null;

		if (exprStmt.expr != null)
			type = exprStmt.expr.accept(this, arg);

        SemAn.stmtOfType.put(exprStmt, type);
		return type;
	}

    // done
	@Override
	public SemType visit(AstIfStmt ifStmt, Object arg) {
        SemType condition = null;

		if (ifStmt.condExpr != null)
			condition = ifStmt.condExpr.accept(this, arg);
		if (ifStmt.thenBodyStmt != null)
			ifStmt.thenBodyStmt.accept(this, arg);
		if (ifStmt.elseBodyStmt != null)
			ifStmt.elseBodyStmt.accept(this, arg);

        if (condition == null || !(condition.actualType() instanceof SemInt)) {
            throw new Report.Error(ifStmt.condExpr.location, "Condition expression in IF stmt should be of type INT");
        }

        SemAn.stmtOfType.put(ifStmt, new SemVoid());
		return new SemVoid();
	}

    // done
	@Override
	public SemType visit(AstWhileStmt whileStmt, Object arg) {
        SemType condition = null;

		if (whileStmt.condExpr != null)
			condition = whileStmt.condExpr.accept(this, arg);
		if (whileStmt.bodyStmt != null)
			whileStmt.bodyStmt.accept(this, arg);

        if (condition == null || !(condition.actualType() instanceof SemInt)) {
            throw new Report.Error(whileStmt.condExpr.location, "Condition expression in WHILE stmt should be of type INT");
        }
        
        SemAn.stmtOfType.put(whileStmt, new SemVoid());
		return new SemVoid();
	}

	// TYPES

    // mostly (na tto funckijo sm kr proud ker pomoje fajn checkat stvar)
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

		SemType type = new SemArr(elemType, Long.parseLong(expr.name));
        SemAn.describesType.put(arrType, type);
        return type;
	}

    // done
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

    // done
	@Override
	public SemType visit(AstPtrType ptrType, Object arg) {
		SemType baseType = null;

        if (ptrType.subType != null)
            baseType = ptrType.subType.accept(this, arg);

        SemType type =  new SemPtr(baseType);
        SemAn.describesType.put(ptrType, type);
        return type;
	}

    // done
	@Override
	public SemType visit(AstTypeName typeName, Object arg) {
		AstDecl decl = SemAn.declaredAt.get(typeName);
        SemType type = SemAn.declaresType.get(decl);
        SemAn.describesType.put(typeName, type);
		return type;
	}
}