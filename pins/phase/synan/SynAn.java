package pins.phase.synan;

import pins.phase.lexan.*;
import pins.data.symbol.*;
import pins.common.report.*;

public class SynAn implements AutoCloseable {

	private final LexAn lexan;
	private Symbol currSymb = null, prevSymb = null;
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
		return currSymb;
	}

	public void move() {
		if (dontMove) {
			dontMove = false;
		} else {
			prevSymb = peek();
			currSymb = lexan.lexer();
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
		parseDecl();
	}

	private void parseDecl() {
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
				break;
			default:
				throw new Report.Error(peek().location, String.format("Unexpected token at start of DECL: %s", peek().lexeme));
		}

		Report.info(declNode.toString());
		syntree.addNode(declNode);
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
		SynNode paramNode = new SynNode("OPTIONAL_PARAMS");
		move();
		switch (peek().token) {
			case COMMA:
				paramNode.addNodeSymbol(peek());
				addExpected(paramNode, Token.IDENTIFIER);
				addExpected(paramNode, Token.COLON);
				parseType(paramNode);
				parseOptionalParams(paramNode);
				break;
			default:
				dontMove = true;
		}

		parentNode.addNode(paramNode);
		Report.info(paramNode.toString());
	}

	private void parseExpr(SynNode parentNode) {
		SynNode exprNode = new SynNode("EXPR");
		move();
		switch (peek().token) {
			case INT:
				break;
			default:
				throw new Report.Error(peek().location, String.format("Unexpected token at start of TYPE: %s", peek().lexeme));
		}

		parentNode.addNode(exprNode);
		Report.info(exprNode.toString());
	}
}
