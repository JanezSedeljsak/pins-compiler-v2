package pins.data.symbol;

import pins.common.report.*;

public enum Token {
	
	EOF,

	/* constants */
	VOID_CONST("none"), INT_CONST("[0-9]+", true), CHAR_CONST, POINTER_CONST("nil"),

	/* keywoards */
	CHAR("char"), DEL("del"), DO("do"), ELSE("else"), END("end"), FUN("fun"), IF("if"), INT("int"), 
	NEW("new"), THEN("then"), TYP("typ"), VAR("var"), VOID("void"), WHERE("where"), WHILE("while"),

	/* brackets */
	LEFT_BRACKET("["), RIGHT_BRACKET("]"),
	LEFT_BRACE("{"), RIGHT_BRACE("}"),
	LEFT_PARENTHESIS("("), RIGHT_PARENTHESIS(")"),

	/* symbols */
	COMMA(","), COLON(":"), SEMICOLON(";"), AND("&"), OR("|"), NEGATION("!"), EQUAL("=="), NOT_EQUAL("!="), 
	LESS("<"), GREATER(">"), LESS_OR_EQUAL("<="), GREATER_OR_EQUAL(">="),
	
	/* math */
	MULTIPLY("*"), DIVIDE("/"), MOD("%"), PLUS("+"),
	MINUS("-"), POINTER("^"), ASSIGN("="),

	IDENTIFIER("[A-Za-z_][A-Za-z0-9_]*", true);

	private String match;
	private boolean isRegex;
	private static char FIRST_PRINTABLE = ' ', LAST_PRINTABLE = '~'; // 32, 126

	Token() {
		this.match = null;
		this.isRegex = false;
	}

	Token(String match) {
		this();
		this.match = match;
	}

	Token(String match, boolean isRegex) {
		this.match = match;
		this.isRegex = isRegex;
	}

	public String str() {
		if (this == EOF) {
			return "EOF";
		}

		if (!isRegex) {
			return match;
		}

		return this.toString();
	}

	// a bit of a hacky solution for character matching (implemented like this for easier error tracking)
	private static boolean handleCharacterMatching(String match, boolean fullMatch, int row, int col) {
		int matchLength = match.length();

		try {
			if (matchLength == 0 || match.charAt(0) != '\'') return false;
			// from here forward it must be a char contstant (if it won't match error will be thrown)

			if (matchLength == 1 && !fullMatch) return true;

			if (!(FIRST_PRINTABLE <= match.charAt(1) && match.charAt(1) <= LAST_PRINTABLE && match.charAt(1) != '\'')) {
				throw new Report.Error(new Location(row, col + 1), String.format("Invalid character symbol %s", String.valueOf(match.charAt(1))));
			}

			if (matchLength == 2 && !fullMatch) return true;

			if (match.charAt(1) == '\\') {
				if (match.charAt(2) != '\'' && match.charAt(2) != '\\') {
					throw new Report.Error(new Location(row, col + 2), String.format("Invalid character symbol after escape character \\%s", String.valueOf(match.charAt(2))));
				}

				if (matchLength == 3  && !fullMatch) return true;
				if (matchLength > 3 && match.charAt(3) == '\'') return matchLength == 4;

				throw new Report.Error(new Location(row, col + 3), "Missing closing tag for character");

			} else if (match.charAt(2) == '\'') {
				return matchLength == 3;
			}
				
			int charIndex = match.charAt(1) == '\\' ? 2 : 1;
			throw new Report.Error(new Location(row, col + charIndex), String.format("Invalid closing tag for character: %s", String.valueOf(match.charAt(charIndex))));
			

		} catch (Report.Error __) {
			System.exit(1);
		}

		return false;
	}

	public static Token getFirstMatch(String match, boolean fullMatch, int row, int col) {
		if (match == null) return null;
		int matchLength = match.length();

		boolean isCharMatch = handleCharacterMatching(match, fullMatch, row, col);
		if (isCharMatch) return Token.CHAR_CONST;

		for (Token token: Token.values()) {
			if (token.match == null) continue;
			
			if (!token.isRegex) {
				if (fullMatch && token.match.equals(match)) return token;
				if (!fullMatch && token.match.substring(0, Math.min(token.match.length(), matchLength)).equals(match)) {
					return token;
				}

			} else if (match.matches(token.match)) return token;
		}

		return null;
	}
}
