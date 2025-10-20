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

/**
 * This class extracts the ICSS Abstract Syntax Tree from the Antlr Parse tree.
 */
public class ASTListener extends ICSSBaseListener {

    //Accumulator attributes:
    private AST ast;

    //Use this to keep track of the parent nodes when recursively traversing the ast
    private HANStack<ASTNode> currentContainer;

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
    }

    @Override
    public void exitDeclaration(ICSSParser.DeclarationContext ctx) {
        Declaration decl = (Declaration) currentContainer.pop();
        currentContainer.peek().addChild(decl);
    }

}

