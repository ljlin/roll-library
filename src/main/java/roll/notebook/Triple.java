package roll.notebook;

import roll.query.Query;
import roll.table.HashableValue;

public class Triple {
    String learner;              // the data structure of the learner
    String hypothesis;           // the automaton of the learner
    Query<HashableValue> query;  // the counterexample used in last time
    
    public Triple(String learner, String hypothesis, Query<HashableValue> query) {
        this.learner = learner;
        this.hypothesis = hypothesis;
        this.query = query;
    }
    
    public String getLeft() {
        return learner;
    }
    
    public String getMiddle() {
        return hypothesis;
    }
    
    public Query<HashableValue> getRight() {
        return query;
    }
}