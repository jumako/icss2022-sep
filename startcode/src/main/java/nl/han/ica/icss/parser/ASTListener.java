package nl.han.ica.icss.parser;

import java.util.Stack;


import nl.han.ica.datastructures.HANStack;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.parser.ICSSParser;

import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;
import nl.han.ica.icss.ast.selectors.ClassSelector;
import nl.han.ica.icss.ast.selectors.IdSelector;
import nl.han.ica.icss.ast.selectors.TagSelector;
import org.checkerframework.checker.signature.qual.Identifier;

/**
 * This class extracts the ICSS Abstract Syntax Tree from the Antlr Parse tree.
 */
public class ASTListener extends ICSSBaseListener {

    private AST ast;
    private HANStack<ASTNode> currentContainer;
    private Expression currentExpr;

    public ASTListener() {
        ast = new AST();
        currentContainer = new HANStack<>();

    }

    public AST getAST() {
        return ast;
    }


    @Override
    public void enterStylesheet(ICSSParser.StylesheetContext ctx) {
        currentContainer.push(new Stylesheet());
    }

    @Override
    public void exitStylesheet(ICSSParser.StylesheetContext ctx) {
        Stylesheet stylesheet = (Stylesheet) currentContainer.pop();
        ast.setRoot(stylesheet);
    }

    @Override
    public void enterVariableAssignment(ICSSParser.VariableAssignmentContext ctx) {
        currentContainer.push(new VariableAssignment());
        currentExpr = null;

    }

    @Override
    public void exitVariableAssignment(ICSSParser.VariableAssignmentContext ctx) {
        VariableAssignment variableAssignment = (VariableAssignment) currentContainer.pop();
        variableAssignment.expression = currentExpr;
        currentContainer.peek().addChild(variableAssignment);
    }

    @Override
    public void enterStylerule(ICSSParser.StyleruleContext ctx) {
        currentContainer.push(new Stylerule());
    }

    @Override
    public void exitStylerule(ICSSParser.StyleruleContext ctx) {
        Stylerule stylerule = (Stylerule) currentContainer.pop();
        currentContainer.peek().addChild(stylerule);
    }

    @Override
    public void enterTagSelector(ICSSParser.TagSelectorContext ctx) {
        currentContainer.push(new TagSelector(ctx.getText()));
    }

    @Override
    public void exitTagSelector(ICSSParser.TagSelectorContext ctx) {
        TagSelector sel = (TagSelector) currentContainer.pop();
        currentContainer.peek().addChild(sel);
    }

    @Override
    public void enterClassSelector(ICSSParser.ClassSelectorContext ctx) {
        currentContainer.push(new ClassSelector(ctx.getText().substring(1)));
    }

    @Override
    public void exitClassSelector(ICSSParser.ClassSelectorContext ctx) {
        ClassSelector sel = (ClassSelector) currentContainer.pop();
        currentContainer.peek().addChild(sel);
    }

    @Override
    public void enterIdSelector(ICSSParser.IdSelectorContext ctx) {
        currentContainer.push(new IdSelector(ctx.getText().substring(1)));
    }

    @Override
    public void exitIdSelector(ICSSParser.IdSelectorContext ctx) {
        IdSelector sel = (IdSelector) currentContainer.pop();
        currentContainer.peek().addChild(sel);
    }

    @Override
    public void enterDeclaration(ICSSParser.DeclarationContext ctx) {
        currentContainer.push(new Declaration());
        currentExpr = null;
    }

    @Override
    public void exitDeclaration(ICSSParser.DeclarationContext ctx) {
        Declaration decl = (Declaration) currentContainer.pop();
        decl.expression = currentExpr;
        currentContainer.peek().addChild(decl);
    }

    @Override
    public void exitProperty(ICSSParser.PropertyContext ctx) {
        Declaration decl = (Declaration) currentContainer.peek();
        decl.property = new PropertyName(ctx.getText());
    }

    @Override
    public void exitPixelLiteral(ICSSParser.PixelLiteralContext ctx) {
        String t = ctx.getText();
        int n = Integer.parseInt(t.substring(0, t.length() - 2));
        currentExpr = new PixelLiteral(n);
    }

    @Override
    public void exitColorLiteral(ICSSParser.ColorLiteralContext ctx) {
        currentExpr = new ColorLiteral(ctx.getText());
    }

    @Override
    public void exitVariableName(ICSSParser.VariableNameContext ctx) {
        ASTNode top = currentContainer.peek();
        if (top instanceof VariableAssignment) {
            ((VariableAssignment) top).name = new VariableReference(ctx.getText());
        }
    }


    @Override
    public void exitTrueLiteral(ICSSParser.TrueLiteralContext ctx) {
        currentExpr = new BoolLiteral(true);
    }

    @Override
    public void exitFalseLiteral(ICSSParser.FalseLiteralContext ctx) {
        currentExpr = new BoolLiteral(false);
    }

    @Override
    public void exitVarRef(ICSSParser.VarRefContext ctx) {
        currentExpr = new VariableReference(ctx.getText());
    }

}

