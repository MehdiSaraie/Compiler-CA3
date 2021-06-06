package main.visitor;

import main.JepetoCompiler;
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
import main.compileErrors.typeErrors.ConditionNotBool;
import main.compileErrors.typeErrors.ReturnValueNotMatchFunctionReturnType;
import main.compileErrors.typeErrors.UnsupportedTypeForPrint;
import main.symbolTable.*;
import main.symbolTable.exceptions.*;
import main.symbolTable.items.*;
import main.symbolTable.utils.Stack;

import java.util.*;

public class TypeSetter  extends Visitor<Void> {
    public static Stack<FunctionSymbolTableItem> func_stack = new Stack<>();
    public static ArrayList<String> visited_function_name = new ArrayList<>();
    public static ArrayList<FunctionDeclaration> visited_function_declaration = new ArrayList<>();

    private TypeInference typeInference;

    public void set_inference(TypeInference _type_inference){
        this.typeInference = _type_inference;
    }

    public void set_function_return_type(){
        for(FunctionDeclaration func : visited_function_declaration) {
            try {
                FunctionSymbolTableItem func_symbol_table = (FunctionSymbolTableItem) (SymbolTable.root.getItem(FunctionSymbolTableItem.START_KEY + func.getFunctionName().getName()));
                if(func_symbol_table.getReturnType() == null || func_symbol_table.getReturnType() instanceof NoType){
                    func.accept(this);
                }
                if(func_symbol_table.getReturnType() == null || func_symbol_table.getReturnType() instanceof NoType){
                    func_symbol_table.setReturnType(new NoType());
                }
            }catch(ItemNotFoundException e){}
        }
    }

    @Override
    public Void visit(Program program) {
        program.getMain().accept(this);

        set_function_return_type();

        for(FunctionDeclaration func : program.getFunctions()){
            String function_name = func.getFunctionName().getName();
            if(visited_function_name.contains(function_name)) {
                try {
                    FunctionSymbolTableItem functionSymbolTableItem = (FunctionSymbolTableItem) (SymbolTable.root.getItem(FunctionSymbolTableItem.START_KEY + func.getFunctionName().getName()));
                    ArrayList<Type> argTypes = functionSymbolTableItem.getArgTypes();
                    System.out.println(function_name);
                    String type_report = "[";
                    int index = 0;
                    for (Type arg_t : argTypes) {
                        if (index == 0)
                            type_report += arg_t.toString();
                        else
                            type_report += ", " + arg_t.toString();
                        index += 1;
                    }
                    type_report += "]";
                    System.out.println(type_report);
                    System.out.println(functionSymbolTableItem.getReturnType().toString());
                } catch (ItemNotFoundException e) {
                }
            }
        }

        return null;
    }

    @Override
    public Void visit(FunctionDeclaration funcDeclaration) {
        try {
            FunctionSymbolTableItem functionSymbolTableItem = (FunctionSymbolTableItem) (SymbolTable.root.getItem(FunctionSymbolTableItem.START_KEY + funcDeclaration.getFunctionName().getName()));
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
        Type cond_type = conditionalStmt.getCondition().accept(typeInference);
        if (!(cond_type instanceof BoolType || cond_type instanceof NoType)){
            //if error
        }
        conditionalStmt.getThenBody().accept(this);
        Statement else_body = conditionalStmt.getElseBody();
        if (else_body != null)
            else_body.accept(this);
        return null;
    }

    @Override
    public Void visit(FunctionCallStmt funcCallStmt) {
        funcCallStmt.getFunctionCall().accept(typeInference);
        return null;
    }

    @Override
    public Void visit(PrintStmt print) {
        Type arg_type = print.getArg().accept(typeInference);
        if (arg_type instanceof FptrType || arg_type instanceof VoidType){
            //print error
        }
        return null;
    }

    @Override
    public Void visit(ReturnStmt returnStmt) {
        Type cur_return_type = returnStmt.getReturnedExpr().accept(typeInference);
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
