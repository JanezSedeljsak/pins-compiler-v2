package pins.phase.synan;

import java.util.*;
import pins.data.symbol.*;

public class SynNode {
    Symbol symbol;
    String ruleName;
    ArrayList<SynNode> data;

    /* Root constructor */
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

    public String toString() {
        StringBuilder sb = new StringBuilder(String.format("%s => ", ruleName));
        String seperator = "";
        for (SynNode node: data) {
            String str = "";
            if (node.symbol != null) {
                str = String.format("%s(%s)", node.symbol.token.toString().toLowerCase(), node.symbol.lexeme);
            } else {
                str = node.ruleName;
            }
            
            sb.append(seperator + str);
            seperator = ", ";
        }

        return sb.toString();
    }
}
