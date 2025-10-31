package nl.han.ica.icss.transforms;

import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.PercentageLiteral;
import nl.han.ica.icss.ast.literals.PixelLiteral;
import nl.han.ica.icss.ast.literals.ScalarLiteral;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;

import nl.han.ica.icss.ast.types.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.*;

import java.util.*;
/**
 * Evaluator — rekent de ICSS-boom uit.
 * Verwijdert variabele-assignments en if/else-structuren door alles
 * te evalueren tot concrete waardes (literals).
 * Resultaat: stylerules met declaraties waarin de expressies al uitgerekend zijn.
 */

public class Evaluator implements Transform {

    private LinkedList<HashMap<String, Literal>> variableValues;

    public Evaluator() {
        this.variableValues = new LinkedList<HashMap<String, Literal>>();
    }
    /**
     * apply(AST) — startpunt van de evaluatie.
     * 1) Maakt een globalscope.
     * 2) Transformeert alle kinderen van de stylesheet (variabelen uitrekenen, if's ontvouwen).
     * 3) Vervangt de originele kinderen door de vereenvoudigde lijst.
     * 4) Ruimt de scope-stack op.
     */

    @Override
    public void apply(AST ast) {
        variableValues.clear();
        variableValues.push(new HashMap<String, Literal>());

        Stylesheet sheet = (Stylesheet) ast.root;
        List<ASTNode> simplified = transfromBlock(sheet.getChildren());
        List<ASTNode> kids = sheet.getChildren();
        kids.clear();
        kids.addAll(simplified);
        variableValues.pop();
    }



    private List<ASTNode> transfromBlock(List<ASTNode> nodes) {
        List<ASTNode> out = new ArrayList<ASTNode>();
        for (ASTNode node : nodes) {
            if (node instanceof VariableAssignment) {
                VariableAssignment varAssign = (VariableAssignment) node;
                Literal value = eval(varAssign.expression);
                ensureScope();
                variableValues.peek().put(varAssign.name.name, value);
            } else if (node instanceof IfClause) {
                IfClause ifClause = (IfClause) node;
                Literal condLit = eval(ifClause.conditionalExpression);
                boolean cond = toBool(condLit);

                List<ASTNode> chosen;
                if (cond) {
                    chosen = ifClause.body;
                } else {
                    chosen = (ifClause.elseClause != null && ifClause.elseClause.body != null)
                            ? ifClause.elseClause.body
                            : Collections.<ASTNode>emptyList();
                }
                variableValues.push(copyTopScope());
                out.addAll(transfromBlock(chosen));
                variableValues.pop();
            } else if (node instanceof Declaration) {
                Declaration declaration = (Declaration) node;
                Literal value = eval(declaration.expression);
                declaration.expression = value;
                out.add(declaration);
            } else if (node instanceof Stylerule) {
                Stylerule stylerule = (Stylerule) node;
                variableValues.push(copyTopScope());
                Stylerule newStylerule = new Stylerule();
                newStylerule.selectors = stylerule.selectors;
                newStylerule.body = new ArrayList<>(transfromBlock(stylerule.body));
                variableValues.pop();
                out.add(newStylerule);
            } else {
                if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                    List<ASTNode> kids = transfromBlock(node.getChildren());
                    node.getChildren().clear();
                    node.getChildren().addAll(kids);
                }
                out.add(node);
            }
        }
        return out;
    }
