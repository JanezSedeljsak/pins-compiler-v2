package pins.phase.seman;

import pins.data.typ.*;

import java.util.*;

import pins.common.report.Report;
import pins.data.ast.*;
import pins.data.ast.AstConstExpr.Kind;
import pins.data.ast.visitor.AstFullVisitor;

public class TypeChecker extends AstFullVisitor<SemType, Object> {

    private boolean isRoot = true;
  
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

        boolean isBaseEqual = t1.getClass().equals(t2.getClass());
        if (isBaseEqual && t1 instanceof SemArr) {
            SemArr arr1 = (SemArr)t1, arr2 = (SemArr)t2;
            boolean arrElemTypeEqual = validateTwoTypes(arr1.elemType, arr2.elemType);
            return arrElemTypeEqual && arr1.numElems == arr2.numElems;
        }

        if (isBaseEqual && t1 instanceof SemPtr) {
            SemPtr ptr1 = (SemPtr)t1, ptr2 = (SemPtr)t2;
            return validateTwoTypes(ptr1.baseType, ptr2.baseType);
        }

        return isBaseEqual;
    }
   
    private static String stype(SemType type) {
        type = type.actualType();
        if (type instanceof SemArr) {
            SemArr arrType = (SemArr)type;
            return String.format("[%d]%s", arrType.numElems, stype(arrType.elemType));
        }

        if (type instanceof SemChar) return String.format("CHAR");
        if (type instanceof SemInt) return String.format("INT");
        if (type instanceof SemPtr) {
            SemPtr ptrType = (SemPtr)type;
            return String.format("^%s", stype(ptrType.baseType));
        }

        if (type instanceof SemVoid) return String.format("VOID");
        return "/";
    }

    
    private static boolean validateType(SemType type) {
        Set<String> seenNames = new HashSet<>();
        if (type instanceof SemName) {
            SemName nameDef = null;
            while (type instanceof SemName) {
                nameDef = (SemName)type;
                if (seenNames.contains(nameDef.name)) {
                    return false;
                }

                seenNames.add(nameDef.name);
                type = nameDef.type();
            }

            return true;
        }

        return true;
    }

    enum TypeHandlePhase {
        ADD, DEFINE, VALIDATE
    };
    
    @SuppressWarnings("unchecked")
    private void handleTypes(ASTs<? extends AST> decls, Object arg) {
        Vector<AstDecl> declerationVector = (Vector<AstDecl>)decls.asts();
        handleTypesWithPhase(declerationVector, TypeHandlePhase.ADD);
        handleTypesWithPhase(declerationVector, TypeHandlePhase.DEFINE);
        handleTypesWithPhase(declerationVector, TypeHandlePhase.VALIDATE);
    }
    
    private void handleTypesWithPhase(Vector<AstDecl> decls, TypeHandlePhase phase) {
        for (AstDecl decl : decls) {
            if (decl == null || !(decl instanceof AstTypDecl)) continue;
            
            AstTypDecl typDecl = (AstTypDecl)decl;
            switch (phase) {
                case ADD:
                    SemAn.declaresType.put(typDecl, new SemName(typDecl.name));
                    break;
                case DEFINE:
                    SemType type = typDecl.type.accept(this, null);
                    SemAn.declaresType.get(typDecl).define(type);
                    break;
                case VALIDATE:
                    type = typDecl.type.accept(this, null);
                    if (!validateType(type)) {
                        throw new Report.Error(typDecl.location, String.format("Type decleration of (%s) has an inifinite loop in the definition", typDecl.name));
                    }
                    break;
            }
        }
    }
    
    private SemType visitASTs(ASTs<? extends AST> trees, Object arg) {
        SemType lastAstType = null;
        for (AST t : trees.asts()) {
            if (t != null) {
                lastAstType = t.accept(this, arg);
            }
        }
           
        return lastAstType;
    }
    
    @Override
    public SemType visit(ASTs<? extends AST> trees, Object arg) {
        if (isRoot) {
            isRoot = false;
            handleTypes(trees, arg);
            SemType res = visitASTs(trees, arg);
            return res;
        }

        return visitASTs(trees, arg);
    }
    
    @Override
	public SemType visit(AstWhereExpr whereExpr, Object arg) {
        handleTypes(whereExpr.decls, arg);
        SemType type = null;

        if (whereExpr.decls != null)
			whereExpr.decls.accept(this, arg);
		if (whereExpr.subExpr != null)
			type = whereExpr.subExpr.accept(this, arg);
		

        SemAn.exprOfType.put(whereExpr, type);
		return type;
	}
    
    @Override
	public SemType visit(AstFunDecl funDecl, Object arg) {
        SemType exprType = null, definedType = null;

		if (funDecl.pars != null)
			funDecl.pars.accept(this, arg);
		if (funDecl.type != null)
			definedType = funDecl.type.accept(this, arg);
		if (funDecl.expr != null)
            exprType = funDecl.expr.accept(this, arg);

        if (definedType.actualType() instanceof SemArr) {
            throw new Report.Error("Function can not return type Array");
        }

        if (!validateTwoTypes(definedType, exprType)) {
            throw new Report.Error(funDecl.location, String.format("Function return missmatch expected %s got %s", stype(definedType), stype(exprType)));
        }
        
		return definedType;
	}
    
	@Override
	public SemType visit(AstParDecl parDecl, Object arg) {
        SemType type = null;

		if (parDecl.type != null)
			type = parDecl.type.accept(this, arg);

        if (type.actualType() instanceof SemArr || type.actualType() instanceof SemVoid) {
            throw new Report.Error(parDecl.location, "Function params of type ARR or VOID are not allowed");
        }

		return type;
	}
    
	@Override
	public SemType visit(AstTypDecl typDecl, Object arg) {
		if (typDecl.type != null)
			return typDecl.type.accept(this, arg);

        return null;
	}
    
	@Override
	public SemType visit(AstVarDecl varDecl, Object arg) {
		if (varDecl.type != null)
			return varDecl.type.accept(this, arg);

		return null;
	}

	// EXPRESSIONS
    
	@Override
	public SemType visit(AstBinExpr binExpr, Object arg) {
        SemType t1 = null, t2 = null;

		if (binExpr.fstSubExpr != null)
			t1 = binExpr.fstSubExpr.accept(this, arg);
		if (binExpr.sndSubExpr != null)
			t2 = binExpr.sndSubExpr.accept(this, arg);

        switch (binExpr.oper) {
            case EQU:
            case NEQ:
            case LTH:
            case GTH:
            case LEQ:
            case GEQ:
                if (t1.actualType() instanceof SemArr || t2.actualType() instanceof SemArr) {
                    throw new Report.Error(binExpr.location, String.format("Operation %s in not supported for Arrays (got %s and %s)", binExpr.oper.name(), stype(t1), stype(t2)));
                }
                break;
            case AND:
            case OR:
            case MUL:
            case DIV:
            case MOD:
            case ADD:
            case SUB:
                if (!(t1.actualType() instanceof SemInt && t2.actualType() instanceof SemInt)) {
                    throw new Report.Error(binExpr.location, String.format("Operation %s in not defined on types %s and %s", binExpr.oper.name(), stype(t1), stype(t2)));
                }
                break;
            case ARR:
                if (!(t1.actualType() instanceof SemArr) || !(t2.actualType() instanceof SemInt)) {
                    throw new Report.Error(binExpr.location, String.format("Expected INT[] got %s[%s]", stype(t1), stype(t2)));
                }

                SemArr array = (SemArr)t1.actualType();
                SemAn.exprOfType.put(binExpr, array.elemType);
                return array.elemType;
        }

        if (binExpr.oper != AstBinExpr.Oper.ARR && !validateTwoTypes(t1, t2)) {
            throw new Report.Error(binExpr.location, String.format("Cannot join types %s and %s", stype(t1), stype(t2)));
        }

        SemAn.exprOfType.put(binExpr, t1);
		return t1;
	}

	@Override
	public SemType visit(AstCallExpr callExpr, Object arg) {
        AstDecl baseDecl = SemAn.declaredAt.get(callExpr);
        if (!(baseDecl instanceof AstFunDecl)) {
            throw new Report.Error(callExpr.location, String.format("%s is not a function", baseDecl.name));
        }

        AstFunDecl decl = (AstFunDecl) baseDecl;
        SemType type = decl.type.accept(this, arg);
        if (type.actualType() instanceof SemArr) {
            throw new Report.Error("Function can not return type Array");
        }

		if (callExpr.args != null) {
            callExpr.args.accept(this, arg);

            Vector<AstExpr> sentParams = callExpr.args.asts();
            Vector<AstParDecl> declaredParams = decl.pars.asts();
            if (sentParams.size() != declaredParams.size()) {
                throw new Report.Error(callExpr.location, String.format("Passed arguments missmatch (expected %d, got %d)", declaredParams.size(), sentParams.size()));
            }

            for (int i = 0; i < sentParams.size(); i++) {
                SemType sentType = SemAn.exprOfType.get(sentParams.get(i));
                SemType requiredParamType =  declaredParams.get(i).accept(this, arg);
                if (!validateTwoTypes(requiredParamType, sentType)) {
                    throw new Report.Error(callExpr.location, String.format("[%d-th] param should be of type %s got %s!", i, stype(requiredParamType), stype(sentType)));
                }
            }
        }
			

        SemAn.exprOfType.put(callExpr, type);
		return type;
	}

	@Override
	public SemType visit(AstCastExpr castExpr, Object arg) {
        SemType t1 = null, t2 = null;

		if (castExpr.subExpr != null)
			t1 = castExpr.subExpr.accept(this, arg);
		if (castExpr.type != null)
			t2 = castExpr.type.accept(this, arg);

        SemType t1Actual = t1.actualType();
        SemType t2Actual = t2.actualType();

        if (!(t1Actual instanceof SemInt || t1Actual instanceof SemChar || t1Actual instanceof SemPtr)) {
            throw new Report.Error(castExpr.location, String.format("Expression of type cannot be cast %s!", stype(t1)));
        }

        if (!(t2Actual instanceof SemInt || t2Actual instanceof SemChar || t2Actual instanceof SemPtr)) {
            throw new Report.Error(castExpr.location, String.format("Cast result of type %s is not allowed!", stype(t2)));
        }

        SemAn.exprOfType.put(castExpr, t2);
		return t2;
	}
    
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
    
	@Override
	public SemType visit(AstPreExpr preExpr, Object arg) {
        SemType type = null, resultType = null;

		if (preExpr.subExpr != null)
			type = preExpr.subExpr.accept(this, arg);
        
        switch (preExpr.oper) {
            case NEW:
                if (!(type.actualType() instanceof SemInt)) {
                    throw new Report.Error(preExpr.location, String.format("Type when using [new] has to be INT got %s", stype(type)));
                }
                resultType = new SemPtr(new SemVoid());
                break;
            case DEL:
                if (!(type.actualType() instanceof SemPtr)) {
                    throw new Report.Error(preExpr.location, String.format("DEL is only allowed on pointers got (%s)", stype(type)));
                }
                resultType = new SemVoid();
                break;
            case NOT:
            case ADD:
            case SUB:
                if (!(type.actualType() instanceof SemInt)) {
                    throw new Report.Error(preExpr.location, String.format("Operation %s is only allowed on INTs", preExpr.oper.name()));
                }
                resultType = type;
                break;
            case PTR:
                resultType = new SemPtr(type);
                break;
        }

        SemAn.exprOfType.put(preExpr, resultType);
		return resultType;
	}
    
	@Override
	public SemType visit(AstPstExpr pstExpr, Object arg) {
        SemType type = null;

		if (pstExpr.subExpr != null)
			type = pstExpr.subExpr.accept(this, arg);

        SemType actualType = type.actualType();
        if (!(actualType instanceof SemPtr)) {
            throw new Report.Error(pstExpr.location, String.format("Cannot get value at address because type is %s (expected PTR)", stype(actualType)));
        }

        SemType pointerOfType = ((SemPtr)actualType).baseType;
        SemAn.exprOfType.put(pstExpr, pointerOfType);
		return pointerOfType;
	}
    
	@Override
	public SemType visit(AstStmtExpr stmtExpr, Object arg) {
        SemType type = null;

		if (stmtExpr.stmts != null)
			type = stmtExpr.stmts.accept(this, arg);

        SemAn.exprOfType.put(stmtExpr, type);
		return type;
	}

	// STATEMENTS
    
	@Override
	public SemType visit(AstAssignStmt assignStmt, Object arg) {
        SemType t1 = null, t2 = null;

		if (assignStmt.fstSubExpr != null)
			t1 = assignStmt.fstSubExpr.accept(this, arg);
		if (assignStmt.sndSubExpr != null)
			t2 = assignStmt.sndSubExpr.accept(this, arg);

        if (t1.actualType() instanceof SemArr || t1.actualType() instanceof SemVoid) {
            throw new Report.Error(assignStmt.location, String.format("You can only assign to PTR, INT, CHAR got %s", stype(t1)));
        }

        if (t2.actualType() instanceof SemArr || t2.actualType() instanceof SemVoid) {
            throw new Report.Error(assignStmt.location, String.format("You can only assign type PTR, INT, CHAR got %s", stype(t2)));
        }

        if (!validateTwoTypes(t1, t2)) {
            throw new Report.Error(assignStmt.location, String.format("Cannot assign type %s to variable of type %s", stype(t2), stype(t1)));
        }

        SemAn.stmtOfType.put(assignStmt, new SemVoid());
		return new SemVoid();
	}
    
	@Override
	public SemType visit(AstExprStmt exprStmt, Object arg) {
        SemType type = null;

		if (exprStmt.expr != null)
			type = exprStmt.expr.accept(this, arg);

        SemAn.stmtOfType.put(exprStmt, type);
		return type;
	}
    
	@Override
	public SemType visit(AstIfStmt ifStmt, Object arg) {
        SemType condition = null;

		if (ifStmt.condExpr != null)
			condition = ifStmt.condExpr.accept(this, arg);

		if (ifStmt.thenBodyStmt != null) {
            SemType type = ifStmt.thenBodyStmt.accept(this, arg);
            if (!(type.actualType() instanceof SemVoid)) {
                throw new Report.Error(ifStmt.thenBodyStmt.location, String.format("stmts in IF block have to be type VOID got %s", stype(type)));
            }
        }

		if (ifStmt.elseBodyStmt != null) {
            SemType type = ifStmt.elseBodyStmt.accept(this, arg);
            if (!(type.actualType() instanceof SemVoid)) {
                throw new Report.Error(ifStmt.elseBodyStmt.location, String.format("stmts in ELSE block have to be type VOID got %s", stype(type)));
            }
        }

        if (condition == null || !(condition.actualType() instanceof SemInt)) {
            throw new Report.Error(ifStmt.condExpr.location, "Condition expression in IF stmt should be of type INT");
        }

        SemAn.stmtOfType.put(ifStmt, new SemVoid());
		return new SemVoid();
	}
    
	@Override
	public SemType visit(AstWhileStmt whileStmt, Object arg) {
        SemType condition = null;

		if (whileStmt.condExpr != null)
			condition = whileStmt.condExpr.accept(this, arg);
		if (whileStmt.bodyStmt != null) {
            SemType type = whileStmt.bodyStmt.accept(this, arg);
            if (!(type.actualType() instanceof SemVoid)) {
                throw new Report.Error(whileStmt.bodyStmt.location, String.format("stmts in WHILE block have to be type VOID got %s", stype(type)));
            }
        }

        if (condition == null || !(condition.actualType() instanceof SemInt)) {
            throw new Report.Error(whileStmt.condExpr.location, "Condition expression in WHILE stmt should be of type INT");
        }
        
        SemAn.stmtOfType.put(whileStmt, new SemVoid());
		return new SemVoid();
	}

	// TYPES
    
	@Override
	public SemType visit(AstArrType arrType, Object arg) {
        SemType elemType = null;
        String constExprError = "Array must be defined with size - [INT_CONST > 0]!";

		if (arrType.elemType != null)
			elemType = arrType.elemType.accept(this, arg);
		if (arrType.size != null)
            arrType.size.accept(this, arg);
        
        if (elemType.actualType() instanceof SemVoid) {
            throw new Report.Error(arrType.location, "Array does not support items of type VOID");
        }

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
	public SemType visit(AstPtrType ptrType, Object arg) {
		SemType baseType = null;

        if (ptrType.subType != null)
            baseType = ptrType.subType.accept(this, arg);

        SemType type =  new SemPtr(baseType);
        SemAn.describesType.put(ptrType, type);
        return type;
	}
    
	@Override
	public SemType visit(AstTypeName typeName, Object arg) {
		AstDecl decl = SemAn.declaredAt.get(typeName);
        SemType type = SemAn.declaresType.get(decl);
        SemAn.describesType.put(typeName, type);
		return type;
	}
}