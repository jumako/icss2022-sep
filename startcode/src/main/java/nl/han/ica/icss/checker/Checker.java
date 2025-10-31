package nl.han.ica.icss.checker;


import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.ColorLiteral;
import nl.han.ica.icss.ast.literals.PixelLiteral;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;
import nl.han.ica.icss.ast.types.ExpressionType;
import nl.han.ica.icss.ast.literals.BoolLiteral;
import nl.han.ica.icss.ast.literals.PercentageLiteral;
import nl.han.ica.icss.ast.literals.ScalarLiteral;


import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;


public class Checker {
    private final LinkedList<HashMap<String, ExpressionType>> variableTypes = new LinkedList<>();
    private static final java.util.Set<String> ALLOWED_PROPS =
            java.util.Set.of("color","background-color","width","height");
    public void check(AST ast) {
        variableTypes.clear();
        variableTypes.push(new HashMap<String, ExpressionType>());
        checkStylesheet(ast.root);
    }


    private void checkStylesheet(Stylesheet sheet) {
        for (ASTNode child : sheet.getChildren()) {
            checkNode(child);
        }
    }

    private void checkNode(ASTNode node) {
        if (node instanceof Stylerule) {
            checkStylerule((Stylerule) node);
        } else if (node instanceof VariableAssignment) {
            checkVariableAssignment((VariableAssignment) node);
        } else if (node instanceof Declaration) {
            checkDeclaration((Declaration) node);
        } else if (node instanceof IfClause) {
            checkIfClause((IfClause)node);
        }
        else {
            for (ASTNode child : node.getChildren()) {
                checkNode(child);
            }
        }
    }



    private void checkStylerule(Stylerule rule) {
        HashMap<String, ExpressionType> childScope = new HashMap<>();
        if (!variableTypes.isEmpty()) {
            for (Map.Entry<String, ExpressionType> expression : variableTypes.peek().entrySet()) {
                childScope.put(expression.getKey(), expression.getValue());
            }
        }
        variableTypes.push(childScope);
        for (ASTNode node : rule.getChildren()) {
            checkNode(node);
        }
        variableTypes.pop();
    }

    private void checkVariableAssignment(VariableAssignment variableAssignment) {
        ExpressionType expression = inferType(variableAssignment.expression);
        if (expression == null) {
            variableAssignment.setError("Kan type van expressie niet bepalen voor variabele '" + variableAssignment.name.name + "'.");
            return;
        }
        ensureScope();
        variableTypes.peek().put(variableAssignment.name.name, expression);
    }


    private void checkDeclaration(Declaration declaration) {
        ExpressionType expression = inferType(declaration.expression);
        String prop = declaration.property.name.toLowerCase();
        if (!ALLOWED_PROPS.contains(prop)) {
            declaration.setError("Property '" + declaration.property.name + "' is niet toegestaan in ICSS.");
            return;
        }


        if (declaration.expression instanceof VariableReference && expression == null) {
            declaration.setError("Onbekende variabele: " + ((VariableReference) declaration.expression).name);
            return;
        }
        if ("width".equalsIgnoreCase(declaration.property.name)) {
            if (!(expression == ExpressionType.PIXEL || expression == ExpressionType.PERCENTAGE)) {

                declaration.setError("Property 'width' verwacht pixel of percentage, maar kreeg: " + (expression == null ? "onbekend" : expression.toString()));
            }
        }
        if ("background-color".equalsIgnoreCase(declaration.property.name)) {
            if (expression != ExpressionType.COLOR) {
                declaration.setError("Property 'background-color' verwacht color, maar kreeg: " + (expression == null ? "onbekend" : expression.toString()));
            }
        }
        if ("color".equalsIgnoreCase(declaration.property.name)) {
            if (expression != ExpressionType.COLOR) {
                declaration.setError("Property 'color' verwacht color, maar kreeg: " + (expression == null ? "onbekend" : expression));;
            }
        }
        if ("height".equalsIgnoreCase(declaration.property.name)) {
            if (!(expression == ExpressionType.PIXEL || expression == ExpressionType.PERCENTAGE)) {
                declaration.setError("Property 'height' verwacht pixel of percentage, maar kreeg: " + (expression == null ? "onbekend" : expression));
            }
        }

    }
    private void checkIfClause(IfClause ifClause) {
        ExpressionType expression = inferType(ifClause.conditionalExpression);
        if(expression != ExpressionType.BOOL) {
            ifClause.setError("If-conditie moet BOOL zijn, kreeg: " + (expression == null ? "onbekend" : expression.toString())+".");
        }
        variableTypes.push(copyTopScope());
        for (ASTNode node : ifClause.body){
            checkNode(node);
        }
            variableTypes.pop();

        if(ifClause.elseClause != null && ifClause.elseClause.body != null){
            variableTypes.push(copyTopScope());
            for (ASTNode node : ifClause.elseClause.body){
                checkNode(node);
            }
            variableTypes.pop();
        }
    }

