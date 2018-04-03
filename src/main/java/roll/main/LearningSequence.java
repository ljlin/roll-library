package roll.main;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import roll.automata.DFA;
import roll.automata.NBA;
import roll.learner.Learner;
import roll.learner.LearnerBase;
import roll.learner.LearnerDFA;
import roll.learner.LearnerType;
import roll.learner.dfa.table.LearnerDFATableColumn;
import roll.learner.dfa.table.LearnerDFATableLStar;
import roll.learner.dfa.tree.LearnerDFATreeColumn;
import roll.learner.dfa.tree.LearnerDFATreeKV;
import roll.learner.nba.lomega.LearnerNBALOmega;
import roll.oracle.dfa.dk.TeacherDFADK;
import roll.oracle.nba.rabit.TeacherNBARABIT;
import roll.query.Query;
import roll.table.HashableValue;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class LearningSequence {

    static public Iterator<Triple<Integer, LearnerBase<DFA>, Optional<Query<HashableValue>>>> creat(DFA target, Options options) {
        return new Iterator<Triple<Integer, LearnerBase<DFA>, Optional<Query<HashableValue>>>>() {
            TeacherDFADK teacher = new TeacherDFADK(options, target);
            LearnerDFA learner;
            boolean isEq = false;
            Query<HashableValue> ceQuery = null;
            DFA hypothesis = null;

            int cnt = 0;

            private void createLearner(LearnerType algo){
                if(algo == LearnerType.DFA_COLUMN_TABLE) learner = new LearnerDFATableColumn(options, target.getAlphabet(), teacher);
                else if(algo == LearnerType.DFA_KV) {
                    learner = new LearnerDFATreeKV(options, target.getAlphabet(), teacher);
                }else if(algo == LearnerType.DFA_COLUMN_TREE){
                    learner = new LearnerDFATreeColumn(options, target.getAlphabet(), teacher);
                }else {
                    learner = new LearnerDFATableLStar(options, target.getAlphabet(), teacher);
                }
//                System.out.println("starting learning");
                learner.startLearning();
            }

            @Override
            public boolean hasNext() {
                return !isEq;
            }

            @Override
            public Triple<Integer, LearnerBase<DFA>, Optional<Query<HashableValue>>> next() {
                LearnerType algo = null;

                switch (options.algorithm) {
                    case DFA_LSTAR:{
                        algo = LearnerType.DFA_LSTAR;
                        break;
                    }
                    case DFA_COLUMN:{
                        algo = options.structure == Options.Structure.TREE ?
                                LearnerType.DFA_COLUMN_TREE :
                                LearnerType.DFA_COLUMN_TABLE;
                        break;
                    }
                    case DFA_KV:{
                        algo = LearnerType.DFA_KV;
                        break;
                    }
                }
//                LearnerType algo = LearnerType.DFA_COLUMN_TABLE;
//                if(args[0].equals("lstar")) algo = LearnerType.DFA_LSTAR;
//                if(args[0].equals("kv")) algo =  LearnerType.DFA_KV;
//                if(args[0].equals("tr")) algo = LearnerType.DFA_COLUMN_TREE;
                if (ceQuery != null) {
                    learner.refineHypothesis(ceQuery);
                } else {
                    createLearner(algo);
                }
                Triple<Integer, LearnerBase<DFA>, Optional<Query<HashableValue>>> res
                        = ImmutableTriple.of(
                        cnt,
                        learner,
                        Optional.ofNullable(ceQuery).map(q -> q.clone())
                );
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
    static public Iterator<Query<HashableValue>> ceSequence(DFA target, Options options) {
        return new Iterator<Query<HashableValue>>() {
            Iterator<Triple<Integer, LearnerBase<DFA>, Optional<Query<HashableValue>>>> delegate = creat(target, options);

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

    static public ImmutableList<Query<HashableValue>> ceList(DFA target, Options options) {
        return ImmutableList.copyOf(ceSequence(target, options));
    }

    static public void refineLearner(LearnerBase<DFA> learner, List<Query<HashableValue>> ceList){
        for (Query<HashableValue> ce : ceList) {
            learner.refineHypothesis(ce);
        }
    }

    static public LearnerBase<DFA> refinedLearner(DFA target,Options options,List<Query<HashableValue>> ceList){
        TeacherDFADK teacher = new TeacherDFADK(options, target);
        LearnerType algo = null;

        switch (options.algorithm) {
            case DFA_LSTAR:{
                algo = LearnerType.DFA_LSTAR;
                break;
            }
            case DFA_COLUMN:{
                algo = options.structure == Options.Structure.TREE ?
                        LearnerType.DFA_COLUMN_TREE :
                        LearnerType.DFA_COLUMN_TABLE;
                break;
            }
            case DFA_KV:{
                algo = LearnerType.DFA_KV;
                break;
            }
        }
        LearnerBase<DFA> learner = null;
        if(algo == LearnerType.DFA_COLUMN_TABLE) learner = new LearnerDFATableColumn(options, target.getAlphabet(), teacher);
        else if(algo == LearnerType.DFA_KV) {
            learner = new LearnerDFATreeKV(options, target.getAlphabet(), teacher);
        }else if(algo == LearnerType.DFA_COLUMN_TREE){
            learner = new LearnerDFATreeColumn(options, target.getAlphabet(), teacher);
        }else {
            learner = new LearnerDFATableLStar(options, target.getAlphabet(), teacher);
        }
//                System.out.println("starting learning");
        learner.startLearning();
        LearningSequence.refineLearner(learner,ceList);
        return learner;
    }
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
