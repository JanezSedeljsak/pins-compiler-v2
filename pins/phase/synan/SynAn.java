package pins.phase.synan;

import pins.phase.lexan.*;
import pins.data.symbol.*;
import pins.common.report.*;

public class SynAn implements AutoCloseable {

	private final LexAn lexan;
	private Symbol currSymb = null;
	private SynNode syntree;

	public SynAn(LexAn lexan) {
		this.lexan = lexan;
	}
	
	public void close() {
		lexan.close();
	}
	
	public void parser() {
		parseSource();
		if (currSymb.token != Token.EOF) {
			throw new Report.Error(currSymb, "Unexpected '" + currSymb + "' at the end of a program.");
		}
	}

	private void addExpected(SynNode node, Token token) {
		Symbol prev = currSymb;
		currSymb = lexan.lexer();
		if (currSymb.token.equals(token)) {
			node.data.add(new SynNode(currSymb));
		}  else {
			throw new Report.Error(String.format("Unexpected token: %s after: %s", currSymb.lexeme, prev.lexeme));
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
		currSymb = lexan.lexer();
		switch (currSymb.token) {
			case EOF:
				return;
			case TYP:
				declNode.addNode(new SynNode(currSymb));
				addExpected(declNode, Token.IDENTIFIER);
				addExpected(declNode, Token.ASSIGN);
				parseType(declNode);
				addExpected(declNode, Token.SEMICOLON);
				break;
			case VAR:
				declNode.addNode(new SynNode(currSymb));
				addExpected(declNode, Token.IDENTIFIER);
				addExpected(declNode, Token.COLON);
				parseType(declNode);
				addExpected(declNode, Token.SEMICOLON);
				break;
			case FUN:
				declNode.addNode(new SynNode(currSymb));
				addExpected(declNode, Token.IDENTIFIER);
				addExpected(declNode, Token.LEFT_PARENTHESIS);
				addExpected(declNode, Token.RIGHT_PARENTHESIS);
				addExpected(declNode, Token.SEMICOLON);
				break;
			default:
				throw new Report.Error("Unexpected token");
		}

		Report.info(declNode.toString());
		syntree.addNode(declNode);
		parseDecls();
	}

	private void parseType(SynNode parentNode) {
		SynNode typeNode = new SynNode("TYPE");
		currSymb = lexan.lexer();
		switch (currSymb.token) {
			case VOID:
			case CHAR:
			case INT:
			case IDENTIFIER:
				typeNode.addNode(new SynNode(currSymb));
				break;
			case LEFT_BRACKET:
				break;
			case POW:
				typeNode.addNode(new SynNode(currSymb));
				parseType(typeNode);
				break;
			case LEFT_PARENTHESIS:
				typeNode.addNode(new SynNode(currSymb));
				parseType(typeNode);
				addExpected(typeNode, Token.RIGHT_PARENTHESIS);
				break;
			default:
				throw new Report.Error("Unexpected token");
		}

		parentNode.addNode(typeNode);
		Report.info(typeNode.toString());
	}
}
