package nl.han.ica.icss.parser;

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


/**
 * Bouwt een ICSS-AST vanuit de ANTLR parse tree.
 * <p>
 * Architectuur:
 * - currentContainer (stack): houdt de huidige AST-"container" vast (Stylesheet, Stylerule, If/Else, etc.)
 * - exprStack (stack): bouwt expressies bottom-up (literals, var refs, +, -, *)
 * - ifStack (stack): koppelt de conditie en (else-)body aan de juiste IfClause, ook bij nesting
 * <p>
 * Belangrijk:
 * - In exitCondition() koppelen we de conditie altijd aan ifStack.peek() (geen globale state).
 * - In exitDeclaration()/exitVariableAssignment() verwachten we dat exprStack een RHS bevat zo niet → duidelijke foutmelding.
 */

public class ASTListener extends ICSSBaseListener {
    private AST ast;
    // Houdt de huidige AST-container vast tijdens de parse (Stylesheet / Stylerule / IfClause / ElseClause)
    private HANStack<ASTNode> currentContainer = new HANStack<>();
    // Bouwt expressies (literals, var refs, +, -, *) in RPN-stijl op via exit* van de grammar
    private HANStack<Expression> exprStack = new HANStack<>();
    // Koppelt condities en else-bodies altijd aan de juiste IfClause (ook bij geneste if's)
    private HANStack<IfClause> ifStack = new HANStack<>();

    public ASTListener() {
        ast = new AST();
    }


    public AST getAST() {
        return ast;
    }

// Stylesheet
    @Override
    public void enterStylesheet(ICSSParser.StylesheetContext ctx) {
        currentContainer.push(new Stylesheet());
    }

    @Override
    public void exitStylesheet(ICSSParser.StylesheetContext ctx) {
        Stylesheet sheet = (Stylesheet) currentContainer.pop();
        ast.setRoot(sheet);
    }
// Variablen
    @Override
    public void enterVariableAssignment(ICSSParser.VariableAssignmentContext ctx) {
        currentContainer.push(new VariableAssignment());

    }

