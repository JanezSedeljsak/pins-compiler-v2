package pins.phase.synan;

import pins.phase.lexan.*;
import pins.data.symbol.*;
import pins.common.report.*;
import pins.data.ast.*;
import java.util.Vector;

public class SynAn implements AutoCloseable {

	private final LexAn lexan;
	private Symbol current = null, prevSymb = null;
	private AST ast = null;
	private boolean dontMove = false;

	public SynAn(LexAn lexan) {
		this.lexan = lexan;
	}

	public void close() {
		lexan.close();
	}

	public AST parser() {
		parseSource();
		if (peek() != null && peek().token == Token.EOF) {
			return ast;
		}

		throw new Report.Error(peek().location, String.format("Unexpected token at end of program: %s, expected: EOF", peek().lexeme()));
	}

	public Symbol peek() {
		if (current == null) {
			throw new Report.Error("Unexpected EOF (code not finished properly)");
		}
		return current;
	}

	public void move() {
		if (dontMove) {
			dontMove = false;
		} else {
			prevSymb = current;
			current = lexan.lexer();
		}
	}

	private Symbol checkExpected(Token token) {
		move();
		if (peek().token.equals(token)) {
			return peek();
		} else {
			String err = String.format("Unexpected token: \"%s\" after: \"%s\" - expected \"%s\"", peek().lexeme(), prevSymb.lexeme(), token.str());
			if (peek().token.equals(Token.EOF)) {
				throw new Report.Error(String.format("Unexpected EOF, expected: \"%s\"", token.str()));
			}

			if (peek() != null && peek().location != null) {
				throw new Report.Error(peek().location, err);
			} else {
				throw new Report.Error(err);
			}
		}
	}

	private Location fromTo(Location loc) {
		return loc.join(dontMove ? prevSymb.location : current.location);
	}

	private void parseSource() {
		Vector<AstDecl> declerations = new Vector<>();
		parseDecls(declerations);
		ast = new ASTs<AstDecl>(null, declerations);
	}

	private void parseDecls(Vector<AstDecl> parentNode) {
		parseDecl(parentNode);
	}

	private void parseDecl(Vector<AstDecl> parentNode) {
		move();
		AstDecl declNode;
		Location loc = peek().location;

		switch (peek().token) {
			case EOF:
			case RIGHT_PARENTHESIS:
				dontMove = true;
				return;
			case TYP:
				String name = checkExpected(Token.IDENTIFIER).lexeme();
				checkExpected(Token.ASSIGN);
				AstType type = parseType();
				checkExpected(Token.SEMICOLON);
				declNode = new AstTypDecl(fromTo(loc), name, type);
				break;
			case VAR:
				name = checkExpected(Token.IDENTIFIER).lexeme();
				checkExpected(Token.COLON);
				type =  parseType();
				checkExpected(Token.SEMICOLON);
				declNode = new AstVarDecl(fromTo(loc), name, type);
				break;
			case FUN:
				name = checkExpected(Token.IDENTIFIER).lexeme();
				checkExpected(Token.LEFT_PARENTHESIS);
				Vector<AstParDecl> paramsVector = new Vector<>();
				parseParams(paramsVector);
				ASTs<AstParDecl> pars = new ASTs<>(null, paramsVector);
				checkExpected(Token.RIGHT_PARENTHESIS);
				checkExpected(Token.COLON);
				type = parseType();
				checkExpected(Token.ASSIGN);
				AstExpr expr = parseExpr();
				checkExpected(Token.SEMICOLON);
				declNode = new AstFunDecl(fromTo(loc), name, pars, type, expr);
				break;
			default:
				throw new Report.Error(peek().location, String.format("Unexpected token at start of decleration \"%s\", allowed [typ, var, fun]", peek().lexeme()));
		}

		parentNode.add(declNode);
		parseDecls(parentNode);
	}

