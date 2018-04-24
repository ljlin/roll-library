/* Copyright (c) 2016, 2017                                               */
/*       Institute of Software, Chinese Academy of Sciences               */
/* This file is part of ROLL, a Regular Omega Language Learning library.  */
/* ROLL is free software: you can redistribute it and/or modify           */
/* it under the terms of the GNU General Public License as published by   */
/* the Free Software Foundation, either version 3 of the License, or      */
/* (at your option) any later version.                                    */

/* This program is distributed in the hope that it will be useful,        */
/* but WITHOUT ANY WARRANTY; without even the implied warranty of         */
/* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          */
/* GNU General Public License for more details.                           */

/* You should have received a copy of the GNU General Public License      */
/* along with this program.  If not, see <http://www.gnu.org/licenses/>.  */

package roll.notebook;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import jupyter.Displayer;
import jupyter.Displayers;
import jupyter.MIMETypes;
import roll.automata.DFA;
import roll.automata.FASimple;
import roll.automata.NBA;
import roll.learner.LearnerBase;
import roll.learner.dfa.table.LearnerDFATableColumn;
import roll.learner.dfa.table.LearnerDFATableLStar;
import roll.learner.dfa.tree.LearnerDFATreeColumn;
import roll.learner.dfa.tree.LearnerDFATreeKV;
import roll.learner.nba.ldollar.LearnerNBALDollar;
import roll.learner.nba.lomega.LearnerNBALOmega;
import roll.main.Options;
import roll.oracle.MembershipOracle;
import roll.oracle.TeacherAbstract;
import roll.oracle.dfa.TeacherDFA;
import roll.oracle.dfa.dk.TeacherDFADK;
import roll.oracle.nba.TeacherNBA;
import roll.oracle.nba.rabit.TeacherNBARABIT;
import roll.query.Query;
import roll.query.QuerySimple;
import roll.table.HashableValue;
import roll.table.HashableValueBoolean;
import roll.words.Alphabet;
import roll.words.Word;

/**
 * @author Jianlin Li, Yong Li (liyong@ios.ac.cn)
 * */

public class ROLLNotebook {
    
    static {
        register();
    }
    
    private static String dotToSVG(String dot) {
        return NativeTool.dot2SVG(dot);
    }
    
    private static void register(){
        // for FASimple
        Displayers.register(FASimple.class, new Displayer<FASimple>() {
            @Override
            public Map<String, String> display(FASimple automaton) {
                return new HashMap<String, String>() {
                    private static final long serialVersionUID = 1L;

                    {
                        put(MIMETypes.HTML, dotToSVG(automaton.toDot()));
                    }
                };
            }
        });
        // for Learners
        Displayers.register(LearnerBase.class, new Displayer<LearnerBase>() {
            @Override
            public Map<String, String> display(LearnerBase learner) {
                return new HashMap<String, String>() {
                    private static final long serialVersionUID = 1L;

                    {
                        if (learner.getOptions().structure == Options.Structure.TREE)
                            put(MIMETypes.HTML, dotToSVG(learner.toString()));
                        else
                            put(MIMETypes.TEXT, learner.toString());
                    }
                };
            }
        });
        
        Displayers.register(Triple.class, new Displayer<Triple>() {
            @Override
            public Map<String, String> display(Triple triple) {
                return new HashMap<String, String> () {/**
                     * 
                     */
                    private static final long serialVersionUID = 1L;
                {
                    String HTML =
                            "<table border=\"1\" cellspacing=\"0\" bordercolor=\"#000000\"  style=\"border-collapse:collapse;\">\n" +
                            "  <tr>\n" +
                            "    <th><center> Learner</center></th>\n" +
                            "    <th><center>  Hypothesis </center></th>\n" +
                            "    <th><center> Counterexample </center></th>\n" +
                            "  </tr>\n" +
                            "  <tr>\n"  +
                            "    <td>%s</td>\n" +
                            "    <td><center>%s</center></td>\n" +
                            "    <td><center>%s</center></td>\n" +
                            "  </tr>\n" +
                            "</table>";
                    String learner = triple.getLeft();
                    String hypothesis = triple.getMiddle();
                    QuerySimple<HashableValue> query = (QuerySimple<HashableValue>) triple.getRight();
                    put(MIMETypes.HTML,String.format(HTML, learner, hypothesis, query == null ? "" : "$" + query.toLaTex() + "$"));
                }};
            }
        }); 
    }
    
    public static Alphabet alphabet;
    
    public static void createAlphabet(List<Character> array) {
        alphabet = new Alphabet();
        for(Character letter : array) {
            alphabet.addLetter(letter);
        }
    }
    