    public void ensureScope() {
        if (variableTypes.isEmpty()) variableTypes.push(new HashMap<String, ExpressionType>());
    }

    private ExpressionType inferType(Expression expression) {
        if (expression instanceof ColorLiteral) return ExpressionType.COLOR;
        if (expression instanceof PixelLiteral) return ExpressionType.PIXEL;
        if (expression instanceof PercentageLiteral) return ExpressionType.PERCENTAGE;
        if (expression instanceof ScalarLiteral) return ExpressionType.SCALAR;
        if (expression instanceof BoolLiteral) return ExpressionType.BOOL;
        if (expression instanceof VariableReference) {
            String name = ((VariableReference) expression).name;
            return resolve(name);
        }

        if (expression instanceof Operation) {
            Operation operation = (Operation) expression;
            ExpressionType left = inferType(operation.lhs);
            ExpressionType right = inferType(operation.rhs);


            if (left == null || right == null) {
                operation.setError("Ongeldige operand(en) voor " + operation.getClass().getSimpleName());
                return null;
            }
// Keer
            if (operation instanceof MultiplyOperation) {
                if (left == ExpressionType.SCALAR && (right == ExpressionType.PIXEL || right == ExpressionType.PERCENTAGE)) {
                    return right;
                }
                if ((left == ExpressionType.PIXEL || left == ExpressionType.PERCENTAGE) && right == ExpressionType.SCALAR) {
                    return left;
                }
                if (left == ExpressionType.SCALAR && right == ExpressionType.SCALAR) {
                    return ExpressionType.SCALAR;
                }
                if (left == ExpressionType.COLOR || right == ExpressionType.COLOR) {
                    operation.setError("Vermenigvuldiging met color is niet toegestaan.");
                    return null;
                }
                operation.setError("Ongeldige vermenigvuldiging: " + left + " * " + right);
                return null;
            }
// plus/Min
            if (operation instanceof AddOperation || operation instanceof SubtractOperation) {
                if (left == ExpressionType.COLOR || right == ExpressionType.COLOR) {
                    operation.setError("Kleur mag niet gebruikt worden in + of -.");
                    return null;
                }
                if (left == right && (left == ExpressionType.PIXEL || left == ExpressionType.PERCENTAGE || left == ExpressionType.SCALAR)) {
                    return left;
                }
                operation.setError("Ongeldige optelling/aftrekking: " + left + " en " + right + " zijn niet compatibel.");
                return null;
            }
            operation.setError("Onbekende operatie: " + operation.getClass().getSimpleName());
            return null;
        }

        return null;
    }
// Zoekt het type variable op
    private ExpressionType resolve(String name) {
        for (HashMap<String, ExpressionType> scope : variableTypes) {
            if (scope.containsKey(name)) return scope.get(name);
        }
        return null;
    }

    private HashMap<String, ExpressionType> copyTopScope() {
        HashMap<String, ExpressionType> map = new HashMap<>();
        if (!variableTypes.isEmpty()) map.putAll(variableTypes.peek()); {
            return map;
        }
    }


}
