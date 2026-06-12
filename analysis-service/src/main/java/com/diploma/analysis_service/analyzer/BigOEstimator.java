package com.diploma.analysis_service.analyzer;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Rough static estimate of time complexity for Java code.
 * Walks the AST, tracking max nested-loop depth per method, halving patterns
 * (i /= 2, i = i / 2, i >>= 1) and self-recursion.
 */
@Slf4j
@Component
public class BigOEstimator {

    public String estimate(String code) {
        if (code == null || code.isBlank()) return null;
        CompilationUnit cu;
        try {
            cu = StaticJavaParser.parse(code);
        } catch (Exception e) {
            log.debug("Big-O estimator: could not parse source: {}", e.getMessage());
            return null;
        }

        int maxNesting = 0;
        boolean anyHalving = false;
        int maxSelfCalls = 0;

        for (MethodDeclaration md : cu.findAll(MethodDeclaration.class)) {
            MethodSnapshot snap = analyzeMethod(md);
            maxNesting = Math.max(maxNesting, snap.maxNesting);
            if (snap.hasHalving) anyHalving = true;
            maxSelfCalls = Math.max(maxSelfCalls, snap.selfCalls);
        }

        if (maxSelfCalls >= 2) return "O(2^n)";
        if (maxSelfCalls == 1) return anyHalving ? "O(log n)" : "O(n)";
        if (maxNesting == 0) return "O(1)";
        if (maxNesting == 1) return anyHalving ? "O(log n)" : "O(n)";
        if (maxNesting == 2) return anyHalving ? "O(n log n)" : "O(n^2)";
        if (maxNesting == 3) return "O(n^3)";
        return "O(n^" + maxNesting + ")";
    }

    private static final class MethodSnapshot {
        int maxNesting;
        boolean hasHalving;
        int selfCalls;
    }

    private MethodSnapshot analyzeMethod(MethodDeclaration md) {
        MethodSnapshot a = new MethodSnapshot();
        String methodName = md.getNameAsString();
        md.accept(new VoidVisitorAdapter<Integer>() {
            @Override public void visit(ForStmt n, Integer depth) {
                int d = depth + 1;
                a.maxNesting = Math.max(a.maxNesting, d);
                if (loopHasHalving(n)) a.hasHalving = true;
                super.visit(n, d);
            }
            @Override public void visit(ForEachStmt n, Integer depth) {
                int d = depth + 1;
                a.maxNesting = Math.max(a.maxNesting, d);
                super.visit(n, d);
            }
            @Override public void visit(WhileStmt n, Integer depth) {
                int d = depth + 1;
                a.maxNesting = Math.max(a.maxNesting, d);
                if (loopHasHalving(n)) a.hasHalving = true;
                super.visit(n, d);
            }
            @Override public void visit(DoStmt n, Integer depth) {
                int d = depth + 1;
                a.maxNesting = Math.max(a.maxNesting, d);
                if (loopHasHalving(n)) a.hasHalving = true;
                super.visit(n, d);
            }
            @Override public void visit(MethodCallExpr n, Integer depth) {
                if (n.getNameAsString().equals(methodName) && n.getScope().isEmpty()) {
                    a.selfCalls++;
                }
                super.visit(n, depth);
            }
        }, 0);
        return a;
    }

    private boolean loopHasHalving(com.github.javaparser.ast.Node loop) {
        return loop.findAll(AssignExpr.class).stream().anyMatch(this::isHalvingAssign);
    }

    private boolean isHalvingAssign(AssignExpr a) {
        AssignExpr.Operator op = a.getOperator();
        Expression value = a.getValue();
        if (op == AssignExpr.Operator.DIVIDE) return isLiteral(value, 2);
        if (op == AssignExpr.Operator.SIGNED_RIGHT_SHIFT
                || op == AssignExpr.Operator.UNSIGNED_RIGHT_SHIFT) {
            return isLiteral(value, 1);
        }
        if (op == AssignExpr.Operator.ASSIGN && value instanceof BinaryExpr be) {
            BinaryExpr.Operator bop = be.getOperator();
            if (bop == BinaryExpr.Operator.DIVIDE && isLiteral(be.getRight(), 2)) return true;
            if (bop == BinaryExpr.Operator.SIGNED_RIGHT_SHIFT && isLiteral(be.getRight(), 1)) return true;
        }
        return false;
    }

    private boolean isLiteral(Expression e, int expected) {
        if (!(e instanceof IntegerLiteralExpr lit)) return false;
        try {
            return Integer.parseInt(lit.getValue()) == expected;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
