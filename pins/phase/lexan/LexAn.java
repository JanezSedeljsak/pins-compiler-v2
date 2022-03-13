package pins.phase.lexan;

import java.io.*;
import pins.common.report.*;
import pins.data.symbol.*;

/**
 * Lexical analyzer.
 */
public class LexAn implements AutoCloseable {

	private String srcFileName;

	private FileReader srcFile;

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

	public Symbol lexer() {
	}

}
