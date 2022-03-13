package pins.phase.lexan;

import java.io.*;
import java.util.*;

import pins.common.report.*;
import pins.data.symbol.*;

/**
 * Lexical analyzer.
 */
public class LexAn implements AutoCloseable {

	private String srcFileName;
	private FileReader srcFile;
	private final static int TAB_SIZE = 8;

	private int row = 1;
	private int col = 0;

	private Queue<Character> unseen = new LinkedList<>();
	
	public LexAn(String srcFileName) {
		this.srcFileName = srcFileName;
		try {
			srcFile = new FileReader(new File(srcFileName));
		} catch (FileNotFoundException __) {
			throw new Report.Error("Cannot open source file '" + srcFileName + "'.");
		}
	}

	public void close() {
		try {
			srcFile.close();
		} catch (IOException __) {
			throw new Report.Error("Cannot close source file '" + srcFileName + "'.");
		}
	}

	private char getNextCharacter() {
		try {
			if (unseen.isEmpty()) {
				col++;
				return (char) srcFile.read(); 
			}

			return unseen.remove();

		} catch (IOException __) {
			throw new Report.Error("Error reading from source file '" + srcFileName + "'.");
		}
	}

	private boolean isEOF(char chr) {
		return (char)chr == (char)-1;
	}

	private boolean isNotWhiteSpace(char chr) {
		return !String.valueOf(chr).matches("\\s+");
	}

	private Symbol passComment() {
		int commentDepth = 1;
		char currChar = getNextCharacter();
		if (currChar != '{') {
			throw new Error(String.format("Invalid start of comment #%c", currChar));
		}

		while ((currChar = getNextCharacter()) != (char)-1) {
			if (currChar == '#' && getNextCharacter() == '{') commentDepth++;
			else if (currChar == '}') {
				char next;

				// handle eg. '#{  this is a comment }}}}}}#'
				while ((next = getNextCharacter()) == '}') {}
				if (next == '#') {
					commentDepth--;
				}

			} else if (currChar == '\n') {
				row++;
				col = 0;
			} else if (currChar == '\t') {
				col = ((col / TAB_SIZE) + 1) * TAB_SIZE;
			}

			if (commentDepth == 0) break;
		}

		if (currChar == (char)-1) {
			if (commentDepth != 0) Report.warning("Comment not closed at end of file!");
			return new Symbol(Token.EOF, "", null);
		}

		return null;
	}

	public Symbol lexer() {
		StringBuilder builder = new StringBuilder("");
		boolean eofFlag = false, notWhiteSpaceFlag = false, noMathFlag = false;
		int len;
		char current;
		
		// inner loop breaks out everytime we see a whitespace, however, that does not mean we found a valid symbol
		// this is why we break out to the outer loop and run until we find a valid next symbol 
		while (true) {
			while (true) {

				// read next character and set flags
				current = getNextCharacter();
				eofFlag = isEOF(current);
				notWhiteSpaceFlag = isNotWhiteSpace(current);

				if (builder.length() > 0 || notWhiteSpaceFlag) {
					builder.append(current);
				}

				// handle eof and newline
				if (eofFlag) break;
				if (current == '\n') {
					row++;
					col = 0;
				}

				// handle tab
				if (current == '\t') {
					col = ((col / TAB_SIZE) + 1) * TAB_SIZE;
					break;
				}

				// handle single whitespace (if char was seen don't break)
				boolean prevIsCharStart = builder.length() > 0 && builder.toString().charAt(builder.length() - 1) == '\'';
				if (current == ' ' && builder.length() > 0 && prevIsCharStart) break;

				// handle comment start
				if (current == '#') {
					Symbol eofSymbol = passComment();
					builder.setLength(0); // clear buffer after comment
					if (eofSymbol != null) return eofSymbol;
				}
	
				// break and return last valid lexeme without last token
				noMathFlag = Token.getFirstMatch(builder.toString(), false, row, col) == null;
				if (noMathFlag) break;
			}

			// store token if not comment, whitespace (allow other characters or EOF)
			if (current != '#' && (notWhiteSpaceFlag || eofFlag)) {
				unseen.add(current);
			}

			// handle EOF
			len = builder.length();
			if (len == 0 || (len == 1 && eofFlag)) {
				if (eofFlag) return new Symbol(Token.EOF, "", null);
				continue;
			}

			// print invalid token error
			if (noMathFlag && len == 1) {
				throw new Report.Error(new Location(row, col), String.format("Invalid token: %s", builder.toString()));
			}

			// parse and return valid symbol
			builder.setLength(builder.length() - 1);
			String lexeme = builder.toString();
			Token matchedToken = Token.getFirstMatch(lexeme, true, row, col);

			if (matchedToken != null) {
				return new Symbol(matchedToken, lexeme, new Location(row, col - lexeme.length(), row, col - 1));
			}
		}
	}
}
 