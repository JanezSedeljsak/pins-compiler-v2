package pins.phase.synan;

import java.util.*;
import pins.data.symbol.*;
import pins.common.report.*;

public class SynNode {
    Symbol symbol;
    String ruleName;
    ArrayList<SynNode> data;

    // true => print only rules where there is atleast one lexeme
    static boolean onlyWithLexemes = true; 

    SynNode(String ruleName) {
        this.symbol = null;
        this.ruleName = ruleName;
        this.data = new ArrayList<>();
    }

    SynNode(Symbol symbol) {
        this.symbol = symbol;
        this.data = new ArrayList<>();
    }

    public void addNode(SynNode node) {
        this.data.add(node);
    }

    public void addNodeSymbol(Symbol symbol) {
        this.addNode(new SynNode(symbol));
    }

    public static void print(SynNode node) {
        boolean hasLexemChild = false;

        if (onlyWithLexemes) {
            for (SynNode child : node.data) {
                if (child.symbol != null) {
                    hasLexemChild = true;
                    break;
                }
            }
        } else {
            hasLexemChild = true;
        }

        if (hasLexemChild && node.symbol == null) {
            Report.info(node.toString());
        }

        for (SynNode child : node.data) {
            print(child);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(String.format("%s => ", ruleName));
        String seperator = "";
        if (data.isEmpty()) {
            sb.append(".");
            return sb.toString();
        }

        for (SynNode node : data) {
            String str = "";
            if (node.symbol != null) {
                str = String.format("[%s \"%s\"]", node.symbol.token, node.symbol.lexeme);
            } else {
                str = String.format("%s", node.ruleName);
            }

            sb.append(seperator + str);
            seperator = " ";
        }

        return sb.toString();
    }
}
