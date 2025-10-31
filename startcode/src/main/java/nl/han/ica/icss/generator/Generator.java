package nl.han.ica.icss.generator;


import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.selectors.ClassSelector;
import nl.han.ica.icss.ast.selectors.IdSelector;
import nl.han.ica.icss.ast.selectors.TagSelector;

public class Generator {
/** et ISCC-AST om naar CSS
 * - Zet ISCC-AST om naar CSS
 * - Bouwt CSS door de boom te doorlopen en tekst in een StringBuilder te append’en.
 * - Retourneert het eindresultaat als string.
 */

    public String generate(AST ast) {
        if (ast == null || ast.root == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        walk(ast.root, builder, 0);
        return builder.toString();
    }
// Loopt door de boom heen
    private void walk(ASTNode astnode, StringBuilder builder, int indent) {
        if (astnode instanceof Stylesheet) {
            Stylesheet stylesheet = (Stylesheet) astnode;
            for (ASTNode child : stylesheet.getChildren()) {
                walk(child, builder, 0);
            }
            return;
        }
        if (astnode instanceof Stylerule) {
            Stylerule stylerule = (Stylerule) astnode;
            indent(builder, indent).append(joinSelectors(stylerule)).append(" {\n");

            for (ASTNode child : stylerule.getChildren()) {
                if (child instanceof Declaration) {
                    Declaration declaration = (Declaration) child;
                    appendDeclaration(declaration, builder, indent + 1);
                }
            }
            indent(builder, indent).append("}\n\n");
            return;
        }
        for (ASTNode child : astnode.getChildren()) {
            walk(child, builder, indent);
        }
    }

    private void appendDeclaration(Declaration declaration, StringBuilder builder, int indent) {
        if (!(declaration.expression instanceof Literal)) {
            throw new IllegalStateException("Generator verwacht Literal-expressies (run Evaluator eerst).");
        }

        String property = declaration.property.name;
        String value = literalToString((Literal) declaration.expression);
        indent(builder, indent).append(property).append(": ").append(value).append(";\n");
    }

    private String joinSelectors(Stylerule stylerule) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < stylerule.selectors.size(); i++) {
            if (i > 0) stringBuilder.append(", ");
            stringBuilder.append(selectorToString(stylerule.selectors.get(i)));
        }
        return stringBuilder.toString();
    }

    private String selectorToString(Selector selector) {
        if (selector instanceof IdSelector) {
            IdSelector idSelector = (IdSelector) selector;
            return "#" + idSelector.id;
        }
        if (selector instanceof ClassSelector) {
            ClassSelector classSelector = (ClassSelector) selector;
            return "." + classSelector.cls;
        }
        if (selector instanceof TagSelector) {
            TagSelector tagSelector = (TagSelector) selector;
            return tagSelector.tag;
        }
        return selector.toString();
    }

    private String literalToString(Literal literal) {
        if (literal instanceof PixelLiteral) {
            PixelLiteral pixelLiteral = (PixelLiteral) literal;
            return pixelLiteral.value + "px";
        }
        if (literal instanceof PercentageLiteral) {
            PercentageLiteral percentageLiteral = (PercentageLiteral) literal;
            return percentageLiteral.value + "%";
        }
        if (literal instanceof ColorLiteral) {
            ColorLiteral colorLiteral = (ColorLiteral) literal;
            return colorLiteral.value;
        }
        if (literal instanceof ScalarLiteral) {
            ScalarLiteral scalarLiteral = (ScalarLiteral) literal;
            return Integer.toString(scalarLiteral.value);
        }
        if (literal instanceof BoolLiteral) {
            BoolLiteral boolLiteral = (BoolLiteral) literal;
            return boolLiteral.value ? "true" : "false";
        }
        return literal.toString();
    }

    private StringBuilder indent(StringBuilder builder, int indent) {
        for (int i = 0; i < indent; i++) {
            builder.append("  ");
        }
        return builder;
    }


}