	private AstType parseType() {
		move();
		Location loc = peek().location;

		switch (peek().token) {
			case VOID:
				return new AstAtomType(loc, AstAtomType.Kind.VOID);
			case CHAR:
				return new AstAtomType(loc, AstAtomType.Kind.CHAR);
			case INT:
				return new AstAtomType(loc, AstAtomType.Kind.INT);
			case IDENTIFIER:
				return new AstTypeName(loc, peek().lexeme());
			case LEFT_BRACKET:
				AstExpr expr = parseExpr();
				checkExpected(Token.RIGHT_BRACKET);
				AstType type = parseType();
				return new AstArrType(fromTo(loc), type, expr);
			case POINTER:
				type = parseType();
				return new AstPtrType(fromTo(loc), type);
			case LEFT_PARENTHESIS:
				type = parseType();
				checkExpected(Token.RIGHT_PARENTHESIS);
				return type;
			default:
				throw new Report.Error(peek().location, String.format("Unexpected token at start of TYPE decleration: \"%s\"", peek().lexeme()));
		}
	}

	private void parseParams(Vector<AstParDecl> parentNode) {
		move();
		Location loc = peek().location;

		switch (peek().token) {
			case IDENTIFIER:
				String name = peek().lexeme();
				checkExpected(Token.COLON);
				AstType type = parseType();
				parentNode.add(new AstParDecl(fromTo(loc), name, type));
				parseOptionalParams(parentNode);
				break;
			default:
				dontMove = true;
		}
	}

	private void parseOptionalParams(Vector<AstParDecl> parentNode) {
		move();
		Location loc = peek().location;

		switch (peek().token) {
			case COMMA:
				String name = checkExpected(Token.IDENTIFIER).lexeme();
				checkExpected(Token.COLON);
				AstType type = parseType();
				parentNode.add(new AstParDecl(fromTo(loc), name, type));
				parseOptionalParams(parentNode);
				break;
			default:
				dontMove = true;
		}
	}

	private AstExpr parseExpr() {
		return parseOr();
	}
	
	private AstExpr parseOr() {
		AstExpr left = parseAnd();
		return parseInnerOr(left);
	}

	private AstExpr parseInnerOr(AstExpr left) {
		move();

		switch (peek().token) {
			case OR:
				AstExpr temp = parseAnd();
				AstExpr expr = new AstBinExpr(fromTo(left.location), AstBinExpr.Oper.OR, left, temp);
				return parseInnerOr(expr);
			default:
				dontMove = true;
		}

		return left;
	}

	private AstExpr parseAnd() {
		AstExpr left = parseRelational();
		return parseInnerAnd(left);
	}

	private AstExpr parseInnerAnd(AstExpr left) {
		move();

		switch (peek().token) {
			case AND:
				AstExpr temp = parseRelational();
				AstExpr expr = new AstBinExpr(fromTo(left.location), AstBinExpr.Oper.AND, left, temp);
				return parseInnerAnd(expr);
			default:
				dontMove = true;
		}

		return left;
	}

	private AstExpr parseRelational() {
		AstExpr left = parseAddSub();
		return parseInnerRelational(left);
	}

	private AstExpr parseInnerRelational(AstExpr left) {
		move();
		AstBinExpr.Oper operator = null;

		switch (peek().token) {
			case EQUAL:
				operator = AstBinExpr.Oper.EQU;
				break;
			case NOT_EQUAL:
				operator = AstBinExpr.Oper.NEQ;
				break;
			case LESS:
				operator = AstBinExpr.Oper.LTH;
				break;
			case GREATER:
				operator = AstBinExpr.Oper.GTH;
				break;
			case LESS_OR_EQUAL:
				operator = AstBinExpr.Oper.LEQ;
				break;
			case GREATER_OR_EQUAL:
				operator = AstBinExpr.Oper.GEQ;
				break;
			default:
				dontMove = true;
		}

		if (operator != null) {
			AstExpr temp = parseAddSub();
			AstExpr expr = new AstBinExpr(fromTo(left.location), operator, left, temp);
			return parseInnerRelational(expr);
		}

		return left;
	}

	private AstExpr parseAddSub() {
		AstExpr left = parseOtherMath();
		return parseInnerAddSub(left);
	}

	private AstExpr parseInnerAddSub(AstExpr left) {
		move();
		AstBinExpr.Oper operator = null;

		switch (peek().token) {
			case PLUS:
				operator = AstBinExpr.Oper.ADD;
				break;
			case MINUS:
				operator = AstBinExpr.Oper.SUB;
				break;
			default:
				dontMove = true;
		}

		if (operator != null) {
			AstExpr temp = parseOtherMath();
			AstExpr expr = new AstBinExpr(fromTo(left.location), operator, left, temp);
			return parseInnerAddSub(expr);
		}

		return left;
	}

