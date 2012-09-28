package com.github.uchan_nos.c_helper.pointer.test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.*;

import static org.junit.Assert.*;

import org.junit.Test;

import com.github.uchan_nos.c_helper.analysis.Parser;

import com.github.uchan_nos.c_helper.pointer.MallocCallFinder;

import com.github.uchan_nos.c_helper.util.Util;

public class MallocCallFinderTest {
    @Test
    public void findPathTest1() {
        final String src =
            "#include <stdlib.h>\n" +
            "void f(void) {\n" +
            "  char *p = malloc(10), *q;\n" +
            "  q = NULL;\n" +
            "  q = malloc(20);\n" +
            "  p = q = malloc(30);\n" +
            "}\n";

        IASTTranslationUnit tu = new Parser("", src).parseOrNull();
        List<IASTDeclaration> decls = extractPartOfTUDeclarations(tu.getDeclarations());

        List<List<IASTNode>> pathToMalloc = null;

        // char *p = malloc(10), *q;
        pathToMalloc = MallocCallFinder.findPathToMalloc(Util.getChildNode(decls.get(0), 2, 0));
        assertEquals(1, pathToMalloc.size());
        assertEquals(5, pathToMalloc.get(0).size());
        assertTrue(isSameOrder(pathToMalloc.get(0),
                    IASTDeclarationStatement.class,
                    IASTSimpleDeclaration.class,
                    IASTDeclarator.class,
                    IASTEqualsInitializer.class,
                    IASTFunctionCallExpression.class
                    ));

        // q = NULL;
        pathToMalloc = MallocCallFinder.findPathToMalloc(Util.getChildNode(decls.get(0), 2, 1));
        assertEquals(0, pathToMalloc.size());

        // p = q = malloc(30);
        pathToMalloc = MallocCallFinder.findPathToMalloc(Util.getChildNode(decls.get(0), 2, 3));
        assertEquals(1, pathToMalloc.size());
        assertEquals(4, pathToMalloc.get(0).size());
        assertTrue(isSameOrder(pathToMalloc.get(0),
                    IASTExpressionStatement.class,
                    IASTBinaryExpression.class,
                    IASTBinaryExpression.class,
                    IASTFunctionCallExpression.class
                    ));

        pathToMalloc = MallocCallFinder.findPathToMalloc(Util.getChildNode(decls.get(0), 2));
        assertEquals(3, pathToMalloc.size());
    }

    private List<IASTDeclaration> extractPartOfTUDeclarations(IASTDeclaration[] declarations) {
        ArrayList<IASTDeclaration> result = new ArrayList<IASTDeclaration>();
        for (IASTDeclaration d : declarations) {
            if (d.isPartOfTranslationUnitFile()) {
                result.add(d);
            }
        }
        return result;
    }

    private boolean isSameOrder(List<IASTNode> path, Class<?>... classes) {
        Iterator<IASTNode> it = path.iterator();
        for (Class<?> c : classes) {
            if (!it.hasNext()) {
                return false;
            }
            IASTNode node = it.next();
            if (!c.isInstance(node)) {
                return false;
            }
        }
        return true;
    }
}
