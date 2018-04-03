package roll.main;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import roll.automata.NBA;
import roll.learner.Learner;
import roll.learner.LearnerBase;
import roll.learner.nba.lomega.LearnerNBALOmega;
import roll.oracle.nba.rabit.TeacherNBARABIT;
import roll.parser.Format;
import roll.parser.Parser;
import roll.parser.UtilParser;
import roll.query.Query;
import roll.table.HashableValue;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class LearningSequence {

    static public Iterator<Triple<Integer, LearnerBase<NBA>, Optional<Query<HashableValue>>>> creat(NBA target, Options options) {
        return new Iterator<Triple<Integer, LearnerBase<NBA>, Optional<Query<HashableValue>>>>() {
            TeacherNBARABIT teacher = new TeacherNBARABIT(options, target);
            LearnerNBALOmega learner;
            boolean isEq = false;
            NBA hypothesis = null;
            Query<HashableValue> ceQuery = null;
            int cnt = 0;

            @Override
            public boolean hasNext() {
                return !isEq;
            }

            @Override
            public Triple<Integer, LearnerBase<NBA>, Optional<Query<HashableValue>>> next() {
                if (ceQuery != null) {
                    learner.refineHypothesis(ceQuery);
                } else {
                    learner = new LearnerNBALOmega(options, target.getAlphabet(), teacher);
                    learner.startLearning();
                }
                Triple<Integer, LearnerBase<NBA>, Optional<Query<HashableValue>>> res
                        = ImmutableTriple.of(
                        cnt,
                        learner,
                        Optional.ofNullable(ceQuery).map(q -> q.clone())
                );
                // learner.copy ceQuery.copy

                // calc new counter example
                hypothesis = learner.getHypothesis();
                ceQuery = teacher.answerEquivalenceQuery(hypothesis);
                isEq = ceQuery.getQueryAnswer().get();
                // if isEq then stop
                ceQuery.answerQuery(null);
                cnt++;
                return res;
            }
        };
    }

    static public Iterator<Query<HashableValue>> ceSequence(NBA target, Options options) {
        return new Iterator<Query<HashableValue>>() {
            Iterator<Triple<Integer, LearnerBase<NBA>, Optional<Query<HashableValue>>>> delegate = creat(target, options);

            {
                if (delegate.hasNext()) {
                    delegate.next();
                    // to skip the first empty ce
                }

            }

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public Query<HashableValue> next() {
                return delegate.next().getRight().get();
            }
        };
    }

    static public ImmutableList<Query<HashableValue>> ceList(NBA target, Options options) {
        return ImmutableList.copyOf(ceSequence(target, options));
    }

    static public void refineLearner(LearnerNBALOmega learner, List<Query<HashableValue>> ceList){
        for (Query<HashableValue> ce : ceList) {
            learner.refineHypothesis(ce);
        }
    }

    static public LearnerNBALOmega refinedLearner(NBA target,Options options,List<Query<HashableValue>> ceList){
        TeacherNBARABIT teacher = new TeacherNBARABIT(options, target);
        LearnerNBALOmega learner = new LearnerNBALOmega(options, target.getAlphabet(), teacher);
        learner.startLearning();
        LearningSequence.refineLearner(learner,ceList);
        return learner;
    }

}