	private AstExpr parseOtherMath() {
		AstExpr left = parsePrefix();
		return parseInnerOtherMath(left);
	}

	private AstExpr parseInnerOtherMath(AstExpr left) {
		move();
		AstBinExpr.Oper operator = null;

		switch (peek().token) {
			case MULTIPLY:
				operator = AstBinExpr.Oper.MUL;
				break;
			case DIVIDE:
				operator = AstBinExpr.Oper.DIV;
				break;
			case MOD:
				operator = AstBinExpr.Oper.MOD;
				break;
			default:
				dontMove = true;
		}

		if (operator != null) {
			AstExpr temp = parsePrefix();
			AstExpr expr = new AstBinExpr(fromTo(left.location), operator, left, temp);
			return parseInnerOtherMath(expr);
		}

		return left;
	}

	private AstExpr parsePrefix() {
		move();
		Location loc = peek().location;
		AstPreExpr.Oper operator = null;

		switch (peek().token) {
			case NEGATION:
				operator = AstPreExpr.Oper.NOT;
				break;
			case PLUS:
				operator = AstPreExpr.Oper.ADD;
				break;
			case MINUS:
				operator = AstPreExpr.Oper.SUB;
				break;
			case POINTER:
				operator = AstPreExpr.Oper.PTR;
				break;
			case NEW:
				operator = AstPreExpr.Oper.NEW;
				break;
			case DEL:
				operator = AstPreExpr.Oper.DEL;
				break;
			default:
				dontMove = true;
		}

		if (operator != null) {
			AstExpr temp = parsePrefix();
			AstExpr expr = new AstPreExpr(fromTo(loc), operator, temp);
			return expr;
		}

		return parsePostfix();
	}

	private AstExpr parsePostfix() {
		AstExpr left = parseExprWrapper();
		return parseInnerPostfix(left);
	}

	private AstExpr parseInnerPostfix(AstExpr left) {
		move();

		switch (peek().token) {
			case LEFT_BRACKET:
				AstExpr temp = parseExpr();
				checkExpected(Token.RIGHT_BRACKET);
				AstExpr expr = new AstBinExpr(fromTo(left.location), AstBinExpr.Oper.ARR, left, temp);
				return parseInnerPostfix(expr);
			case POINTER:
				expr = new AstPstExpr(fromTo(left.location), AstPstExpr.Oper.PTR, left);
				return parseInnerPostfix(expr);
			default:
				dontMove = true;
		}
		
		return left;
	}

	private AstExpr parseExprWrapper() {
		move();
		Location loc = peek().location;

		switch (peek().token) {
			case INT_CONST:
				return new AstConstExpr(loc, AstConstExpr.Kind.INT, peek().lexeme());
			case CHAR_CONST:
				return new AstConstExpr(loc, AstConstExpr.Kind.CHAR, peek().lexeme());
			case POINTER_CONST:
				return new AstConstExpr(loc, AstConstExpr.Kind.PTR, peek().lexeme());
			case VOID_CONST:
				return new AstConstExpr(loc, AstConstExpr.Kind.VOID, peek().lexeme());
			case IDENTIFIER:
				return parseFuncCall(peek().lexeme(), loc);
			case LEFT_BRACE:
				AstStmtExpr stmtExpr = parseListOfStmts();
				checkExpected(Token.RIGHT_BRACE);
				return stmtExpr;
			case LEFT_PARENTHESIS:
				AstExpr temp = parseExpr();
				AstExpr right = parseExprEnd(temp);
				checkExpected(Token.RIGHT_PARENTHESIS);
				return right;
			default:
				throw new Report.Error(peek().location, String.format("Unexpected token at start of expression: \"%s\"", peek().lexeme()));
		}
	}

	private AstExpr parseExprEnd(AstExpr left) {
		move();

		switch (peek().token) {
			case COLON:
				AstType type = parseType();
				return new AstCastExpr(fromTo(left.location), left, type);
			case WHERE:
				Vector<AstDecl> declVector = new Vector<>();
				parseDecls(declVector);
				ASTs<AstDecl> decls = new ASTs<>(null, declVector);
				return new AstWhereExpr(fromTo(left.location), decls, left);
			default:
				dontMove = true;
		}

		return left;
	}

