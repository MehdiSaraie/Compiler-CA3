package main.visitor;

import main.ast.nodes.*;
import main.ast.nodes.declaration.*;
import main.ast.nodes.expression.*;
import main.ast.nodes.expression.values.*;
import main.ast.nodes.expression.values.primitive.*;
import main.ast.nodes.statement.*;
import main.ast.types.*;
import main.ast.types.functionPointer.FptrType;
import main.ast.types.single.BoolType;
import main.compileErrors.typeErrors.ConditionNotBool;
import main.compileErrors.typeErrors.ReturnValueNotMatchFunctionReturnType;
import main.compileErrors.typeErrors.UnsupportedTypeForPrint;
import main.symbolTable.*;
import main.symbolTable.exceptions.*;
import main.symbolTable.items.*;
import main.symbolTable.utils.Stack;

import java.util.*;

public class TypeSetterErrorCatcher  extends Visitor<Void> {
    public static main.symbolTable.utils.Stack<FunctionSymbolTableItem> func_stack = new Stack<>();
    public static ArrayList<String> visited_function_name = new ArrayList<>();
    public static ArrayList<FunctionDeclaration> visited_function_declaration = new ArrayList<>();

    private TypeInferenceErrorCatcher typeInferenceErrorCatcher = new TypeInferenceErrorCatcher();
    @Override
    public Void visit(Program program) {
        program.getMain().accept(this);
        return null;
    }

    @Override
    public Void visit(FunctionDeclaration funcDeclaration) {
        try {
            FunctionSymbolTableItem functionSymbolTableItem = (FunctionSymbolTableItem) (SymbolTable.root.getItem(FunctionSymbolTableItem.START_KEY + funcDeclaration.getFunctionName()));
            TypeSetter.func_stack.push(functionSymbolTableItem);
            funcDeclaration.getBody().accept(this);
            TypeSetter.func_stack.pop();
        }catch (ItemNotFoundException e){}
        return null;
    }

    @Override
    public Void visit(MainDeclaration mainDeclaration) {
        mainDeclaration.getBody().accept(this);
        return null;
    }

    @Override
    public Void visit(BlockStmt blockStmt) {
        for (Statement stat : blockStmt.getStatements())
            stat.accept(this);
        return null;
    }

    @Override
    public Void visit(ConditionalStmt conditionalStmt) {
        Type cond_type = conditionalStmt.getCondition().accept(typeInferenceErrorCatcher);
        if (!(cond_type instanceof BoolType || cond_type instanceof NoType)){
            conditionalStmt.addError(new ConditionNotBool(conditionalStmt.getLine()));
        }
        conditionalStmt.getThenBody().accept(this);
        Statement else_body = conditionalStmt.getElseBody();
        if (else_body != null)
            else_body.accept(this);
        return null;
    }

    @Override
    public Void visit(FunctionCallStmt funcCallStmt) {
        funcCallStmt.getFunctionCall().accept(typeInferenceErrorCatcher);
        return null;
    }

    @Override
    public Void visit(PrintStmt print) {
        Type arg_type = print.getArg().accept(typeInferenceErrorCatcher);
        if (arg_type instanceof FptrType || arg_type instanceof VoidType){
            print.addError(new UnsupportedTypeForPrint(print.getLine()));
        }
        return null;
    }

    @Override
    public Void visit(ReturnStmt returnStmt) {
        Type cur_return_type = returnStmt.getReturnedExpr().accept(typeInferenceErrorCatcher);
        FunctionSymbolTableItem cur_func = TypeSetter.func_stack.pop();
        TypeSetter.func_stack.push(cur_func);
        Type function_return_type = cur_func.getReturnType();

        if(function_return_type == null || function_return_type instanceof NoType){
            cur_func.setReturnType(cur_return_type);
        }
        // TODO error return type

        return null;
    }
}
