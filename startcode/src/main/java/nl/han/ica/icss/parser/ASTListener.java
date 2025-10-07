package nl.han.ica.icss.parser;

import java.util.Stack;


import nl.han.ica.datastructures.IHANStack;
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
	private IHANStack<ASTNode> currentContainer;

	public ASTListener() {
		ast = new AST();
		//currentContainer = new HANStack<>();
	}
    public AST getAST() {
        return ast;
    }

    @Override
    public void enterStylesheet (ICSSParser.styleruleContext ctx){
    Stylerule stylerule = new Stylerule();
    currentContainer.push(stylerule);
    }


    @Override
    public void exitStylerule (ICSSParser.styleruleContext ctx){
        Stylerule stylerule = (Stylerule)currentContainer.pop();
        currentContainer.peek().addChild(stylerule);
    }
    @Override
    public void exitStyleSheet(ICSSParser.StylesheetContext ctx) {
        Stylesheet stylesheet = (stylesheet) currentContainer.pop();
        ast.setRoot(stylesheet);
    }


    
}