	private AstExpr parseFuncCall(String name, Location loc) {
		move();

		switch (peek().token) {
			case LEFT_PARENTHESIS:
				Vector<AstExpr> argsVector = new Vector<>();
				parseCallParams(argsVector);
				ASTs<AstExpr> args = new ASTs<>(null, argsVector);
				checkExpected(Token.RIGHT_PARENTHESIS);
				return new AstCallExpr(fromTo(loc), name, args);
			default:
				dontMove = true;
		}

		return new AstNameExpr(fromTo(loc), name);
	}

	private void parseCallParams(Vector<AstExpr> parentNode) {
		move();

		switch (peek().token) {
			case LEFT_PARENTHESIS:
			case LEFT_BRACE:
			case IDENTIFIER:
			case POINTER:
			case PLUS:
			case MINUS:
			case NEGATION:
			case VOID_CONST:
			case INT_CONST:
			case CHAR_CONST:
			case POINTER_CONST:
			case NEW:
			case DEL:
				dontMove = true;
				parentNode.add(parseExpr());
				parseOptionalCallParams(parentNode);
				break;
			default:
				dontMove = true;
		}
	}

	private void parseOptionalCallParams(Vector<AstExpr> parentNode) {
		move();

		switch (peek().token) {
			case COMMA:
				parentNode.add(parseExpr());
				parseOptionalCallParams(parentNode);
				break;
			default:
				dontMove = true;
		}
	}

	private AstStmtExpr parseListOfStmts() {
		Location loc = peek().location;
		Vector<AstStmt> stmtsVector = new Vector<>();
		parseStmt(stmtsVector);
		parseOptionalStmts(stmtsVector);
		ASTs<AstStmt> stmts = new ASTs<>(null, stmtsVector);

		return new AstStmtExpr(fromTo(loc), stmts);
	}

	private void parseStmt(Vector<AstStmt> parentNode) {
		move();
		Location loc = peek().location;

		switch (peek().token) {
			case IF:
				AstExpr expr = parseExpr();
				checkExpected(Token.THEN);
				Location innerLoc = peek().location;
				AstStmtExpr stmtList = parseListOfStmts();
				AstExprStmt bodyStmt = new AstExprStmt(fromTo(innerLoc), stmtList);
				innerLoc = peek().location;
				AstStmtExpr endIf = parseIfEnd();
				parentNode.add(new AstIfStmt(fromTo(loc), expr, bodyStmt, endIf != null ? new AstExprStmt(fromTo(innerLoc), endIf) : null));
				break;
			case WHILE:
				expr = parseExpr();
				checkExpected(Token.DO);
				innerLoc = peek().location;
				stmtList = parseListOfStmts();
				bodyStmt = new AstExprStmt(fromTo(innerLoc), stmtList);
				checkExpected(Token.END);
				checkExpected(Token.SEMICOLON);
				parentNode.add(new AstWhileStmt(fromTo(loc), expr, bodyStmt));
				break;
			default:
				dontMove = true;
				expr = parseExpr();
				parentNode.add(parseOptionalAssign(expr));
		}
	}

	private void parseOptionalStmts(Vector<AstStmt> parentNode) {
		move();

		switch (peek().token) {
			case RIGHT_BRACE:
			case ELSE:
			case END:
				dontMove = true;
				break;
			default:
				dontMove = true;
				parseStmt(parentNode);
				parseOptionalStmts(parentNode);
		}
	}

	private AstStmt parseOptionalAssign(AstExpr expr) {
		move();

		switch (peek().token) {
			case SEMICOLON:
				return new AstExprStmt(fromTo(expr.location), expr);
			case ASSIGN:
				AstExpr expr2 = parseExpr();
				checkExpected(Token.SEMICOLON);
				return new AstAssignStmt(fromTo(expr.location), expr, expr2);
			default:
				throw new Report.Error(peek().location, String.format("Expected assignment or semicolon after expression, got: \"%s\"", peek().lexeme()));
		}
	}

	private AstStmtExpr parseIfEnd() {
		move();

		switch (peek().token) {
			case END:
				checkExpected(Token.SEMICOLON);
				return null;
			case ELSE:
				AstStmtExpr elseExpression = parseListOfStmts();
				checkExpected(Token.END);
				checkExpected(Token.SEMICOLON);
				return elseExpression;
			default:
				throw new Report.Error(peek().location, String.format("Unexpected token got: \"%s\" \"%s\"", "else|end", peek().lexeme()));
		}
	}
}
