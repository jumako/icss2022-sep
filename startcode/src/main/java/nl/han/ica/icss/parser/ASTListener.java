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
    private HANStack<ASTNode> currentContainer = new HANStack<>();
    private HANStack<Expression> exprStack = new HANStack<>();
    private HANStack<IfClause> ifStack = new HANStack<>();
    private Expression currentCon;

    public ASTListener() {
        ast = new AST();


    }

    public AST getAST() {
        return ast;
    }


    @Override
    public void enterStylesheet(ICSSParser.StylesheetContext ctx) {
        System.out.println("test");
        currentContainer.push(new Stylesheet());
    }

    @Override
    public void exitStylesheet(ICSSParser.StylesheetContext ctx) {
        Stylesheet sheet = (Stylesheet) currentContainer.pop();
        ast.setRoot(sheet);
    }

    @Override
    public void enterVariableAssignment(ICSSParser.VariableAssignmentContext ctx) {
        currentContainer.push(new VariableAssignment());

    }

    @Override
    public void exitVariableAssignment(ICSSParser.VariableAssignmentContext ctx) {
        VariableAssignment variableAssignment = (VariableAssignment) currentContainer.pop();
        if(exprStack.isEmpty()){
            throw new RuntimeException("RHS expression ontbreekt voor variableAssignment op regel "+ ctx.getStart().getLine());
        }
        variableAssignment.expression = exprStack.pop();
        currentContainer.peek().addChild(variableAssignment);
    }

    @Override
    public void exitVariableName(ICSSParser.VariableNameContext ctx) {
        ASTNode top = currentContainer.peek();
        if (top instanceof VariableAssignment) {
            ((VariableAssignment) top).name = new VariableReference(ctx.getText());
        }
    }

    @Override
    public void enterStylerule(ICSSParser.StyleruleContext ctx) {
        currentContainer.push(new Stylerule());
    }

    @Override
    public void exitStylerule(ICSSParser.StyleruleContext ctx) {
        Stylerule srule = (Stylerule) currentContainer.pop();
        currentContainer.peek().addChild(srule);
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

    }

    @Override
    public void exitDeclaration(ICSSParser.DeclarationContext ctx) {
        Declaration decl = (Declaration) currentContainer.pop();
        if(exprStack.isEmpty()){
            throw new RuntimeException("Expression ontbreekt in declaration op regel " + ctx.getStart().getLine());
        }
        decl.expression = exprStack.pop();
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
        exprStack.push(new PixelLiteral(n));
    }

    @Override
    public void exitScalarLiteral(ICSSParser.ScalarLiteralContext ctx) {
        exprStack.push(new ScalarLiteral(Integer.parseInt(ctx.getText())));
    }

    @Override
    public void exitColorLiteral(ICSSParser.ColorLiteralContext ctx) {
        exprStack.push(new ColorLiteral(ctx.getText()));
    }

    @Override
    public void exitTrueLiteral(ICSSParser.TrueLiteralContext ctx) {
        exprStack.push(new BoolLiteral(true));
    }

    @Override
    public void exitFalseLiteral(ICSSParser.FalseLiteralContext ctx) {
        exprStack.push(new BoolLiteral(false));
    }

    @Override
    public void exitVarRef(ICSSParser.VarRefContext ctx) {
        exprStack.push(new VariableReference(ctx.getText()));
    }

    @Override
    public void exitAddOperation(ICSSParser.AddOperationContext ctx) {
        Expression rhs = exprStack.pop();
        Expression lhs = exprStack.pop();
        AddOperation op = new AddOperation();
        op.lhs = lhs;
        op.rhs = rhs;
        exprStack.push(op);
    }

    @Override
    public void exitSubOperation(ICSSParser.SubOperationContext ctx) {
        Expression rhs = exprStack.pop();
        Expression lhs = exprStack.pop();
        SubtractOperation op = new SubtractOperation();
        op.lhs = lhs;
        op.rhs = rhs;
        exprStack.push(op);
    }

    @Override
    public void exitMulOperation(ICSSParser.MulOperationContext ctx) {
        Expression rhs = exprStack.pop();
        Expression lhs = exprStack.pop();
        MultiplyOperation op = new MultiplyOperation();
        op.lhs = lhs;
        op.rhs = rhs;
        exprStack.push(op);
    }

    @Override
    public void enterIfClause(ICSSParser.IfClauseContext ctx) {
        IfClause ifClause = new IfClause();
        currentContainer.push(ifClause);
        ifStack.push(ifClause);


    }

    @Override
    public void exitIfClause(ICSSParser.IfClauseContext ctx) {
        IfClause ifClause = (IfClause) currentContainer.pop();
        ifClause.conditionalExpression = currentCon;
        ifStack.pop();
        currentContainer.peek().addChild(ifClause);
    }

    @Override
    public void enterElseClause(ICSSParser.ElseClauseContext ctx) {
        currentContainer.push(new ElseClause());
    }

    @Override
    public void exitElseClause(ICSSParser.ElseClauseContext ctx) {
        ElseClause elseClause = (ElseClause) currentContainer.pop();
        IfClause parentIfClause = ifStack.peek();
        parentIfClause.addChild(elseClause);
    }

    @Override
    public void enterCondition (ICSSParser.ConditionContext ctx){

    }

    @Override
    public void exitCondition (ICSSParser.ConditionContext ctx) {
        if (exprStack.isEmpty()) {
            throw new RuntimeException("Condition expression empty" + ctx.getStart().getLine());

        }
        currentCon = exprStack.pop();
    }
    @Override
    public void exitPercentageLiteral(ICSSParser.PercentageLiteralContext ctx) {
        String t = ctx.getText();
        int n = Integer.parseInt(t.substring(0, t.length() - 1));
        exprStack.push(new PercentageLiteral(n));
    }

}






