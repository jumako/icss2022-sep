package nl.han.ica.icss.checker;

import nl.han.ica.datastructures.IHANLinkedList;
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

    public void check(AST ast) {
        variableTypes.clear();
        variableTypes.push(new HashMap<String, ExpressionType>());
        checkStylesheet(ast.root);
    }

    private void checkStylesheet(Stylesheet sheet) {
        for (ASTNode child : sheet.getChildren()){
            checkNode(child);
        }
    }

    private void checkNode(ASTNode node) {
        if(node instanceof Stylerule){
            checkStylerule((Stylerule) node);
        }
        else if(node instanceof VariableAssignment){
            checkVariableAssignment((VariableAssignment) node);
        }
        else if(node instanceof Declaration){
            checkDeclaration((Declaration) node);
        }
        else{
            for (ASTNode child : node.getChildren()){
                checkNode(child);
            }
        }
    }

    private void checkStylerule(Stylerule rule) {
        HashMap<String, ExpressionType> childScope = new HashMap<>();
        if (!variableTypes.isEmpty()) {
            for (Map.Entry<String, ExpressionType> e : variableTypes.peek().entrySet()) {
                childScope.put(e.getKey(), e.getValue());
            }
        }
        variableTypes.push(childScope);
        for (ASTNode child : rule.getChildren()) {
            checkNode(child);
        }
        variableTypes.pop();
    }

    private void checkVariableAssignment(VariableAssignment variableAssignment) {
        ExpressionType ext = inferType(variableAssignment.expression);
        if (ext == null) {
            variableAssignment.setError("Kan type van expressie niet bepalen voor variabele '" + variableAssignment.name.name + "'.");
            return;
        }
        ensureScope();
        variableTypes.peek().put(variableAssignment.name.name, ext);
    }


    private void checkDeclaration(Declaration declaration) {
        ExpressionType expressionType = inferType(declaration.expression);

        if(declaration.expression instanceof VariableReference && expressionType == null){
            declaration.setError("Onbekende variabele: " + ((VariableReference) declaration.expression).name);
            return;
        }
        if("width".equalsIgnoreCase(declaration.property.name)){
            if(!(expressionType == ExpressionType.PIXEL || expressionType == ExpressionType.PERCENTAGE)){

                declaration.setError("Property 'width' verwacht pixel of percentage, maar kreeg: " + (expressionType == null ? "onbekend" : expressionType.toString()));
            }
        }
        if ("background-color".equalsIgnoreCase(declaration.property.name)) {
            if(expressionType != ExpressionType.COLOR ){
                declaration.setError("Property 'background-color' verwacht color, maar kreeg: " + (expressionType == null ? "onbekend" : expressionType.toString()));
            }
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

        if (expression instanceof Operation){
            Operation op = (Operation) expression;
            ExpressionType lt = inferType(op.lhs);
            ExpressionType rt = inferType(op.rhs);


            if(lt == null || rt == null){
                op.setError("Ongeldige operand(en) voor "+ op.getClass().getSimpleName());
                return null;
            }

            if (op instanceof MultiplyOperation) {
                if (lt == ExpressionType.SCALAR && (rt == ExpressionType.PIXEL || rt == ExpressionType.PERCENTAGE)){
                    return rt;
                }
                if ((lt == ExpressionType.PIXEL || rt == ExpressionType.PERCENTAGE) && rt == ExpressionType.SCALAR){
                    return lt;
                }
                if (lt == ExpressionType.SCALAR && rt == ExpressionType.SCALAR){
                    return ExpressionType.SCALAR;
                }
                if (lt == ExpressionType.COLOR || rt == ExpressionType.COLOR){
                    op.setError("Vermenigvuldiging met color is niet toegestaan.");
                    return null;
                }
                op.setError("Ongeldige vermenigvuldiging: " + lt + " * " + rt);
                return null;
            }

            if (op instanceof AddOperation || op instanceof SubtractOperation){
                if(lt == rt && (lt == ExpressionType.PIXEL || lt == ExpressionType.PERCENTAGE || lt == ExpressionType.SCALAR)){
                    return lt;
                }
                op.setError("Ongeldige optelling/aftrekking: " + lt + " en " + rt + " zijn niet compatibel.");
                return null;
            }
            op.setError("Onbekende operatie: " + op.getClass().getSimpleName());
            return null;
        }

        return null;
    }
    private ExpressionType resolve(String name) {
        for (HashMap<String, ExpressionType> scope : variableTypes) {
            if (scope.containsKey(name)) return scope.get(name);
        }
        return null;
    }


}
