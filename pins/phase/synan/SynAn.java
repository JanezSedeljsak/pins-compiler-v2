package pins.phase.synan;

import pins.phase.lexan.*;
import pins.data.symbol.*;
import pins.common.report.*;

public class SynAn implements AutoCloseable {

	private final LexAn lexan;
	private Symbol current = null, prevSymb = null;
	private SynNode syntree;
	private boolean dontMove = false; 

	public SynAn(LexAn lexan) {
		this.lexan = lexan;
	}

	public void close() {
		lexan.close();
	}

	public void parser() {
		parseSource();
		if (peek().token != Token.EOF) {
			throw new Report.Error(peek(), "Unexpected '" + peek() + "' at the end of a program.");
		}
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

	private void addExpected(SynNode node, Token token) {
		move();
		if (peek().token.equals(token)) {
			node.addNodeSymbol(peek());
		} else {
			String err = String.format("Unexpected token: %s after: %s - in %s", peek().lexeme, prevSymb.lexeme, node.ruleName);
			throw new Report.Error(peek().location, err);
		}
	}

	private void parseSource() {
		parseDecls();
	}

	private void parseDecls() {
		this.syntree = new SynNode("ROOT");
		parseDecl(this.syntree);
	}

	private void parseDecl(SynNode parentNode) {
		SynNode declNode = new SynNode("DECL");
		move();
		switch (peek().token) {
			case EOF:
				return;
			case TYP:
				declNode.addNodeSymbol(peek());
				addExpected(declNode, Token.IDENTIFIER);
				addExpected(declNode, Token.ASSIGN);
				parseType(declNode);
				addExpected(declNode, Token.SEMICOLON);
				break;
			case VAR:
				declNode.addNodeSymbol(peek());
				addExpected(declNode, Token.IDENTIFIER);
				addExpected(declNode, Token.COLON);
				parseType(declNode);
				addExpected(declNode, Token.SEMICOLON);
				break;
			case FUN:
				declNode.addNodeSymbol(peek());
				addExpected(declNode, Token.IDENTIFIER);
				addExpected(declNode, Token.LEFT_PARENTHESIS);
				parseParams(declNode);
				addExpected(declNode, Token.RIGHT_PARENTHESIS);
				addExpected(declNode, Token.COLON);
				parseType(declNode);
				addExpected(declNode, Token.ASSIGN);
				parseExpr(declNode);
				break;
			default:
				throw new Report.Error(peek().location, String.format("Unexpected token at start of DECL: %s", peek().lexeme));
		}

		Report.info(declNode.toString());
		parentNode.addNode(declNode);
		parseDecls();
	}

	private void parseType(SynNode parentNode) {
		SynNode typeNode = new SynNode("TYPE");
		move();
		switch (peek().token) {
			case VOID:
			case CHAR:
			case INT:
			case IDENTIFIER:
				typeNode.addNodeSymbol(peek());
				break;
			case LEFT_BRACKET:
				typeNode.addNodeSymbol(peek());
				parseExpr(typeNode);
				addExpected(typeNode, Token.RIGHT_BRACKET);
				parseType(typeNode);
				break;
			case POINTER:
				typeNode.addNodeSymbol(peek());
				parseType(typeNode);
				break;
			case LEFT_PARENTHESIS:
				typeNode.addNodeSymbol(peek());
				parseType(typeNode);
				addExpected(typeNode, Token.RIGHT_PARENTHESIS);
				break;
			default:
				throw new Report.Error(peek().location, String.format("Unexpected token at start of TYPE: %s", peek().lexeme));
		}

		parentNode.addNode(typeNode);
		Report.info(typeNode.toString());
	}

	private void parseParams(SynNode parentNode) {
		SynNode paramsNode = new SynNode("PARAMS");
		move();
		switch (peek().token) {
			case IDENTIFIER:
				paramsNode.addNodeSymbol(peek());
				addExpected(paramsNode, Token.COLON);
				parseType(paramsNode);
				parseOptionalParams(paramsNode);
				break;
			default:
				dontMove = true;
		}

		parentNode.addNode(paramsNode);
		Report.info(paramsNode.toString());
	}

	private void parseOptionalParams(SynNode parentNode) {
		SynNode optionalParamsNode = new SynNode("OPTIONAL_PARAMS");
		move();
		switch (peek().token) {
			case COMMA:
				optionalParamsNode.addNodeSymbol(peek());
				addExpected(optionalParamsNode, Token.IDENTIFIER);
				addExpected(optionalParamsNode, Token.COLON);
				parseType(optionalParamsNode);
				parseOptionalParams(optionalParamsNode);
				break;
			default:
				dontMove = true;
		}

		parentNode.addNode(optionalParamsNode);
		Report.info(optionalParamsNode.toString());
	}

	private void parseExpr(SynNode parentNode) {
		SynNode exprNode = new SynNode("EXPR");
		parseAnd(exprNode);
		parseOr(exprNode);
		parentNode.addNode(exprNode);
		Report.info(exprNode.toString());
	}

	private void parseOr(SynNode parentNode) {
		SynNode orNode = new SynNode("OR");
		switch (peek().token) {
			case OR:
				orNode.addNodeSymbol(peek());
				parseAnd(orNode);
				parseOr(orNode);
				break;
			default:
				dontMove = true;
		}

		parentNode.addNode(orNode);
		Report.info(orNode.toString());
	}

	private void parseAnd(SynNode parentNode) {
		SynNode andNode = new SynNode("AND");
		parseRelational(andNode);
		parseInnerAnd(andNode);
		parentNode.addNode(andNode);
		Report.info(andNode.toString());
	}

	private void parseInnerAnd(SynNode parentNode) {
		SynNode innerAndNode = new SynNode("AND_INNER");
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
		Report.info(innerAndNode.toString());
	}

	private void parseRelational(SynNode parentNode) {
		SynNode relationalNode = new SynNode("RELATIONAL");
		parseAddSub(relationalNode);
		parseInnerRelational(relationalNode);
		parentNode.addNode(relationalNode);
		Report.info(relationalNode.toString());
	}

	private void parseInnerRelational(SynNode parentNode) {
		SynNode innerRelationalNode = new SynNode("RELATIONAL_INNER");
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
		Report.info(innerRelationalNode.toString());
	}

	private void parseAddSub(SynNode parentNode) {
		SynNode addSubNode = new SynNode("ADD_SUB");
		parseOtherMath(addSubNode);
		parseInnerAddSub(addSubNode);
		parentNode.addNode(addSubNode);
		Report.info(addSubNode.toString());
	}

	private void parseInnerAddSub(SynNode parentNode) {
		SynNode innerAddSubNode = new SynNode("INNER_ADD_SUB");
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
		Report.info(innerAddSubNode.toString());
	}

	private void parseOtherMath(SynNode parentNode) {
		SynNode relationalNode = new SynNode("OTHER_MATH");
		parsePrefix(relationalNode);
		parseInnerOtherMath(relationalNode);
		parentNode.addNode(relationalNode);
		Report.info(relationalNode.toString());
	}

	private void parseInnerOtherMath(SynNode parentNode) {
		SynNode innerOtherMathNode = new SynNode("INNER_OTHER_MATH");
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
		Report.info(innerOtherMathNode.toString());
	}

	private void parsePrefix(SynNode parentNode) {
		SynNode prefixNode = new SynNode("PREFIX");
		switch (peek().token) {
			case NEGATION:
			case PLUS:
			case MINUS:
			case POINTER:
				prefixNode.addNodeSymbol(peek());
				parsePrefix(prefixNode);
				break;
			default:
				parsePostfix(prefixNode);
		}

		parentNode.addNode(prefixNode);
		Report.info(prefixNode.toString());
	}

	private void parsePostfix(SynNode parentNode) {
		SynNode postfixNode = new SynNode("POSTFIX");
		parseExprWrapper(postfixNode);
		parseInnerPostfix(postfixNode);
		Report.info(postfixNode.toString());
	}

	private void parseInnerPostfix(SynNode parentNode) {
		SynNode postfixNode = new SynNode("INNER_POSTFIX");
		switch (peek().token) {
			case LEFT_BRACKET:
				postfixNode.addNodeSymbol(peek());
				parseExpr(postfixNode);
				addExpected(postfixNode, Token.RIGHT_BRACKET);
				break;
			case POINTER:
				postfixNode.addNodeSymbol(peek());
				parseInnerPostfix(postfixNode);
				break;
			default:
				dontMove = true;
		}
		
		parentNode.addNode(postfixNode);
		Report.info(postfixNode.toString());
	}

	private void parseExprWrapper(SynNode parentNode) {
		SynNode exprWrapperNode = new SynNode("EXPR_WRAPPER");
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
				parseStmts(exprWrapperNode);
				addExpected(exprWrapperNode, Token.RIGHT_BRACE);
				break;
			case LEFT_PARENTHESIS:
				exprWrapperNode.addNodeSymbol(peek());
				parseExpr(exprWrapperNode);
				parseExprEnd(exprWrapperNode);
				addExpected(exprWrapperNode, Token.RIGHT_PARENTHESIS);
				break;
			default:
				throw new Report.Error(peek().location, String.format("Unexpected token at start of EXPR: %s", peek().lexeme));
		}
		
		parentNode.addNode(exprWrapperNode);
		Report.info(exprWrapperNode.toString());
	}

