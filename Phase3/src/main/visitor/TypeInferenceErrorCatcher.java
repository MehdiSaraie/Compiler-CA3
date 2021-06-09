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
    private TypeSetterErrorCatcher typeSetterErrorCatcher;

    public void set_typeSetter_error_catcher(TypeSetterErrorCatcher _typeSetterErrorCatcher){
        this.typeSetterErrorCatcher = _typeSetterErrorCatcher;
    }

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
                if (operator.equals(BinaryOperator.gt) || operator.equals(BinaryOperator.lt))
                    return new BoolType();
                else
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
            if (tl instanceof IntType && tr instanceof IntType || tl instanceof BoolType && tr instanceof BoolType || tl instanceof StringType && tr instanceof StringType)
                return new BoolType();
            if (tl instanceof FptrType && tr instanceof FptrType){
                try {
                    FunctionSymbolTableItem left_funcSym = (FunctionSymbolTableItem) (SymbolTable.root.getItem(FunctionSymbolTableItem.START_KEY + ((FptrType) tl).getFunctionName()));
                    FunctionSymbolTableItem right_funcSym = (FunctionSymbolTableItem) (SymbolTable.root.getItem(FunctionSymbolTableItem.START_KEY + ((FptrType) tr).getFunctionName()));
                    ArrayList<Type> left_types = (ArrayList<Type>) (left_funcSym.getArgTypes().clone());
                    left_types.add(left_funcSym.getReturnType());
                    ArrayList<Type> right_types = (ArrayList<Type>) (right_funcSym.getArgTypes().clone());
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
                            if ((left_el_type instanceof NoType || left_el_type == null || left_el_type instanceof IntType) && (right_el_type instanceof IntType || right_el_type instanceof NoType || right_el_type == null) ||
                                    (left_el_type instanceof NoType || left_el_type == null || left_el_type instanceof BoolType) && (right_el_type instanceof BoolType || right_el_type instanceof NoType || right_el_type == null) ||
                                    (left_el_type instanceof NoType || left_el_type == null || left_el_type instanceof StringType) && (right_el_type instanceof StringType || right_el_type instanceof NoType || right_el_type == null) ||
                                    (left_el_type instanceof NoType || left_el_type == null || left_el_type instanceof VoidType) && (right_el_type instanceof VoidType || right_el_type instanceof NoType || right_el_type == null) ||
                                    (left_el_type instanceof NoType || left_el_type == null || left_el_type instanceof FptrType) && (right_el_type instanceof FptrType || right_el_type instanceof NoType || right_el_type == null))
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

            if (tl instanceof VoidType){
                binaryExpression.addError(new CantUseValueOfVoidFunction(binaryExpression.getLine()));
                if (tr instanceof ListType) {
                    binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(), operator.name()));
                }
                if (tr instanceof VoidType){
                    binaryExpression.addError(new CantUseValueOfVoidFunction(binaryExpression.getLine()));
                }
                return new NoType();
            }
            if (tr instanceof VoidType){
                if (tl instanceof ListType) {
                    binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(), operator.name()));
                }
                binaryExpression.addError(new CantUseValueOfVoidFunction(binaryExpression.getLine()));
                return new NoType();
            }

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
                if (left_el_type instanceof IntType && right_el_type instanceof IntType ||
                        left_el_type instanceof BoolType && right_el_type instanceof BoolType ||
                        left_el_type instanceof StringType && right_el_type instanceof StringType)
                    return new ListType(tr);
                if (left_el_type instanceof NoType && (right_el_type instanceof IntType || right_el_type instanceof BoolType || right_el_type instanceof StringType || right_el_type instanceof ListType))
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
        try {
            FunctionSymbolTableItem functionSymbolTableItem = (FunctionSymbolTableItem) (SymbolTable.root.getItem(FunctionSymbolTableItem.START_KEY + anonymousFunction.getName()));
            TypeSetter.func_stack.push(functionSymbolTableItem);
            anonymousFunction.getBody().accept(this);
            TypeSetter.func_stack.pop();
            return new FptrType(anonymousFunction.getName());
        }catch (ItemNotFoundException e){}
        return new NoType();
    }

    @Override
    public Type visit(Identifier identifier) {
        try {
            FunctionSymbolTableItem functionSymbolTableItem = (FunctionSymbolTableItem) (SymbolTable.root.getItem(FunctionSymbolTableItem.START_KEY + identifier.getName()));
            return new FptrType(identifier.getName());
        }
        catch(ItemNotFoundException e1){
            FunctionSymbolTableItem cur_func = TypeSetterErrorCatcher.func_stack.pop();
            TypeSetterErrorCatcher.func_stack.push(cur_func);
            SymbolTable function_symbol_table = cur_func.getFunctionSymbolTable();
            try {
                VariableSymbolTableItem var_symbol_table = (VariableSymbolTableItem) function_symbol_table.getItem("Var_" + identifier.getName());
                return var_symbol_table.getType();
            }
            catch(ItemNotFoundException e2){}
        }
        return new NoType();
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
        if (index_type instanceof VoidType){
            listAccessByIndex.addError(new CantUseValueOfVoidFunction(listAccessByIndex.getLine()));
        }
        else if (!(index_type instanceof NoType || index_type instanceof IntType)){
            listAccessByIndex.addError(new ListIndexNotInt(listAccessByIndex.getLine()));
        }
        if (instance_type instanceof VoidType){
            listAccessByIndex.addError(new CantUseValueOfVoidFunction(listAccessByIndex.getLine()));
        }
        else if (!(instance_type instanceof NoType || instance_type instanceof ListType)){
            listAccessByIndex.addError(new ListAccessByIndexOnNoneList(listAccessByIndex.getLine()));
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

        Type fptr2 = instance.accept(this);
        if (!(fptr2 instanceof FptrType)){
            if (!(fptr2 instanceof NoType))
                funcCall.addError(new CallOnNoneFptrType(funcCall.getLine()));
            if (args.isEmpty()) {
                for (Map.Entry<Identifier, Expression> arg_with_key : funcCall.getArgsWithKey().entrySet()) {
                    arg_with_key.getValue().accept(this);
                }
            }
            else{
                for (Expression arg : args)
                    arg.accept(this);
            }
            return new NoType();
        }
        FptrType fptr = (FptrType)fptr2;
        try{
            FunctionSymbolTableItem functionSymbolTableItem = (FunctionSymbolTableItem)(SymbolTable.root.getItem(FunctionSymbolTableItem.START_KEY + fptr.getFunctionName()));
            FunctionDeclaration functionDeclaration = functionSymbolTableItem.getFuncDeclaration();
            Type arg_type = new NoType();
            int arg_index = 0;
            boolean flag = false;
            for (Identifier arg : functionDeclaration.getArgs()) {
                if (args.isEmpty()) {
                    if (arg_index >= funcCall.getArgsWithKey().size()){
                        flag = true;
                        break;
                    }
                    for (Map.Entry<Identifier, Expression> arg_with_key : funcCall.getArgsWithKey().entrySet()) {
                        if (arg_with_key.getKey().getName().equals(arg.getName())) {
                            arg_type = arg_with_key.getValue().accept(this);
                            break;
                        }
                    }
                }
                else {
                    if (arg_index >= args.size()){
                        flag = true;
                        break;
                    }
                    arg_type = args.get(arg_index).accept(this);
                }

                if(TypeSetter.visited_function_name.contains(functionDeclaration.getFunctionName().getName())) {
                    Type func_arg_type = functionSymbolTableItem.getArgTypes().get(arg_index);
                    if (TypeSetterErrorCatcher.checkTypeEquality(func_arg_type, arg_type))
                        flag = true;
                }

                arg_index++;
            }

            if (arg_index < args.size()){
                flag = true;
                for(int index = arg_index; index < args.size(); index++)
                    args.get(arg_index).accept(this);
            }

            if (flag){
                funcCall.addError(new FunctionCallNotMatchDefinition(funcCall.getLine()));
                return new NoType();
            }


            //check for loop
            if(!TypeSetterErrorCatcher.visited_function_name.contains(functionDeclaration.getFunctionName().getName())){
                TypeSetterErrorCatcher.visited_function_name.add(functionDeclaration.getFunctionName().getName());
                TypeSetterErrorCatcher.visited_function_declaration.add(functionDeclaration);
                functionDeclaration.accept(typeSetterErrorCatcher);
            }
            Type return_type = functionSymbolTableItem.getReturnType();
            if (return_type == null)
                return_type = new NoType();
            return return_type;
        }
        catch (ItemNotFoundException e){}
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