    private static void verifyAlphabet() {
        if(alphabet == null) throw new UnsupportedOperationException("Alphabet is empty, use createAlphabet function");
    }
    
    public static NBA createNBA() {
        verifyAlphabet();
        return new NBA(alphabet);
    }
    
    public static DFA createDFA() {
        verifyAlphabet();
        return new DFA(alphabet);
    }
    
    // ==============================================================================================================
    
    public static List<Triple> learningSeq(
            String algo, String structure, FASimple target) {
        // first choose learning algorithm and teacher
        Options options = parseOptions(algo, structure);
        TeacherAbstract<? extends FASimple> teacher = getTeacher(options, target);
        LearnerBase<? extends FASimple> learner = getLearner(options, target.getAlphabet(), teacher);
        ArrayList<Triple> sequence = new ArrayList<>();
        // learning loop
        learner.startLearning();
        Query<HashableValue> ceQuery = null;
        while(true) {
            // along with ce
            FASimple hypothesis = learner.getHypothesis();
            Triple triple = null;
            String learnerStr = options.structure == Options.Structure.TREE ?
                    learner.toSVG()
                    : "<pre>" + learner.toString() + "</pre>"; //.replaceAll("\n", "<br>").replaceAll(" ", "&nbsp;") + "</text>"; //.replaceAll(" ", "&nbsp;").replaceAll("" + (char)(9), "&#9;");
            triple = new Triple(learnerStr,
                    dotToSVG(hypothesis.toDot()), ceQuery);
            if(hypothesis instanceof NBA) {
                TeacherNBA teacherNBA = (TeacherNBA)teacher;
                ceQuery = teacherNBA.answerEquivalenceQuery((NBA)hypothesis);
            }else {
                TeacherDFA teacherDFA = (TeacherDFA)teacher;
                ceQuery = teacherDFA.answerEquivalenceQuery((DFA)hypothesis);
            }
            sequence.add(triple);
            boolean isEq = ceQuery.getQueryAnswer().get();
            if(isEq) {
                break;
            }
            ceQuery.answerQuery(null);
            learner.refineHypothesis(ceQuery);
        }
        return sequence;
    }
    
    private static Options parseOptions(String algo, String structure) {
        Options options = new Options();
        switch(algo) {
        case "periodic":
            options.algorithm = Options.Algorithm.PERIODIC;
            options.automaton = Options.TargetAutomaton.NBA;
            break;
        case "syntactic":
            options.algorithm = Options.Algorithm.SYNTACTIC;
            options.automaton = Options.TargetAutomaton.NBA;
            break;
        case "recurrent":
            options.algorithm = Options.Algorithm.RECURRENT;
            options.automaton = Options.TargetAutomaton.NBA;
            break;
        case "ldollar":
            options.algorithm = Options.Algorithm.NBA_LDOLLAR;
            options.automaton = Options.TargetAutomaton.NBA;
            break;
        case "lstar":
            options.algorithm = Options.Algorithm.DFA_LSTAR;
            options.automaton = Options.TargetAutomaton.DFA;
            break;
        case "kv":
            options.algorithm = Options.Algorithm.DFA_KV;
            options.automaton = Options.TargetAutomaton.DFA;
            break;
        case "column":
            options.algorithm = Options.Algorithm.DFA_COLUMN;
            options.automaton = Options.TargetAutomaton.DFA;
            break;
        default:
                throw new UnsupportedOperationException("Unknown learning algorithm");
        }
        switch(structure) {
        case "table":
            options.structure = Options.Structure.TABLE;
            break;
        case "tree":
            options.structure = Options.Structure.TREE;
            break;
        default:
            throw new UnsupportedOperationException("Unknown data structure");
        }
        
        return options;
    }
    
    private static TeacherAbstract<? extends FASimple> getTeacher(Options options, FASimple target) {
        if((target instanceof NBA) && (options.algorithm == Options.Algorithm.NBA_LDOLLAR
                || options.algorithm == Options.Algorithm.PERIODIC
                || options.algorithm == Options.Algorithm.SYNTACTIC
                || options.algorithm == Options.Algorithm.RECURRENT)
            ) {
               return new TeacherNBARABIT(options, (NBA)target);
           }else if((target instanceof DFA) && (options.algorithm == Options.Algorithm.DFA_COLUMN
                || options.algorithm == Options.Algorithm.DFA_LSTAR
                || options.algorithm == Options.Algorithm.DFA_KV)) {
               return new TeacherDFADK(options, (DFA)target);
           }else {
               throw new UnsupportedOperationException("Unsupported Learning Target");
           }
    }
    