	private void parseExprEnd(SynNode parentNode) {
		SynNode exprEndNode = new SynNode("EXPR_END");
		switch (peek().token) {
			case COLON:
				exprEndNode.addNodeSymbol(peek());
				parseType(exprEndNode);
				break;
			case WHERE:
				exprEndNode.addNodeSymbol(peek());
				parseDecl(exprEndNode);
				break;
			default:
				dontMove = true;
		}
		parentNode.addNode(exprEndNode);
		Report.info(exprEndNode.toString());
	}

	private void parseFuncCall(SynNode parentNode) {
		SynNode funcCallNode = new SynNode("FUNC_PARAMS");
		switch (peek().token) {
			case LEFT_PARENTHESIS:
				funcCallNode.addNodeSymbol(peek());
				parseCallParams(funcCallNode);
				addExpected(funcCallNode, Token.RIGHT_PARENTHESIS);
				break;
			default:
				dontMove = true;
		}
		
		parentNode.addNode(funcCallNode);
		Report.info(funcCallNode.toString());
	}

	private void parseCallParams(SynNode parentNode) {
		SynNode callParamsNode = new SynNode("FUNC_PARAMS");
		switch (peek().token) {
			case IDENTIFIER:
			case LEFT_PARENTHESIS:
			case LEFT_BRACE:
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
				callParamsNode.addNodeSymbol(peek());
				parseExpr(callParamsNode);
				parseOptionalCallParams(callParamsNode);
				break;
			default:
				dontMove = true;
		}
		
		parentNode.addNode(callParamsNode);
		Report.info(callParamsNode.toString());
	}

	private void parseOptionalCallParams(SynNode parentNode) {
		SynNode optionalCallParamsNode = new SynNode("FUNC_PARAMS");
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
		Report.info(optionalCallParamsNode.toString());
	}
}