// evaluatie van expressies
    private Literal eval(Expression expression) {
        if (expression instanceof Literal) {
            return (Literal) expression;
        }
        if (expression instanceof VariableReference) {
            String name = ((VariableReference) expression).name;
            Literal value = lookup(name);
            if (value == null) {
                expression.setError("Onbekende variabele: " + name);
                return new ScalarLiteral(0);
            }
            return value;
        }
        if (expression instanceof Operation) {
            Operation operation = (Operation) expression;
            Literal left = eval(operation.lhs);
            Literal right = eval(operation.rhs);
//Vermenigvuldigen
            if (operation instanceof MultiplyOperation) {
                if (left instanceof PixelLiteral && right instanceof ScalarLiteral) {
                    return new PixelLiteral(((PixelLiteral) left).value * ((ScalarLiteral) right).value);
                }
                if (left instanceof ScalarLiteral && right instanceof PixelLiteral) {
                    return new PixelLiteral(((ScalarLiteral) left).value * ((PixelLiteral) right).value);
                }

                if (left instanceof PercentageLiteral && right instanceof ScalarLiteral) {
                    return new PercentageLiteral(((PercentageLiteral) left).value * ((ScalarLiteral) right).value);
                }
                if (left instanceof ScalarLiteral && right instanceof PercentageLiteral) {
                    return new PercentageLiteral(((ScalarLiteral) left).value * ((PercentageLiteral) right).value);
                }
                if (left instanceof ScalarLiteral && right instanceof ScalarLiteral) {
                    return new ScalarLiteral(((ScalarLiteral) left).value * ((ScalarLiteral) right).value);
                }
                if (left instanceof ColorLiteral || right instanceof ColorLiteral) {
                    operation.setError("Vermenigvuldiging met color is niet toegestaan.");
                    return new ScalarLiteral(0);
                }
                operation.setError("Ongeldige vermenigvuldiging: " +
                        left.getClass().getSimpleName() + " * " + right.getClass().getSimpleName());
                return new ScalarLiteral(0);
            }
            //optellen
            if (operation instanceof AddOperation) {
                if (left instanceof PixelLiteral && right instanceof PixelLiteral) {
                    return new PixelLiteral(((PixelLiteral) left).value + ((PixelLiteral) right).value);
                }
                if (left instanceof PercentageLiteral && right instanceof PercentageLiteral) {
                    return new PercentageLiteral(((PercentageLiteral) left).value + ((PercentageLiteral) right).value);
                }
                if (left instanceof ScalarLiteral && right instanceof ScalarLiteral) {
                    return new ScalarLiteral(((ScalarLiteral) left).value + ((ScalarLiteral) right).value);
                }
                operation.setError("Ongeldige optelling: " +
                        left.getClass().getSimpleName() + " + " + right.getClass().getSimpleName());
                return new ScalarLiteral(0);
            }
// Aftrekken
            if (operation instanceof SubtractOperation) {
                if (left instanceof PixelLiteral && right instanceof PixelLiteral) {
                    return new PixelLiteral(((PixelLiteral) left).value - ((PixelLiteral) right).value);
                }
                if (left instanceof PercentageLiteral && right instanceof PercentageLiteral) {
                    return new PercentageLiteral(((PercentageLiteral) left).value - ((PercentageLiteral) right).value);
                }
                if (left instanceof ScalarLiteral && right instanceof ScalarLiteral) {
                    return new ScalarLiteral(((ScalarLiteral) left).value - ((ScalarLiteral) right).value);
                }
                operation.setError("Ongeldige aftrekking: " +
                        left.getClass().getSimpleName() + " - " + right.getClass().getSimpleName());
                return new ScalarLiteral(0);
            }
        }
        expression.setError("Niet-ondersteunde expressie: " + expression.getClass().getSimpleName());
        return new ScalarLiteral(0);

    }


    private Literal lookup(String name) {
        for (HashMap<String, Literal> scope : variableValues) {
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return null;
    }

    private void ensureScope() {
        if (variableValues.isEmpty()) {
            variableValues.push(new HashMap<String, Literal>());
        }
    }
    private boolean toBool(Literal condLit) {
        if (condLit instanceof BoolLiteral) {
            return ((BoolLiteral) condLit).value;
        }
        condLit.setError("If-conditie moet BOOL zijn.");
        return false;
}
    /**
     * copyTopScope() — maakt een defensieve kopie van de huidige (bovenste) scope.
     * Zo kunnen we binnen stylerules en if/else-branches veilig variabelen wijzigen
     * zonder dat die wijzigingen ‘lekken’ naar hogere scopes.
     */

    private HashMap<String, Literal> copyTopScope() {
        return variableValues.isEmpty() ? new HashMap<>() : new HashMap<>(variableValues.peek());
    }

}
