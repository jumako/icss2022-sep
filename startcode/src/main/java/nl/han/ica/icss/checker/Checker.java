package nl.han.ica.icss.checker;

import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.ColorLiteral;
import nl.han.ica.icss.ast.literals.PixelLiteral;
import nl.han.ica.icss.ast.types.ExpressionType;

import java.util.HashMap;
import java.util.LinkedList;


public class Checker {

    private LinkedList<HashMap<String, ExpressionType>> variableTypes;

    public void check(AST ast) {
        checkStylesheet(ast.root);
          variableTypes = new LinkedList<>();

    }

    private void checkStylesheet(Stylesheet sheet) {
        checkStylerule((Stylerule)sheet.getChildren().get(0));
    }

    private void checkStylerule(Stylerule rule) {
        for (ASTNode child : rule.getChildren()) {
            if (child instanceof Declaration) {
                checkDeclaration((Declaration)child);
            }
        }

    }

    private void checkDeclaration(Declaration declaration) {
        if(declaration.property.name.equals("width")){
            if(declaration.expression instanceof ColorLiteral){
                declaration.setError("Property 'width': color not allowed ");
            }
        }
        if (declaration.property.name.equals("background-color")){
            if(declaration.expression instanceof PixelLiteral){
                declaration.setError("Property 'background-color': pixel not allowed ");
            }
        }
    }


}
