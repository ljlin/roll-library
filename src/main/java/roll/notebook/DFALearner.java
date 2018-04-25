package roll.notebook;

import roll.automata.DFA;
import roll.learner.LearnerBase;
import roll.main.Options;
import roll.oracle.MembershipOracle;
import roll.query.Query;
import roll.query.QuerySimple;
import roll.table.HashableValue;
import roll.words.Alphabet;
import roll.words.Word;

/**
 * A wrapper for DFA learner
 * */

public class DFALearner implements JupyterLearner<DFA> {

    private LearnerBase<DFA> learner;
    private MembershipOracle<HashableValue> mqOracle;
    private Alphabet alphabet;
    public DFALearner(Alphabet alphabet
            , LearnerBase<DFA> learner
            , MembershipOracle<HashableValue> mqOracle) {
        assert learner != null;
        assert alphabet != null;
        assert mqOracle != null;
        this.alphabet = alphabet;
        this.learner = learner;
        this.mqOracle = mqOracle;
    }
    
    @Override
    public DFA getHypothesis() {
        return learner.getHypothesis();
    }
    
    @Override
    public String toString() {
        return learner.toString();
    }
    
    public void refineHypothesis(String counterexample) {
        Word word = alphabet.getWordFromString(counterexample);
        // now verify counterexample
        DFA hypothesis = (DFA) learner.getHypothesis();
        boolean isInHypo = hypothesis.getAcc().isAccepting(word, alphabet.getEmptyWord());
        Query<HashableValue> ceQuery = new QuerySimple<>(word);
        HashableValue isInTarget = mqOracle.answerMembershipQuery(ceQuery);
        if(isInHypo && isInTarget.isAccepting()) {
            System.err.println("Invalid counterexample, both in hypothesis and target");
            return ;
        }
        
        if(!isInHypo && !isInTarget.isAccepting()) {
            System.err.println("Invalid counterexample, neither in hypothesis or target");
            return ;
        }
        ceQuery.answerQuery(null);
        learner.refineHypothesis(ceQuery);
    }

    @Override
    public boolean isTable() {
        return learner.getOptions().structure == Options.Structure.TABLE;
    }

}
