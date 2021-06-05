package main.visitor;

import main.ast.nodes.declaration.*;
import main.ast.nodes.expression.*;
import main.ast.nodes.expression.operators.BinaryOperator;
import main.ast.nodes.expression.operators.UnaryOperator;
import main.ast.nodes.expression.values.*;
import main.ast.nodes.expression.values.primitive.*;
import main.ast.types.*;
import main.ast.types.functionPointer.FptrType;
import main.ast.types.list.ListType;
import main.ast.types.single.*;
import main.compileErrors.typeErrors.*;
import main.symbolTable.SymbolTable;
import main.symbolTable.exceptions.ItemNotFoundException;
import main.symbolTable.items.*;
import java.util.*;

public class TypeInferenceErrorCatcher extends Visitor<Type> {
    private TypeSetter typeSetter = new TypeSetter();
    @Override
    public Type visit(BinaryExpression binaryExpression) {
        Expression left = binaryExpression.getFirstOperand();
        Expression right = binaryExpression.getSecondOperand();

        Type tl = left.accept(this);
        Type tr = right.accept(this);
        BinaryOperator operator =  binaryExpression.getBinaryOperator();

        if (operator.equals(BinaryOperator.and) || operator.equals(BinaryOperator.or)){
            if (tl instanceof BoolType && tr instanceof BoolType)
                return new BoolType();

            if ((tl instanceof NoType || tl instanceof BoolType) && (tr instanceof BoolType || tr instanceof NoType))
                return new NoType();

            if (tl instanceof VoidType){
                binaryExpression.addError(new CantUseValueOfVoidFunction(binaryExpression.getLine()));
                if (!(tr instanceof NoType || tr instanceof BoolType || tr instanceof VoidType)) {
                    binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(), operator.name()));
                }
                if (tr instanceof VoidType){
                    binaryExpression.addError(new CantUseValueOfVoidFunction(binaryExpression.getLine()));
                }
                return new NoType();
            }
            if (tr instanceof VoidType){
                if (!(tl instanceof NoType || tl instanceof BoolType)) {
                    binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(), operator.name()));
                }
                binaryExpression.addError(new CantUseValueOfVoidFunction(binaryExpression.getLine()));
                return new NoType();
            }
        }

        //TODO
        if (operator.equals(BinaryOperator.add) || operator.equals(BinaryOperator.sub) || operator.equals(BinaryOperator.mult) ||
                operator.equals(BinaryOperator.div) || operator.equals(BinaryOperator.gt) || operator.equals(BinaryOperator.lt)){
            if (tl instanceof IntType && tr instanceof IntType)
                return new IntType();

            if ((tl instanceof NoType || tl instanceof IntType) && (tr instanceof IntType || tr instanceof NoType))
                return new NoType();

            if (tl instanceof VoidType){
                binaryExpression.addError(new CantUseValueOfVoidFunction(binaryExpression.getLine()));
                if (!(tr instanceof NoType || tr instanceof IntType || tr instanceof VoidType)) {
                    binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(), operator.name()));
                }
                if (tr instanceof VoidType){
                    binaryExpression.addError(new CantUseValueOfVoidFunction(binaryExpression.getLine()));
                }
                return new NoType();
            }
            if (tr instanceof VoidType){
                if (!(tl instanceof NoType || tl instanceof IntType)) {
                    binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(), operator.name()));
                }
                binaryExpression.addError(new CantUseValueOfVoidFunction(binaryExpression.getLine()));
                return new NoType();
            }
        }

        if (operator.equals(BinaryOperator.eq) || operator.equals(BinaryOperator.neq)){
            if (tl instanceof IntType && tr instanceof IntType || tl instanceof BoolType && tr instanceof BoolType ||
                    tl instanceof StringType && tr instanceof StringType || tl instanceof VoidType && tr instanceof VoidType)
                return new BoolType();
            if (tl instanceof FptrType && tr instanceof FptrType){
                try {
                    FunctionSymbolTableItem left_funcSym = (FunctionSymbolTableItem) (SymbolTable.root.getItem(FunctionSymbolTableItem.START_KEY + ((FptrType) tl).getFunctionName()));
                    FunctionSymbolTableItem right_funcSym = (FunctionSymbolTableItem) (SymbolTable.root.getItem(FunctionSymbolTableItem.START_KEY + ((FptrType) tr).getFunctionName()));
                    ArrayList<Type> left_types = left_funcSym.getArgTypes();
                    left_types.add(left_funcSym.getReturnType());
                    ArrayList<Type> right_types = right_funcSym.getArgTypes();
                    right_types.add(right_funcSym.getReturnType());
                    boolean flag = true;
                    if (left_types.size() == right_types.size()) {
                        flag = false;
                        for (int i = 0; i < left_types.size(); i++) {
                            Type left_type = left_types.get(i);
                            Type right_type = right_types.get(i);
                            Type left_el_type = left_type;
                            Type right_el_type = right_type;
                            while (left_el_type instanceof ListType){
                                if (right_el_type instanceof ListType) {
                                    left_el_type = ((ListType) left_el_type).getType();
                                    right_el_type = ((ListType) right_el_type).getType();
                                }
                                else{
                                    flag = true;
                                    break;
                                }
                            }
                            if ((left_el_type instanceof NoType || left_el_type instanceof IntType) && (right_el_type instanceof IntType || right_el_type instanceof NoType) ||
                                    (left_el_type instanceof NoType || left_el_type instanceof BoolType) && (right_el_type instanceof BoolType || right_el_type instanceof NoType) ||
                                    (left_el_type instanceof NoType || left_el_type instanceof StringType) && (right_el_type instanceof StringType || right_el_type instanceof NoType) ||
                                    (left_el_type instanceof NoType || left_el_type instanceof VoidType) && (right_el_type instanceof VoidType || right_el_type instanceof NoType) ||
                                    (left_el_type instanceof NoType || left_el_type instanceof FptrType) && (right_el_type instanceof FptrType || right_el_type instanceof NoType))
                                continue;
                            else{
                                flag = true;
                                break;
                            }
                        }
                    }
                    if (!flag)
                        return new BoolType();
                }
                catch (ItemNotFoundException e){
                    return new NoType();
                }

            }
            if (tl instanceof VoidType && tr instanceof FptrType || tr instanceof VoidType && tl instanceof FptrType)
                return new BoolType();
            if (!(tl instanceof ListType) && tr instanceof NoType || !(tr instanceof ListType) && tl instanceof NoType)
                return new NoType();
        }

        if (operator.equals(BinaryOperator.append)){
            if (tl instanceof NoType && !(tr instanceof FptrType || tr instanceof VoidType))
                return new NoType();
            if (tl instanceof ListType && tr instanceof NoType)
                return new ListType(((ListType)tl).getType());

            if (tl instanceof ListType) {
                Type left_el_type = ((ListType)tl).getType();
                Type right_el_type = tr;
                while (left_el_type instanceof ListType){
                    if (right_el_type instanceof ListType) {
                        left_el_type = ((ListType) left_el_type).getType();
                        right_el_type = ((ListType) right_el_type).getType();
                    }
                    else
                        break;
                }
                if ((left_el_type instanceof NoType || left_el_type instanceof IntType) && right_el_type instanceof IntType ||
                        (left_el_type instanceof NoType || left_el_type instanceof BoolType) && right_el_type instanceof BoolType ||
                        (left_el_type instanceof NoType || left_el_type instanceof StringType) && right_el_type instanceof StringType)
                    return new ListType(tr);
                if ((left_el_type instanceof NoType || left_el_type instanceof IntType) && right_el_type instanceof NoType ||
                        (left_el_type instanceof NoType || left_el_type instanceof BoolType) && right_el_type instanceof NoType ||
                        (left_el_type instanceof NoType || left_el_type instanceof StringType) && right_el_type instanceof NoType)
                    return new ListType(((ListType)tl).getType());
            }

            if (tl instanceof VoidType){
                binaryExpression.addError(new CantUseValueOfVoidFunction(binaryExpression.getLine()));
                if (tr instanceof FptrType) {
                    binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(), operator.name()));
                }
                if (tr instanceof VoidType){
                    binaryExpression.addError(new CantUseValueOfVoidFunction(binaryExpression.getLine()));
                }
                return new NoType();
            }
            if (tr instanceof VoidType){
                if (!(tl instanceof NoType || tl instanceof ListType)) {
                    binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(), operator.name()));
                }
                binaryExpression.addError(new CantUseValueOfVoidFunction(binaryExpression.getLine()));
                return new NoType();
            }
        }
        binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(), operator.name()));
        return new NoType();
    }

    @Override
    public Type visit(UnaryExpression unaryExpression) {
        //TODO
        Expression operand = unaryExpression.getOperand();

        Type op_type = operand.accept(this);
        UnaryOperator operator = unaryExpression.getOperator();


        if (operator.equals(UnaryOperator.not) && op_type instanceof BoolType)
            return new BoolType();

        if (operator.equals(UnaryOperator.minus) && op_type instanceof IntType)
            return new IntType();

        if (op_type instanceof NoType)
            return new NoType();
        if (op_type instanceof VoidType){
            unaryExpression.addError(new CantUseValueOfVoidFunction(unaryExpression.getLine()));
            return new NoType();
        }
        unaryExpression.addError(new UnsupportedOperandType(unaryExpression.getLine(), operator.name()));
        return new NoType();
    }

    @Override
    public Type visit(AnonymousFunction anonymousFunction) {
        //TODO
        return null;
    }

    @Override
    public Type visit(Identifier identifier) {
        //TODO
        return null;
    }

    @Override
    public Type visit(ListAccessByIndex listAccessByIndex) {
        //TODO
        Expression instance = listAccessByIndex.getInstance();
        Expression index = listAccessByIndex.getIndex();
        Type instance_type = instance.accept(this);
        Type index_type = index.accept(this);
        if (instance_type instanceof ListType && index_type instanceof IntType){
            Type el_type = ((ListType)instance_type).getType();
            if (el_type instanceof IntType)
                return new IntType();
            if (el_type instanceof StringType)
                return new StringType();
            if (el_type instanceof BoolType)
                return new BoolType();
            if (el_type instanceof NoType)
                return new NoType();
            if (el_type instanceof ListType)
                return new ListType(((ListType)el_type).getType());
        }
        if ((instance_type instanceof ListType || instance_type instanceof NoType) && (index_type instanceof IntType || index_type instanceof NoType))
            return new NoType();
        if (instance_type instanceof VoidType){
            listAccessByIndex.addError(new CantUseValueOfVoidFunction(listAccessByIndex.getLine()));
        }
        else{
            listAccessByIndex.addError(new ListAccessByIndexOnNoneList(listAccessByIndex.getLine()));
        }
        if (index_type instanceof VoidType){
            listAccessByIndex.addError(new CantUseValueOfVoidFunction(listAccessByIndex.getLine()));
        }
        else{
            listAccessByIndex.addError(new ListIndexNotInt(listAccessByIndex.getLine()));
        }
        return new NoType();
    }

    @Override
    public Type visit(ListSize listSize) {
        //TODO
        Expression instance = listSize.getInstance();
        Type instance_type = instance.accept(this);
        if (instance_type instanceof ListType)
            return new IntType();
        if (instance_type instanceof NoType)
            return new NoType();
        if (instance_type instanceof VoidType){
            listSize.addError(new CantUseValueOfVoidFunction(listSize.getLine()));
            return new NoType();
        }
        listSize.addError(new GetSizeOfNoneList(listSize.getLine()));
        return new NoType();
    }

    @Override
    public Type visit(FunctionCall funcCall) {
        //TODO
        Expression instance = funcCall.getInstance();
        ArrayList<Expression> args = funcCall.getArgs();
        Map<Identifier, Expression> argsWithKey = funcCall.getArgsWithKey();
        FptrType fptr = (FptrType)instance.accept(this);
        try{
            FunctionSymbolTableItem functionSymbolTableItem = (FunctionSymbolTableItem)(SymbolTable.root.getItem(FunctionSymbolTableItem.START_KEY + fptr.getFunctionName()));
            FunctionDeclaration functionDeclaration = functionSymbolTableItem.getFuncDeclaration();
            Type arg_type;
            for (Expression arg: args){
                arg_type = arg.accept(this);
                functionSymbolTableItem.addArgType(arg_type);

            }
            for (Identifier arg_name : functionDeclaration.getArgs()){
                arg_type = argsWithKey.get(arg_name).accept(this);
                functionSymbolTableItem.addArgType(arg_type);
            }
            // this part may be in TypeSetter (in FunctionDeclaration)
            //SymbolTable.push(functionSymbolTableItem);
            functionDeclaration.accept(typeSetter);
            return functionSymbolTableItem.getReturnType();

        }
        catch (ItemNotFoundException e){
        }
        return new NoType();
    }

    @Override
    public Type visit(ListValue listValue) {
        //TODO
        ArrayList<Expression> elements = listValue.getElements();
        Type old_type = new NoType();
        boolean diff_flag = false;
        for (Expression element: elements){
            Type new_type = element.accept(this);
            if (new_type instanceof NoType){
                continue;
            }
            if (new_type instanceof VoidType){
                listValue.addError(new CantUseValueOfVoidFunction(listValue.getLine()));
                continue;
            }
            if (old_type instanceof NoType){
                old_type = new_type;
                continue;
            }
            Type old_el_type = old_type;
            Type new_el_type = new_type;
            while (old_el_type instanceof ListType){
                if (new_el_type instanceof ListType) {
                    old_el_type = ((ListType) old_el_type).getType();
                    new_el_type = ((ListType) new_el_type).getType();
                }
                else
                    diff_flag = true;
            }
            if ((old_el_type instanceof NoType || old_el_type instanceof IntType) && new_el_type instanceof IntType ||
                    (old_el_type instanceof NoType || old_el_type instanceof BoolType) && new_el_type instanceof BoolType ||
                    (old_el_type instanceof NoType || old_el_type instanceof StringType) && new_el_type instanceof StringType)
                old_type = new_type;
            else if ((old_el_type instanceof NoType || old_el_type instanceof IntType) && new_el_type instanceof NoType ||
                    (old_el_type instanceof NoType || old_el_type instanceof BoolType) && new_el_type instanceof NoType ||
                    (old_el_type instanceof NoType || old_el_type instanceof StringType) && new_el_type instanceof NoType)
                continue;

            else
                diff_flag = true;
        }
        if (diff_flag){
            listValue.addError(new ListElementsTypeNotMatch(listValue.getLine()));
            return new NoType();
        }
        return new ListType(old_type);
    }

    @Override
    public Type visit(IntValue intValue) {
        //TODO
        return new IntType();
    }

    @Override
    public Type visit(BoolValue boolValue) {
        //TODO
        return new BoolType();
    }

    @Override
    public Type visit(StringValue stringValue) {
        //TODO
        return new StringType();
    }

    @Override
    public Type visit(VoidValue voidValue) {
        //TODO
        return new VoidType();
    }
}
