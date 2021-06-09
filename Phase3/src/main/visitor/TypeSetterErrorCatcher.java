package main.visitor;

import main.ast.nodes.*;
import main.ast.nodes.declaration.*;
import main.ast.nodes.expression.*;
import main.ast.nodes.expression.values.*;
import main.ast.nodes.expression.values.primitive.*;
import main.ast.nodes.statement.*;
import main.ast.types.*;
import main.ast.types.functionPointer.FptrType;
import main.ast.types.list.ListType;
import main.ast.types.single.BoolType;
import main.ast.types.single.IntType;
import main.ast.types.single.StringType;
import main.compileErrors.typeErrors.CantUseValueOfVoidFunction;
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

    private TypeInferenceErrorCatcher typeInferenceErrorCatcher;

    public static boolean checkTypeEquality(Type tl, Type tr){
        boolean flag = false;
        if (tl instanceof FptrType && tr instanceof FptrType) {
            try {
                FunctionSymbolTableItem left_funcSym = (FunctionSymbolTableItem) (SymbolTable.root.getItem(FunctionSymbolTableItem.START_KEY + ((FptrType) tl).getFunctionName()));
                FunctionSymbolTableItem right_funcSym = (FunctionSymbolTableItem) (SymbolTable.root.getItem(FunctionSymbolTableItem.START_KEY + ((FptrType) tr).getFunctionName()));
                ArrayList<Type> left_types = (ArrayList<Type>) (left_funcSym.getArgTypes().clone());
                left_types.add(left_funcSym.getReturnType());
                ArrayList<Type> right_types = (ArrayList<Type>) (right_funcSym.getArgTypes().clone());
                right_types.add(right_funcSym.getReturnType());
                flag = true;
                if (left_types.size() == right_types.size()) {
                    flag = false;
                    for (int i = 0; i < left_types.size(); i++) {
                        Type left_type = left_types.get(i);
                        Type right_type = right_types.get(i);
                        Type left_el_type = left_type;
                        Type right_el_type = right_type;
                        while (left_el_type instanceof ListType) {
                            if (right_el_type instanceof ListType) {
                                left_el_type = ((ListType) left_el_type).getType();
                                right_el_type = ((ListType) right_el_type).getType();
                            }
                            else {
                                flag = true;
                                break;
                            }
                        }
                        if ((left_el_type instanceof NoType || left_el_type == null || left_el_type instanceof IntType) && (right_el_type instanceof IntType || right_el_type instanceof NoType || right_el_type == null) ||
                                (left_el_type instanceof NoType || left_el_type == null || left_el_type instanceof BoolType) && (right_el_type instanceof BoolType || right_el_type instanceof NoType || right_el_type == null) ||
                                (left_el_type instanceof NoType || left_el_type == null || left_el_type instanceof StringType) && (right_el_type instanceof StringType || right_el_type instanceof NoType || right_el_type == null) ||
                                (left_el_type instanceof NoType || left_el_type == null || left_el_type instanceof VoidType) && (right_el_type instanceof VoidType || right_el_type instanceof NoType || right_el_type == null) ||
                                (left_el_type instanceof NoType || left_el_type == null || left_el_type instanceof FptrType) && (right_el_type instanceof FptrType || right_el_type instanceof NoType || right_el_type == null))
                            continue;
                        else {
                            flag = true;
                            break;
                        }
                    }
                }
            }
            catch (ItemNotFoundException e) { }
        }

        else if (tl instanceof ListType && tr instanceof ListType){
            flag = false;
            Type left_el_type = tl;
            Type right_el_type = tr;
            while (left_el_type instanceof ListType){
                if (right_el_type instanceof ListType) {
                    left_el_type = ((ListType) left_el_type).getType();
                    right_el_type = ((ListType) right_el_type).getType();
                }
                else {
                    flag = true;
                    break;
                }
            }
            if (!((left_el_type instanceof NoType || left_el_type instanceof IntType) && (right_el_type instanceof IntType || right_el_type instanceof NoType) ||
                    (left_el_type instanceof NoType || left_el_type instanceof BoolType) && (right_el_type instanceof BoolType || right_el_type instanceof NoType) ||
                    (left_el_type instanceof NoType || left_el_type instanceof StringType) && (right_el_type instanceof StringType || right_el_type instanceof NoType)))
                flag = true;
        }

        else if(!((tl instanceof NoType || tl == null || tl instanceof IntType) && (tr instanceof IntType || tr instanceof NoType || tr == null) ||
                (tl instanceof NoType || tl == null || tl instanceof BoolType) && (tr instanceof BoolType || tr instanceof NoType || tr == null) ||
                (tl instanceof NoType || tl == null || tl instanceof StringType) && (tr instanceof StringType || tr instanceof NoType || tr == null) ||
                (tl instanceof NoType || tl == null || tl instanceof VoidType) && (tr instanceof VoidType || tr instanceof NoType || tr == null) ||
                (tl instanceof NoType || tl == null || tl instanceof FptrType) && (tr instanceof FptrType || tr instanceof NoType || tr == null) ||
                (tl instanceof NoType || tl == null || tl instanceof ListType) && (tr instanceof ListType || tr instanceof NoType || tr == null))){
            flag = true;
        }
        return flag;
    }

    public void set_type_inference_error_catcher(TypeInferenceErrorCatcher _typeInferenceErrorCatcher){
        this.typeInferenceErrorCatcher = _typeInferenceErrorCatcher;
    }

    @Override
    public Void visit(Program program) {
        program.getMain().accept(this);
        return null;
    }

    @Override
    public Void visit(FunctionDeclaration funcDeclaration) {
        try {
            FunctionSymbolTableItem functionSymbolTableItem = (FunctionSymbolTableItem) (SymbolTable.root.getItem(FunctionSymbolTableItem.START_KEY + funcDeclaration.getFunctionName().getName()));
            TypeSetterErrorCatcher.func_stack.push(functionSymbolTableItem);
            funcDeclaration.getBody().accept(this);
            TypeSetterErrorCatcher.func_stack.pop();
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
        if (cond_type instanceof VoidType){
            conditionalStmt.addError(new CantUseValueOfVoidFunction(conditionalStmt.getLine()));
        }
        else if (!(cond_type instanceof BoolType || cond_type instanceof NoType)){
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
        if(arg_type instanceof VoidType){
            print.addError(new CantUseValueOfVoidFunction(print.getLine()));
        }
        else if (arg_type instanceof FptrType){
            print.addError(new UnsupportedTypeForPrint(print.getLine()));
        }
        return null;
    }

    @Override
    public Void visit(ReturnStmt returnStmt) {
        Expression returned_expression = returnStmt.getReturnedExpr();
        Type cur_return_type = returned_expression.accept(typeInferenceErrorCatcher);
        FunctionSymbolTableItem cur_func = TypeSetterErrorCatcher.func_stack.pop();
        TypeSetterErrorCatcher.func_stack.push(cur_func);
        Type function_return_type = cur_func.getReturnType();

        if (cur_return_type instanceof VoidType && !(returned_expression instanceof VoidValue)){
            returnStmt.addError(new CantUseValueOfVoidFunction(returnStmt.getLine()));
            return null;
        }

        // TODO error return type
        boolean flag = checkTypeEquality(function_return_type, cur_return_type);
        if (flag)
            returnStmt.addError(new ReturnValueNotMatchFunctionReturnType(returnStmt.getLine()));
        return null;
    }
}
