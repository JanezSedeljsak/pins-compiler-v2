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

		while (peek() == null || peek().token != Token.EOF) {
			move();
		}
		
		if (peek() != null && peek().token == Token.EOF) {
			return ast;
		}

		throw new Report.Error(String.format("Unexpected token at end of program: %s, expected: EOF", peek().lexeme));
	}

	public Symbol peek() {
		return current;
	}

	public void move() {
		if (dontMove) {
			dontMove = false;
		} else {
			prevSymb = peek();
			current = lexan.lexer();
		}
	}

	private Symbol checkExpected(Token token) {
		move();
		if (peek().token.equals(token)) {
			return peek();
		} else {
			String err = String.format("Unexpected token: %s after: %s - expected %s", peek().lexeme, prevSymb.lexeme, token.str());
			if (peek().token.equals(Token.EOF)) {
				throw new Report.Error("Unexpected EOF");
			}

			if (peek() != null && peek().location != null) {
				throw new Report.Error(peek().location, err);
			} else {
				throw new Report.Error(err);
			}
		}
	}

	private void parseSource() {
		Vector<AstDecl> declerations = new Vector<>();
		parseDecls(declerations);
		ast = new ASTs<AstDecl>(peek().location, declerations);
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
				String name = checkExpected(Token.IDENTIFIER).lexeme;
				checkExpected(Token.ASSIGN);
				AstType type = parseType();
				checkExpected(Token.SEMICOLON);
				declNode = new AstTypDecl(loc, name, type);
				break;
			case VAR:
				name = checkExpected(Token.IDENTIFIER).lexeme;
				checkExpected(Token.COLON);
				type =  parseType();
				checkExpected(Token.SEMICOLON);
				declNode = new AstVarDecl(loc, name, type);
				break;
			/*case FUN:
				declNode.addNodeSymbol(peek());
				checkExpected(Token.IDENTIFIER);
				checkExpected(Token.LEFT_PARENTHESIS);
				parseParams(declNode);
				checkExpected(Token.RIGHT_PARENTHESIS);
				checkExpected(Token.COLON);
				parseType(declNode);
				checkExpected(Token.ASSIGN);
				parseExpr(declNode);
				checkExpected(Token.SEMICOLON);
				break;*/
			default:
				throw new Report.Error(peek().location, String.format("Unexpected token at start of DECL: %s", peek().lexeme));
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
				return new AstTypeName(loc, peek().lexeme);
			/*case LEFT_BRACKET:
				typeNode.addNodeSymbol(peek());
				parseExpr(typeNode);
				checkExpected(Token.RIGHT_BRACKET);
				parseType(typeNode);
				break;
			case POINTER:
				typeNode.addNodeSymbol(peek());
				parseType(typeNode);
				break;
			case LEFT_PARENTHESIS:
				typeNode.addNodeSymbol(peek());
				parseType(typeNode);
				checkExpected(Token.RIGHT_PARENTHESIS);
				break;*/
			default:
				throw new Report.Error(peek().location, String.format("Unexpected token at start of TYPE: %s", peek().lexeme));
		}
	}

	private void parseParams(SynNode parentNode) {
		SynNode paramsNode = new SynNode("PARAMS");
		move();

		switch (peek().token) {
			case IDENTIFIER:
				paramsNode.addNodeSymbol(peek());
				checkExpected(Token.COLON);
				parseType();
				parseOptionalParams(paramsNode);
				break;
			default:
				dontMove = true;
		}

		parentNode.addNode(paramsNode);
	}

	private void parseOptionalParams(SynNode parentNode) {
		SynNode optionalParamsNode = new SynNode("OPTIONAL_PARAMS");
		move();

		switch (peek().token) {
			case COMMA:
				optionalParamsNode.addNodeSymbol(peek());
				checkExpected(Token.IDENTIFIER);
				checkExpected(Token.COLON);
				parseType();
				parseOptionalParams(optionalParamsNode);
				break;
			default:
				dontMove = true;
		}

		parentNode.addNode(optionalParamsNode);
	}

	private void parseExpr(SynNode parentNode) {
		SynNode exprNode = new SynNode("EXPR");
		parseOr(exprNode);
		parentNode.addNode(exprNode);
	}
	
	private void parseOr(SynNode parentNode) {
		SynNode orNode = new SynNode("OR");
		parseAnd(orNode);
		parseInnerOr(orNode);
		parentNode.addNode(orNode);
	}

	private void parseInnerOr(SynNode parentNode) {
		SynNode orNode = new SynNode("OR");
		move();

		switch (peek().token) {
			case OR:
				orNode.addNodeSymbol(peek());
				parseAnd(orNode);
				parseInnerOr(orNode);
				break;
			default:
				dontMove = true;
		}

		parentNode.addNode(orNode);
	}

	private void parseAnd(SynNode parentNode) {
		SynNode andNode = new SynNode("AND");
		parseRelational(andNode);
		parseInnerAnd(andNode);
		parentNode.addNode(andNode);
	}

	private void parseInnerAnd(SynNode parentNode) {
		SynNode innerAndNode = new SynNode("AND_INNER");
		move();

		switch (peek().token) {
			case AND:
				innerAndNode.addNodeSymbol(peek());
				parseRelational(innerAndNode);
				parseInnerAnd(innerAndNode);
				break;
			default:
				dontMove = true;
		}

		parentNode.addNode(innerAndNode);
	}

	private void parseRelational(SynNode parentNode) {
		SynNode relationalNode = new SynNode("RELATIONAL");
		parseAddSub(relationalNode);
		parseInnerRelational(relationalNode);
		parentNode.addNode(relationalNode);
	}

	private void parseInnerRelational(SynNode parentNode) {
		SynNode innerRelationalNode = new SynNode("RELATIONAL_INNER");
		move();

		switch (peek().token) {
			case EQUAL:
			case NOT_EQUAL:
			case LESS:
			case GREATER:
			case LESS_OR_EQUAL:
			case GREATER_OR_EQUAL:
				innerRelationalNode.addNodeSymbol(peek());
				parseAddSub(innerRelationalNode);
				parseInnerRelational(innerRelationalNode);
				break;
			default:
				dontMove = true;
		}

		parentNode.addNode(innerRelationalNode);
	}

	private void parseAddSub(SynNode parentNode) {
		SynNode addSubNode = new SynNode("ADD_SUB");
		parseOtherMath(addSubNode);
		parseInnerAddSub(addSubNode);
		parentNode.addNode(addSubNode);
	}

	private void parseInnerAddSub(SynNode parentNode) {
		SynNode innerAddSubNode = new SynNode("INNER_ADD_SUB");
		move();

		switch (peek().token) {
			case PLUS:
			case MINUS:
				innerAddSubNode.addNodeSymbol(peek());
				parseOtherMath(innerAddSubNode);
				parseInnerAddSub(innerAddSubNode);
				break;
			default:
				dontMove = true;
		}

		parentNode.addNode(innerAddSubNode);
	}

	private void parseOtherMath(SynNode parentNode) {
		SynNode relationalNode = new SynNode("OTHER_MATH");
		parsePrefix(relationalNode);
		parseInnerOtherMath(relationalNode);
		parentNode.addNode(relationalNode);
	}

	private void parseInnerOtherMath(SynNode parentNode) {
		SynNode innerOtherMathNode = new SynNode("INNER_OTHER_MATH");
		move();

		switch (peek().token) {
			case MULTIPLY:
			case DIVIDE:
			case MOD:
				innerOtherMathNode.addNodeSymbol(peek());
				parsePrefix(innerOtherMathNode);
				parseInnerOtherMath(innerOtherMathNode);
				break;
			default:
				dontMove = true;
		}

		parentNode.addNode(innerOtherMathNode);
	}

	private void parsePrefix(SynNode parentNode) {
		SynNode prefixNode = new SynNode("PREFIX");
		move();

		switch (peek().token) {
			case NEGATION:
			case PLUS:
			case MINUS:
			case POINTER:
				prefixNode.addNodeSymbol(peek());
				parsePrefix(prefixNode);
				break;
			default:
				dontMove = true;
				parsePostfix(prefixNode);
		}

		parentNode.addNode(prefixNode);
	}

	private void parsePostfix(SynNode parentNode) {
		SynNode postfixNode = new SynNode("POSTFIX");
		parseExprWrapper(postfixNode);
		parseInnerPostfix(postfixNode);
		parentNode.addNode(postfixNode);
	}

	private void parseInnerPostfix(SynNode parentNode) {
		SynNode postfixNode = new SynNode("INNER_POSTFIX");
		move();

		switch (peek().token) {
			case LEFT_BRACKET:
				postfixNode.addNodeSymbol(peek());
				parseExpr(postfixNode);
				checkExpected(Token.RIGHT_BRACKET);
				break;
			case POINTER:
				postfixNode.addNodeSymbol(peek());
				parseInnerPostfix(postfixNode);
				break;
			default:
				dontMove = true;
		}
		
		parentNode.addNode(postfixNode);
	}

	private void parseExprWrapper(SynNode parentNode) {
		SynNode exprWrapperNode = new SynNode("EXPR_WRAPPER");
		move();

		switch (peek().token) {
			case INT_CONST:
			case CHAR_CONST:
			case POINTER_CONST:
			case VOID_CONST:
				exprWrapperNode.addNodeSymbol(peek());
				break;
			case IDENTIFIER:
				exprWrapperNode.addNodeSymbol(peek());
				parseFuncCall(exprWrapperNode);
				break;
			case NEW:
			case DEL:
				exprWrapperNode.addNodeSymbol(peek());
				parseExprWrapper(exprWrapperNode);
				break;
			case LEFT_BRACE:
				exprWrapperNode.addNodeSymbol(peek());
				parseStmt(exprWrapperNode);
				parseOptionalStmts(exprWrapperNode);
				checkExpected(Token.RIGHT_BRACE);
				break;
			case LEFT_PARENTHESIS:
				exprWrapperNode.addNodeSymbol(peek());
				parseExpr(exprWrapperNode);
				parseExprEnd(exprWrapperNode);
				checkExpected(Token.RIGHT_PARENTHESIS);
				break;
			default:
				throw new Report.Error(peek().location, String.format("Unexpected token at start of EXPR_WRAPPER: %s", peek().lexeme));
		}
		
		parentNode.addNode(exprWrapperNode);
	}

	private void parseExprEnd(SynNode parentNode) {
		SynNode exprEndNode = new SynNode("EXPR_END");
		move();

		switch (peek().token) {
			case COLON:
				exprEndNode.addNodeSymbol(peek());
				parseType();
				break;
			case WHERE:
				exprEndNode.addNodeSymbol(peek());
				//parseDecl(exprEndNode);
				break;
			default:
				dontMove = true;
		}
		parentNode.addNode(exprEndNode);
	}

	private void parseFuncCall(SynNode parentNode) {
		SynNode funcCallNode = new SynNode("FUNC_CALL");
		move();

		switch (peek().token) {
			case LEFT_PARENTHESIS:
				funcCallNode.addNodeSymbol(peek());
				parseCallParams(funcCallNode);
				checkExpected(Token.RIGHT_PARENTHESIS);
				break;
			default:
				dontMove = true;
		}
		
		parentNode.addNode(funcCallNode);
	}

	private void parseCallParams(SynNode parentNode) {
		SynNode callParamsNode = new SynNode("CALL_PARAMS");
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
				parseExpr(callParamsNode);
				parseOptionalCallParams(callParamsNode);
				break;
			default:
				dontMove = true;
		}
		
		parentNode.addNode(callParamsNode);
	}

	private void parseOptionalCallParams(SynNode parentNode) {
		SynNode optionalCallParamsNode = new SynNode("OPTIOINAL_CALL_PARAMS");
		move();

		switch (peek().token) {
			case COMMA:
				optionalCallParamsNode.addNodeSymbol(peek());
				parseExpr(optionalCallParamsNode);
				parseOptionalCallParams(parentNode);
				break;
			default:
				dontMove = true;
		}
		
		parentNode.addNode(optionalCallParamsNode);
	}

	private void parseStmt(SynNode parentNode) {
		SynNode stmtNode = new SynNode("STMT");
		move();

		switch (peek().token) {
			case IF:
				stmtNode.addNodeSymbol(peek());
				parseExpr(stmtNode);
				checkExpected(Token.THEN);
				parseStmt(stmtNode);
				parseOptionalStmts(stmtNode);
				parseIfEnd(stmtNode);
				break;
			case WHILE:
				stmtNode.addNodeSymbol(peek());
				parseExpr(stmtNode);
				checkExpected(Token.DO);
				parseStmt(stmtNode);
				parseOptionalStmts(stmtNode);
				checkExpected(Token.END);
				checkExpected(Token.SEMICOLON);
				break;
			default:
				dontMove = true;
				parseExpr(stmtNode);
				parseOptionalAssign(stmtNode);
		}

		parentNode.addNode(stmtNode);
	}

	private void parseOptionalStmts(SynNode parentNode) {
		SynNode optionalStmtsNode = new SynNode("OPTIOINAL_STMTS");
		move();

		switch (peek().token) {
			case RIGHT_BRACE:
			case ELSE:
			case END:
				dontMove = true;
				break;
			default:
				dontMove = true;
				parseStmt(optionalStmtsNode);
				parseOptionalStmts(optionalStmtsNode);
		}

		parentNode.addNode(optionalStmtsNode);
	}

	private void parseOptionalAssign(SynNode parentNode) {
		SynNode optionalAssignNode = new SynNode("OPTIOINAL_ASSIGN");
		move();

		switch (peek().token) {
			case SEMICOLON:
				optionalAssignNode.addNodeSymbol(peek());
				break;
			case ASSIGN:
				optionalAssignNode.addNodeSymbol(peek());
				parseExpr(optionalAssignNode);
				checkExpected(Token.SEMICOLON);
				break;
			default:
				throw new Report.Error(peek().location, String.format("Expected assignment or semicolon after expression, got: %s", peek().lexeme));
		}

		parentNode.addNode(optionalAssignNode);
	}

	private void parseIfEnd(SynNode parentNode) {
		SynNode ifEndNode = new SynNode("IF_END");
		move();

		switch (peek().token) {
			case END:
				ifEndNode.addNodeSymbol(peek());
				checkExpected(Token.SEMICOLON);
				break;
			case ELSE:
				ifEndNode.addNodeSymbol(peek());
				parseStmt(ifEndNode);
				parseOptionalStmts(ifEndNode);
				checkExpected(Token.END);
				checkExpected(Token.SEMICOLON);
				break;
			default:
				throw new Report.Error(peek().location, String.format("Unexpected token at start of IF_END: %s", peek().lexeme));
		}

		parentNode.addNode(ifEndNode);
	}
}
