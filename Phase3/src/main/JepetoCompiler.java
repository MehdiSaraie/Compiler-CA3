package main;

import main.ast.nodes.Program;
import main.visitor.*;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import parsers.*;

public class JepetoCompiler {
    public void compile(CharStream textStream) {
        JepetoLexer jepetoLexer = new JepetoLexer(textStream);
        CommonTokenStream tokenStream = new CommonTokenStream(jepetoLexer);

        JepetoParser jepetoParser = new JepetoParser(tokenStream);
        Program program = jepetoParser.jepeto().jepetoProgram;
        ErrorReporter errorReporter = new ErrorReporter();
        ASTTreePrinter astTreePrinter = new ASTTreePrinter();

        TypeSetter typeSetter = new TypeSetter();
        TypeInference typeInference = new TypeInference();

        typeSetter.set_inference(typeInference);
        typeInference.set_setter(typeSetter);

        TypeSetterErrorCatcher typeSetterErrorCatcher = new TypeSetterErrorCatcher();
        TypeInferenceErrorCatcher typeInferenceErrorCatcher = new TypeInferenceErrorCatcher();

        typeSetterErrorCatcher.set_type_inference_error_catcher(typeInferenceErrorCatcher);
        typeInferenceErrorCatcher.set_typeSetter_error_catcher(typeSetterErrorCatcher);

        NameAnalyser nameAnalyser = new NameAnalyser();
        program.accept(nameAnalyser);

        int numberOfErrors = program.accept(errorReporter);

        if(numberOfErrors > 0)
            System.exit(1);

        //program.accept(astTreePrinter);   //Not used anymore in phase 3
        //TODO

        program.accept(typeSetter);

        program.accept(typeSetterErrorCatcher);
        numberOfErrors = program.accept(errorReporter);

        if(numberOfErrors == 0)
            System.out.println("Compilation successful");
    }
}
