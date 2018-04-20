package test;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.Test;
import roll.automata.DFA;
import roll.automata.NBA;
import roll.learner.LearnerBase;
import roll.learner.nba.lomega.LearnerNBALOmega;
import roll.main.LearningSequence;
import roll.main.Options;
import roll.main.SemiLearning;
import roll.oracle.nba.rabit.TeacherNBARABIT;
import roll.parser.Format;
import roll.parser.Parser;
import roll.parser.UtilParser;
import roll.query.CE;
import roll.query.Query;
import roll.query.QuerySimple;
import roll.table.HashableValue;
import roll.words.Alphabet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public class notebookTest {

    static public NBA nbaExample(){
        // first create an alphabet
        Alphabet alphabet = new Alphabet();
        alphabet.addLetter('a');
        alphabet.addLetter('b');
// create an NBA with alphabet
        NBA target = new NBA(alphabet);
        target.createState();
        target.createState();
        target.createState();

// add transitions for NBA recognizing a^w + ab^w
        int fst = 0, snd = 1, thd = 2;
        target.getState(fst).addTransition(alphabet.indexOf('a'), snd);
        target.getState(fst).addTransition(alphabet.indexOf('a'), thd);
        target.getState(snd).addTransition(alphabet.indexOf('a'), snd);
        target.getState(thd).addTransition(alphabet.indexOf('b'), thd);
        target.setInitial(fst);
        target.setFinal(snd);
        target.setFinal(thd);
        return target;
    }
    static public DFA dfaExample(){

        Alphabet alphabet = new Alphabet();
        Character a = 'a';
        Character b = 'b';

        alphabet.addLetter(a);
        alphabet.addLetter(b);
// create an NBA with alphabet
        DFA target = new DFA(alphabet);
        target.createState();
        target.createState();
        target.createState();
        target.createState();
        int fst = 0, snd = 1, thd = 2, fth = 3;
        target.getState(fst).addTransition(alphabet.indexOf(a), fst);
        target.getState(fst).addTransition(alphabet.indexOf(b), snd);
        target.getState(snd).addTransition(alphabet.indexOf(a), snd);
        target.getState(snd).addTransition(alphabet.indexOf(b), thd);
        target.getState(thd).addTransition(alphabet.indexOf(a), thd);
        target.getState(thd).addTransition(alphabet.indexOf(b), fth);
        target.getState(fth).addTransition(alphabet.indexOf(a), fth);
        target.getState(fth).addTransition(alphabet.indexOf(b), fst);
        target.setInitial(fst);
        target.setFinal(fth);
// List<String> list = ["a", "b"];
// target.toString(list);
        return target;
    }
    @Test
    public void mainTest(){
        Options options = new Options();
        options.inputFile = "/Users/lijianlin/Projects/roll-library-github/nba.hoa";
        options.format = Format.HOA;
// use the under-approximation method to construct a BA from an FDFA
        options.approximation = Options.Approximation.UNDER;
// set NBA learner, here we use tree-based syntactic FDFA learner
        options.algorithm = Options.Algorithm.SYNTACTIC;
        options.structure = Options.Structure.TREE;


        Parser parser = UtilParser.prepare(options, options.inputFile , options.format);
        NBA nba = parser.parse();
        TeacherNBARABIT teacher = new TeacherNBARABIT(options, nba);
        LearnerNBALOmega learner = new LearnerNBALOmega(options, nba.getAlphabet(), teacher);

        learner.startLearning();
//        timer.stop();
        // store the time spent by the learning algorithm
//        options.stats.timeOfLearner += timer.getTimeElapsed();
        NBA hypothesis = null;
        while(true) {
            // output table or tree structure in verbose mode
            options.log.verbose("Table/Tree is both closed and consistent\n" + learner.toString());
            hypothesis = learner.getHypothesis();
            System.out.println(hypothesis);
            options.log.println("Resolving equivalence query for hypothesis (#Q=" + hypothesis.getStateSize() + ")...  ");
            Query<HashableValue> ceQuery = teacher.answerEquivalenceQuery(hypothesis);
            boolean isEq = ceQuery.getQueryAnswer().get();
            if(isEq) {
                // store statistics after learning is completed
//                prepareStats(options, learner, hypothesis);
                break;
            }
            ceQuery.answerQuery(null);
            // output the returned counterexample in verbose mode
            options.log.verbose("Counterexample is: " + ceQuery.toString());
//            timer.start();
            options.log.println("Refining current hypothesis...");
            learner.refineHypothesis(ceQuery);
//            timer.stop();
//            options.stats.timeOfLearner += timer.getTimeElapsed();
        }
        options.log.println("Learning completed...");
    }
    @Test
    public void iteTest(){
        Options options = new Options();
        options.inputFile = "/Users/lijianlin/Projects/roll-library-github/nba.hoa";
        options.format = Format.HOA;
// use the under-approximation method to construct a BA from an FDFA
        options.approximation = Options.Approximation.UNDER;
// set NBA learner, here we use tree-based syntactic FDFA learner
        options.algorithm = Options.Algorithm.SYNTACTIC;
        options.structure = Options.Structure.TREE;


//        Parser parser = UtilParser.prepare(options, options.inputFile , options.format);
//        NBA nba = parser.parse();
        NBA nba = nbaExample();

        Iterator<Triple<Integer,LearnerBase<NBA>,Optional<Query<HashableValue>>>> H =  LearningSequence.create(nba,options);

//        int cnt = 0;
//        while (H.hasNext()){
//            System.out.println(H.next());
////            cnt ++ ;
//        }

//        System.out.println(cnt);

        ImmutableList celist = LearningSequence.ceList(nba,options);
        ImmutableList ce2 = ImmutableList.builder().addAll(celist.subList(0,1)).build();
        LearnerNBALOmega learner = LearningSequence.refinedLearner(nba,options,celist);
        learner.getLearnerFDFA().getHypothesis();
        System.out.println(learner.toSVG());
    }
    @Test
    public void dfaIteTest(){
        Options options = new Options();
        options.inputFile = "/Users/lijianlin/Projects/roll-library-github/nba.hoa";
        options.format = Format.HOA;
// use the under-approximation method to construct a BA from an FDFA
        options.approximation = Options.Approximation.UNDER;
// set NBA learner, here we use tree-based syntactic FDFA learner
        options.algorithm = Options.Algorithm.DFA_COLUMN;
        options.structure = Options.Structure.TREE;


//        Parser parser = UtilParser.prepare(options, options.inputFile , options.format);
//        NBA nba = parser.parse();
        DFA dfa = dfaExample();

//        Iterator<Triple<Integer,LearnerBase<DFA>,Optional<Query<HashableValue>>>> H =  LearningSequence.create(dfa,options);

//        int cnt = 0;
//        while (H.hasNext()){
//            Triple<Integer,LearnerBase<DFA>,Optional<Query<HashableValue>>> t = H.next();
//            System.out.println(t);
//            System.out.println(t.getMiddle().getHypothesis().toDot());
////            cnt ++ ;
//        }

//        System.out.println(cnt);

        ImmutableList celist = LearningSequence.ceList(dfa,options);
        ArrayList a = new ArrayList<>(celist);
        a.size();
        System.out.println(celist);
//        ImmutableList ce2 = ImmutableList.builder().addAll(celist.subList(0,1)).build();
        LearnerBase<DFA> learner = LearningSequence.refinedLearner(dfa,options,celist.subList(0,1));
//        System.out.println(learner.getHypothesis().toDot());
//        learner.getLearnerFDFA().getHypothesis();
//        System.out.println(learner.toSVG());
    }

    @Test
    public void CETEST() throws Exception {
        NBA nba = nbaExample();
        Function<Query<HashableValue>,Boolean> memberAnswer = (querySimple -> {return false;});


        System.out.println(CE.withAlphabet(nba.getAlphabet()).finite("aaaa").toLaTex());
    }
    @Test
    public void SemiTest() throws Exception {
        Alphabet alphabet = new Alphabet();
        Character a = 'a';
        Character b = 'b';

        alphabet.addLetter(a);
        alphabet.addLetter(b);

        Options options = new Options();
        options.approximation = Options.Approximation.UNDER;
        options.algorithm = Options.Algorithm.SYNTACTIC;
        options.structure = Options.Structure.TREE;

        // a U G b
        BiFunction<String,String,Boolean> memberAnswer = (leading, period) -> {
            boolean visb = false;
            for (int i = 0; i < leading.length(); i++) {
                if (leading.charAt(i) == 'a' && visb) {
                    return false;
                } else if (leading.charAt(i) == 'b') {
                    visb = true;
                }
            }
            for (int i = 0; i < period.length(); i++) {
                if (period.charAt(i) != 'b'){
                    return false;
                }
            }
            return true;
        };
        SemiLearning p = new SemiLearning(alphabet,options,memberAnswer);
        System.out.println(p.getHypothesis().toString());

        QuerySimple<HashableValue> ce = CE.withAlphabet(alphabet).omega("a","b");
        ce.answerQuery(null);
        p.refineHypothesis(ce);

        System.out.println(p.getHypothesis().toString());
//
//        ce = CE.withAlphabet(alphabet).omega("b","b");
//        ce.answerQuery(null);
//        p.refineHypothesis(ce);
//
//        System.out.println(p.getHypothesis().toString());


    }
    @Test
    public void semiDFATest() throws Exception {
        Alphabet alphabet = new Alphabet();
        Character a = 'a';
        Character b = 'b';

        alphabet.addLetter(a);
        alphabet.addLetter(b);

        Options options = new Options();
        options.algorithm = Options.Algorithm.DFA_COLUMN;
        options.structure = Options.Structure.TABLE;

        // a*b+
        Function<String,Boolean> memberAnswer = (w) -> {
//            boolean visb = false;
//            for (int i = 0; i < w.length(); i++) {
//                if (w.charAt(i) == 'a' && visb) {
//                    return false;
//                } else if (w.charAt(i) == 'b') {
//                    visb = true;
//                }
//            }
//            return visb;
            long cntA = w.chars().filter(x -> x == 'a').count();
            long cntB = w.chars().filter(x -> x == 'a').count();
            return (cntA % 2 == 1) && (cntB % 2 == 1) && (cntA + cntB == w.length());


        };
        SemiLearning l = new SemiLearning(alphabet,options,memberAnswer);
        l.getHypothesis();
        l.refineHypothesis(CE.withAlphabet(alphabet).finite("bab"));
    }
}