    private static LearnerBase<? extends FASimple> getLearner(Options options, Alphabet alphabet,
            MembershipOracle<HashableValue> teacher) {
        LearnerBase<? extends FASimple> learner = null;
        if(options.algorithm == Options.Algorithm.NBA_LDOLLAR) {
            learner = (LearnerBase<? extends FASimple>)new LearnerNBALDollar(options, alphabet, teacher);
        }else if(options.algorithm == Options.Algorithm.PERIODIC
             || options.algorithm == Options.Algorithm.SYNTACTIC
             || options.algorithm == Options.Algorithm.RECURRENT) {
            learner = new LearnerNBALOmega(options, alphabet, teacher);
        }else if(options.algorithm == Options.Algorithm.DFA_COLUMN) {
            if(options.structure == Options.Structure.TABLE) {
                learner = new LearnerDFATableColumn(options, alphabet, teacher);
            }else {
                learner = new LearnerDFATreeColumn(options, alphabet, teacher);
            }
        }else if(options.algorithm == Options.Algorithm.DFA_LSTAR) {
            learner = new LearnerDFATableLStar(options, alphabet, teacher);
        }else if(options.algorithm == Options.Algorithm.DFA_KV) {
            learner = new LearnerDFATreeKV(options, alphabet, teacher);
        }else {
            throw new UnsupportedOperationException("Unsupported Learner");
        }
        
        return learner;
    }
    // ==============================================================================================================
    // for interactive learning
    private static LearnerBase<? extends FASimple> learner;
    private static MQOracle mqOracle;
    
    // create NBA learner
    public static void createNBALearner(String algo, String structure, BiFunction<String, String, Boolean> mqFunc) {
        Options options = parseOptions(algo, structure);
        if(options.automaton != Options.TargetAutomaton.NBA) {
            throw new UnsupportedOperationException("Unsupported BA learner");
        }
        verifyAlphabet();
        mqOracle = new MQOracle(mqFunc);
        learner = getLearner(options, alphabet, mqOracle);
        learner.startLearning();
    }
    
    private static void verifyLearner() {
        if(learner == null) throw new UnsupportedOperationException("Please first create a learner via createNBALeaner or createDFALeaner");
    }
    
    public static FASimple getHypothesis() {
        verifyLearner();
        return learner.getHypothesis();
    }
    
    // create DFA learner
    public static void createDFALearner(String algo, String structure, Function<String, Boolean> mqFunc) {
        Options options = parseOptions(algo, structure);
        if(options.automaton != Options.TargetAutomaton.DFA) {
            throw new UnsupportedOperationException("Unsupported DFA learner");
        }
        verifyAlphabet();
        mqOracle = new MQOracle(mqFunc);
        learner = getLearner(options, alphabet,  mqOracle);
        learner.startLearning();
    }
    
    public static void refineDFAHypothesis(String counterexample) {
        verifyAlphabet();
        Word word = alphabet.getWordFromString(counterexample);
        verifyLearner();
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
    
    public static void refineNBAHypothesis(String stem, String loop) {
        verifyAlphabet();
        Word prefix = alphabet.getWordFromString(stem);
        Word suffix = alphabet.getWordFromString(loop);
        verifyLearner();
        // now verify counterexample
        NBA hypothesis = (NBA) learner.getHypothesis();
        boolean isInHypo = hypothesis.getAcc().isAccepting(prefix, suffix);
        Query<HashableValue> ceQuery = new QuerySimple<>(prefix, suffix);
        HashableValue isInTarget = mqOracle.answerMembershipQuery(ceQuery);
//        if(isInHypo && isInTarget.isAccepting()) {
//            throw new UnsupportedOperationException("Invalid counterexample, both in hypothesis and target");
//        }
//        
//        if(!isInHypo && !isInTarget.isAccepting()) {
//            throw new UnsupportedOperationException("Invalid counterexample, neither in hypothesis or target");
//        }
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
    
    private static class MQOracle implements MembershipOracle<HashableValue> {
        private Function<Query<HashableValue>,Boolean>  delegate;

        MQOracle(BiFunction<String, String, Boolean> f) {
            this.delegate = (query -> f.apply(
                    query.getPrefix().toStringWithAlphabet(),
                    query.getSuffix().toStringWithAlphabet()
                )
            );
        }

        MQOracle(Function<String, Boolean> f) {
            this.delegate = (query -> f.apply(
                    query.getQueriedWord().toStringWithAlphabet()
                )
            );
        }


        @Override
        public HashableValue answerMembershipQuery(Query<HashableValue> query) {
            return new HashableValueBoolean(this.delegate.apply(query));
        }


    }

   
}
