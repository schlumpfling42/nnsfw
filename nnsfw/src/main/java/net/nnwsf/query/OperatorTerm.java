package net.nnwsf.query;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OperatorTerm extends SearchTerm {
    private final String operator; 
    private final List<SearchTerm> values = new ArrayList<>();
    public OperatorTerm(SearchTerm value, String operator) {
        this.operator = operator;
        this.values.add(value);
    }
    @Override
    public String toString() {
        return "{" + values.stream().map(SearchTerm::toString).collect(Collectors.joining(" " + operator + " ")) + "}";
    }

    public String getOperator() {
        return operator;
    }

    public void addValue(SearchTerm value) {
        values.add(value);
    }

    public List<SearchTerm> getValues() {
        return values;
    }
}