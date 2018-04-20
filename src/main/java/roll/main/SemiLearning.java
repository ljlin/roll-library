package roll.main;

import roll.automata.FASimple;
import roll.automata.NBA;
import roll.learner.Learner;
import roll.learner.LearnerBase;
import roll.learner.dfa.table.LearnerDFATableColumn;
import roll.learner.dfa.table.LearnerDFATableLStar;
import roll.learner.dfa.tree.LearnerDFATreeColumn;
import roll.learner.dfa.tree.LearnerDFATreeKV;
import roll.learner.nba.ldollar.LearnerNBALDollar;
import roll.learner.nba.lomega.LearnerNBALOmega;
import roll.oracle.MembershipOracle;
import roll.oracle.semi.SemiTeacher;
import roll.query.Query;
import roll.table.HashableValue;
import roll.words.Alphabet;

import java.util.function.BiFunction;
import java.util.function.Function;

public class SemiLearning <M extends FASimple> {
    public LearnerBase<M> learner;
    public SemiTeacher teacher;
    public SemiLearning(Alphabet alphabet,Options options, BiFunction<String,String,Boolean> f) {
        teacher = new SemiTeacher(f);
        learner = SemiLearning.createLearner(options,alphabet,teacher);
//        learner = new LearnerNBALOmega(options, alphabet, teacher);
        learner.startLearning();
    }
    public SemiLearning(Alphabet alphabet,Options options, Function<String,Boolean> f) {
        teacher = new SemiTeacher(f);
        learner = SemiLearning.createLearner(options,alphabet,teacher);
//        learner = new LearnerNBALOmega(options, alphabet, teacher);
        learner.startLearning();
    }

    public M getHypothesis(){
        return learner.getHypothesis();
    }

    public M refineHypothesis(Query<HashableValue> query) throws Exception {
        boolean targetAccepting = this.teacher.answerMembershipQuery(query).get();
        boolean hypothAccepting = this.learner.getHypothesis().getAcc().isAccepting(query.getPrefix(),query.getSuffix());
        if (targetAccepting == hypothAccepting ) {
            if ( hypothAccepting == false){
                System.err.println("Counter Example Error : " + query.toString() + " is neither in target or hypothesis.");
            }
            else {
                System.err.println("Counter Example Error : " + query.toString() + " is both in target and hypothesis.");
            }
            throw new Exception("Counter Example Error");
        }

        learner.refineHypothesis(query);

        return learner.getHypothesis();
    }

    public static LearnerBase createLearner(
            Options options,
            Alphabet alphabet,
            MembershipOracle<HashableValue> teacher) {
        LearnerBase learner;
        if (options.algorithm == Options.Algorithm.NBA_LDOLLAR) {
            learner = new LearnerNBALDollar(options, alphabet, teacher);
        } else if (options.algorithm == Options.Algorithm.PERIODIC
                || options.algorithm == Options.Algorithm.SYNTACTIC
                || options.algorithm == Options.Algorithm.RECURRENT) {
            learner = new LearnerNBALOmega(options, alphabet, teacher);
        } else if(options.algorithm == Options.Algorithm.DFA_COLUMN) {
            if(options.structure == Options.Structure.TABLE) {
                learner = new LearnerDFATableColumn(options, alphabet, teacher);
            }else {
                learner = new LearnerDFATreeColumn(options, alphabet, teacher);
            }
        } else if (options.algorithm == Options.Algorithm.DFA_LSTAR) {
            learner = new LearnerDFATableLStar(options, alphabet, teacher);
        } else if (options.algorithm == Options.Algorithm.DFA_KV) {
            learner = new LearnerDFATreeKV(options, alphabet, teacher);
        } else {
            throw new UnsupportedOperationException("Unsupported BA Learner");
        }

        return learner;
    }
}
