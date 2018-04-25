package roll.notebook;

import roll.automata.NBA;
import roll.learner.LearnerBase;
import roll.main.Options;
import roll.oracle.MembershipOracle;
import roll.query.Query;
import roll.query.QuerySimple;
import roll.table.HashableValue;
import roll.words.Alphabet;
import roll.words.Word;

/**
 * A wrapper for NBA learner
 * */

public class NBALearner implements JupyterLearner<NBA> {

    private LearnerBase<NBA> learner;
    private MembershipOracle<HashableValue> mqOracle;
    private Alphabet alphabet;
    public NBALearner(Alphabet alphabet
            , LearnerBase<NBA> learner
            , MembershipOracle<HashableValue> mqOracle) {
        assert learner != null;
        assert alphabet != null;
        assert mqOracle != null;
        this.alphabet = alphabet;
        this.learner = learner;
        this.mqOracle = mqOracle;
    }
    
    @Override
    public NBA getHypothesis() {
        return learner.getHypothesis();
    }
    
    @Override
    public String toString() {
        if(isTable()) {
            return learner.toString();
        }else {
            return learner.toSVG();
        }
    }
    
    public void refineHypothesis(String stem, String loop) {

        Word prefix = alphabet.getWordFromString(stem);
        Word suffix = alphabet.getWordFromString(loop);
        // now verify counterexample
        NBA hypothesis = (NBA) learner.getHypothesis();
        boolean isInHypo = hypothesis.getAcc().isAccepting(prefix, suffix);
        Query<HashableValue> ceQuery = new QuerySimple<>(prefix, suffix);
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
