package com.diploma.analysis_service.analyzer;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JavaMetricsCalculator {

    public record Metrics(int methodCount, int cyclomatic, int cognitive) {
        static final Metrics EMPTY = new Metrics(0, 0, 0);
    }

    public Metrics compute(String code) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(code);
            int methods = 0;
            int cyclomatic = 0;
            int cognitive = 0;
            for (MethodDeclaration md : cu.findAll(MethodDeclaration.class)) {
                methods++;
                cyclomatic += cyclomaticOf(md);
                cognitive += cognitiveOf(md);
            }
            return new Metrics(methods, cyclomatic, cognitive);
        } catch (Exception e) {
            log.debug("JavaParser failed to parse the source: {}", e.getMessage());
            return Metrics.EMPTY;
        }
    }

    private int cyclomaticOf(MethodDeclaration md) {
        int[] count = {1};
        md.accept(new VoidVisitorAdapter<Void>() {
            @Override public void visit(IfStmt n, Void a)        { count[0]++; super.visit(n, a); }
            @Override public void visit(ForStmt n, Void a)       { count[0]++; super.visit(n, a); }
            @Override public void visit(ForEachStmt n, Void a)   { count[0]++; super.visit(n, a); }
            @Override public void visit(WhileStmt n, Void a)     { count[0]++; super.visit(n, a); }
            @Override public void visit(DoStmt n, Void a)        { count[0]++; super.visit(n, a); }
            @Override public void visit(CatchClause n, Void a)   { count[0]++; super.visit(n, a); }
            @Override public void visit(ConditionalExpr n, Void a) { count[0]++; super.visit(n, a); }
            @Override public void visit(SwitchEntry n, Void a) {
                if (!n.getLabels().isEmpty()) count[0]++;
                super.visit(n, a);
            }
            @Override public void visit(BinaryExpr n, Void a) {
                BinaryExpr.Operator op = n.getOperator();
                if (op == BinaryExpr.Operator.AND || op == BinaryExpr.Operator.OR) count[0]++;
                super.visit(n, a);
            }
        }, null);
        return count[0];
    }

    private int cognitiveOf(MethodDeclaration md) {
        int[] sum = {0};
        md.accept(new VoidVisitorAdapter<Integer>() {
            @Override public void visit(IfStmt n, Integer depth)        { sum[0] += 1 + depth; super.visit(n, depth + 1); }
            @Override public void visit(ForStmt n, Integer depth)       { sum[0] += 1 + depth; super.visit(n, depth + 1); }
            @Override public void visit(ForEachStmt n, Integer depth)   { sum[0] += 1 + depth; super.visit(n, depth + 1); }
            @Override public void visit(WhileStmt n, Integer depth)     { sum[0] += 1 + depth; super.visit(n, depth + 1); }
            @Override public void visit(DoStmt n, Integer depth)        { sum[0] += 1 + depth; super.visit(n, depth + 1); }
            @Override public void visit(CatchClause n, Integer depth)   { sum[0] += 1 + depth; super.visit(n, depth + 1); }
            @Override public void visit(ConditionalExpr n, Integer depth) { sum[0] += 1 + depth; super.visit(n, depth); }
        }, 0);
        return sum[0];
    }
}
