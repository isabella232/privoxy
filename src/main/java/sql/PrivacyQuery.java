package sql;

import solver.Query;
import solver.Schema;

import java.util.*;

public abstract class PrivacyQuery {
    public ParserResult parsedSql;
    public Object[] parameters;
    public List<String> paramNames;

    public PrivacyQuery(ParserResult parsedSql)
    {
        this(parsedSql, new Object[0], Collections.emptyList());
    }

    public PrivacyQuery(ParserResult parsedSql, Object[] parameters, List<String> paramNames)
    {
        this.parsedSql = parsedSql;
        this.parameters = new Object[parameters.length];
        System.arraycopy(parameters, 0, this.parameters, 0, parameters.length);
        this.paramNames = new ArrayList<>(paramNames);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrivacyQuery query = (PrivacyQuery) o;
        if (!parsedSql.equals(query.parsedSql)) return false;
        if (parameters.length != query.parameters.length) return false;
        for (int i = 0; i < parameters.length; ++i) {
            if (paramNames.get(i).equals("?") && !parameters[i].equals(query.parameters[i])) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(parsedSql);
        for (int i = 0; i < parameters.length; ++i) {
            if (paramNames.get(i).equals("?")) {
                result = 31 * result + Objects.hash(parameters[i]);
            }
        }
        return result;
    }

    abstract public Set<String> getProjectColumns();
    abstract public Set<String> getThetaColumns();
    abstract public Query getSolverQuery(Schema schema);
    abstract public List<Boolean> getResultBitmap();
}

