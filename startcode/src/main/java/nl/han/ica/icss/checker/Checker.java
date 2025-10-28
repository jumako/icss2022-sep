package nl.han.ica.icss.checker;

import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.ColorLiteral;
import nl.han.ica.icss.ast.literals.PixelLiteral;
import nl.han.ica.icss.ast.types.ExpressionType;
import nl.han.ica.icss.ast.literals.BoolLiteral;
import nl.han.ica.icss.ast.literals.PercentageLiteral;
import nl.han.ica.icss.ast.literals.ScalarLiteral;


import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;


public class Checker {

    private final LinkedList<HashMap<String, ExpressionType>> variableTypes =  new LinkedList<>();

    public void check(AST ast) {
        variableTypes.clear();
        variableTypes.push(new HashMap<>());
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
        if("width".equals(declaration.property.name)){
            if(expressionType == ExpressionType.COLOR || declaration.expression instanceof ColorLiteral){

                declaration.setError("Property 'width': color not allowed ");
            }

        }
        if ("background-color".equals(declaration.property.name)) {
            if(expressionType == ExpressionType.PIXEL || declaration.expression instanceof PixelLiteral){
                declaration.setError("Property 'background-color': pixel not allowed ");
            }
        }
    }

    public void ensureScope() {
        if (variableTypes.isEmpty()) variableTypes.push(new HashMap<>());
    }

    private ExpressionType inferType(Expression expression) {
        if (expression instanceof ColorLiteral) return ExpressionType.COLOR;
        if (expression instanceof PixelLiteral) return ExpressionType.PIXEL;
        if (expression instanceof BoolLiteral) return ExpressionType.BOOL;
        if (expression instanceof VariableReference) {
            String name = ((VariableReference) expression).name;
            ExpressionType resolved = resolve(name);
            return resolved;
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
