package solver;

import cache.QueryTrace;
import cache.QueryTraceEntry;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.StringSymbol;
import com.microsoft.z3.Solver;

import java.util.*;
import java.util.stream.Collectors;

public abstract class DeterminacyFormula {
    protected Context context;
    protected Schema schema;
    protected Instance inst1;
    protected Instance inst2;
    protected BoolExpr preparedExpr;
    protected String preparedExprSMT;

    protected DeterminacyFormula(Context context, Schema schema, Collection<Query> views) {
        this.context = context;
        this.schema = schema;
        this.inst1 = schema.makeFreshInstance(context);
        this.inst2 = schema.makeFreshInstance(context);
        this.preparedExpr = null;
    }

    protected void setPreparedExpr(BoolExpr expr) {
        this.preparedExpr = expr;
        Solver solver = this.context.mkSolver();
        solver.add(this.preparedExpr);
        this.preparedExprSMT = solver.toString();
    }

    protected BoolExpr generateTupleCheck(QueryTrace queries, Expr[] constants) {
        List<BoolExpr> exprs = new ArrayList<>();
        for (List<QueryTraceEntry> queryTraceEntries : queries.getQueries().values()) {
            for (QueryTraceEntry queryTraceEntry : queryTraceEntries) {
                Query query = queryTraceEntry.getQuery().getSolverQuery(schema);
                Relation r1 = query.apply(context, inst1);
                Relation r2 = query.apply(context, inst2);
                if (!queryTraceEntry.getTuples().isEmpty()) {
                    List<Tuple> tuples = queryTraceEntry.getTuples().stream().map(tuple -> new Tuple(tuple.stream().map(v -> Tuple.getExprFromObject(context, v)).toArray(Expr[]::new))).collect(Collectors.toList());
                    exprs.add(r1.doesContain(context, tuples));
                    exprs.add(r2.doesContain(context, tuples));
                }
            }
        }

        // Constrain constant values.
        for (Map.Entry<String, Integer> entry : queries.getConstMap().entrySet()) {
            StringSymbol nameSymbol = context.mkSymbol("!" + entry.getKey());
            exprs.add(context.mkEq(
                    context.mkConst(nameSymbol, context.getIntSort()),
                    context.mkInt(entry.getValue())
            ));
        }

        if (exprs.isEmpty()) {
            return context.mkTrue();
        }
        return context.mkAnd(exprs.toArray(new BoolExpr[0]));
    }

    protected abstract Expr[] makeFormulaConstants(QueryTrace queries);
    protected abstract BoolExpr makeFormula(QueryTrace queries, Expr[] constants);

    protected String makeFormulaSMT(QueryTrace queries, Expr[] constants) {
        return "(assert " + makeFormula(queries, constants).toString() + ")";
    }

    public Solver makeSolver(QueryTrace queries) {
        Solver solver = context.mkSolver();
        solver.add(preparedExpr);
        solver.add(makeFormula(queries, makeFormulaConstants(queries)));
        return solver;
    }

    public synchronized String generateSMT(QueryTrace queries) {
//        System.out.println("\t| Make SMT:");
        MyZ3Context myContext = (MyZ3Context) context;
        myContext.startTrackingConsts();
        String smt = makeFormulaSMT(queries, makeFormulaConstants(queries));
        myContext.stopTrackingConsts();

        StringBuilder stringBuilder = new StringBuilder();
        for (Expr constant : myContext.getConsts()) {
            stringBuilder.append("(declare-fun ").append(constant.getSExpr()).append(" () ").append(constant.getSort().getSExpr()).append(")\n");
        }
        stringBuilder.append(this.preparedExprSMT);
        stringBuilder.append(smt);
        return stringBuilder.toString();
    }
}