    @Override
    public void exitVariableAssignment(ICSSParser.VariableAssignmentContext ctx) {
        VariableAssignment variableAssignment = (VariableAssignment) currentContainer.pop();
        if (exprStack.isEmpty()) {
            variableAssignment.setError("RHS ontbreekt voor variableAssignment (syntaxfout eerder in de regel?).");
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
//Stylerule
    @Override
    public void enterStylerule(ICSSParser.StyleruleContext ctx) {
        currentContainer.push(new Stylerule());
    }

    @Override
    public void exitStylerule(ICSSParser.StyleruleContext ctx) {
        Stylerule stylerule = (Stylerule) currentContainer.pop();
        currentContainer.peek().addChild(stylerule);
    }
//Selectors
    //Tagselector
    @Override
    public void enterTagSelector(ICSSParser.TagSelectorContext ctx) {
        currentContainer.push(new TagSelector(ctx.getText()));
    }

    @Override
    public void exitTagSelector(ICSSParser.TagSelectorContext ctx) {
        TagSelector tagSelector = (TagSelector) currentContainer.pop();
        currentContainer.peek().addChild(tagSelector);
    }
// Class selector
    @Override
    public void enterClassSelector(ICSSParser.ClassSelectorContext ctx) {
        currentContainer.push(new ClassSelector(ctx.getText().substring(1)));
    }

    @Override
    public void exitClassSelector(ICSSParser.ClassSelectorContext ctx) {
        ClassSelector classSelector = (ClassSelector) currentContainer.pop();
        currentContainer.peek().addChild(classSelector);
    }
// Id selector
    @Override
    public void enterIdSelector(ICSSParser.IdSelectorContext ctx) {
        currentContainer.push(new IdSelector(ctx.getText().substring(1)));
    }

    @Override
    public void exitIdSelector(ICSSParser.IdSelectorContext ctx) {
        IdSelector idSelector = (IdSelector) currentContainer.pop();
        currentContainer.peek().addChild(idSelector);
    }
// Declaration
    @Override
    public void enterDeclaration(ICSSParser.DeclarationContext ctx) {
        currentContainer.push(new Declaration());

    }

    @Override
    public void exitDeclaration(ICSSParser.DeclarationContext ctx) {
        Declaration declaration = (Declaration) currentContainer.pop();
        if (exprStack.isEmpty()) {
            declaration.setError("Expression ontbreekt in declaration op regel " + ctx.getStart().getLine());
        }
        declaration.expression = exprStack.pop();
        currentContainer.peek().addChild(declaration);
    }
// Property
    @Override
    public void exitProperty(ICSSParser.PropertyContext ctx) {
        Declaration declaration = (Declaration) currentContainer.peek();
        declaration.property = new PropertyName(ctx.getText());
    }
//Literals
    // PixelLiteral
    @Override
    public void exitPixelLiteral(ICSSParser.PixelLiteralContext ctx) {
        String text = ctx.getText();
        int value = Integer.parseInt(text.substring(0, text.length() - 2));
        exprStack.push(new PixelLiteral(value));
    }
// ScarletLiteral
    @Override
    public void exitScalarLiteral(ICSSParser.ScalarLiteralContext ctx) {
        exprStack.push(new ScalarLiteral(Integer.parseInt(ctx.getText())));
    }
//ExitLiteral
    @Override
    public void exitColorLiteral(ICSSParser.ColorLiteralContext ctx) {
        exprStack.push(new ColorLiteral(ctx.getText()));
    }
// Bool
    // true
    @Override
    public void exitTrueLiteral(ICSSParser.TrueLiteralContext ctx) {
        exprStack.push(new BoolLiteral(true));
    }
//false
    @Override
    public void exitFalseLiteral(ICSSParser.FalseLiteralContext ctx) {
        exprStack.push(new BoolLiteral(false));
    }
    // Percentage literal
    @Override
    public void exitPercentageLiteral(ICSSParser.PercentageLiteralContext ctx) {
        String text = ctx.getText();
        int value = Integer.parseInt(text.substring(0, text.length() - 1));
        exprStack.push(new PercentageLiteral(value));
    }

    @Override
    public void exitVarRef(ICSSParser.VarRefContext ctx) {
        exprStack.push(new VariableReference(ctx.getText()));
    }
// Plus
    @Override
    public void exitAddOperation(ICSSParser.AddOperationContext ctx) {
        Expression right = exprStack.pop();
        Expression left = exprStack.pop();
        AddOperation op = new AddOperation();
        op.lhs = left;
        op.rhs = right;
        exprStack.push(op);
    }
// Min
    @Override
    public void exitSubOperation(ICSSParser.SubOperationContext ctx) {
        Expression right = exprStack.pop();
        Expression left = exprStack.pop();
        SubtractOperation op = new SubtractOperation();
        op.lhs = left;
        op.rhs = right;
        exprStack.push(op);
    }
// Keer
    @Override
    public void exitMulOperation(ICSSParser.MulOperationContext ctx) {
        Expression right = exprStack.pop();
        Expression left = exprStack.pop();
        MultiplyOperation op = new MultiplyOperation();
        op.lhs = left;
        op.rhs = right;
        exprStack.push(op);
    }
// If statement
    @Override
    public void enterIfClause(ICSSParser.IfClauseContext ctx) {
        IfClause ifClause = new IfClause();
        currentContainer.push(ifClause);
        ifStack.push(ifClause);


    }

    @Override
    public void exitIfClause(ICSSParser.IfClauseContext ctx) {
        IfClause ifClause = (IfClause) currentContainer.pop();
        ifStack.pop();
        currentContainer.peek().addChild(ifClause);
    }
// Else statement
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
    public void exitCondition(ICSSParser.ConditionContext ctx) {
        if (exprStack.isEmpty()) {
            throw new RuntimeException("Condition expression empty" + ctx.getStart().getLine());
        }

        IfClause owner = ifStack.peek();
        owner.conditionalExpression = exprStack.pop();
    }

}






