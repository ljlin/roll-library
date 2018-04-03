package test;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.Test;
import roll.automata.NBA;
import roll.learner.Learner;
import roll.learner.LearnerBase;
import roll.learner.fdfa.tree.LearnerOmegaTree;
import roll.learner.nba.lomega.LearnerNBALOmega;
import roll.main.LearningSequence;
import roll.main.Options;
import roll.oracle.nba.rabit.TeacherNBARABIT;
import roll.parser.Format;
import roll.parser.Parser;
import roll.parser.UtilParser;
import roll.query.Query;
import roll.table.HashableValue;
import roll.words.Alphabet;

import java.util.Iterator;
import java.util.Optional;

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

        Iterator<Triple<Integer,LearnerBase<NBA>,Optional<Query<HashableValue>>>> H =  LearningSequence.creat(nba,options);

//        int cnt = 0;
//        while (H.hasNext()){
//            System.out.println(H.next());
////            cnt ++ ;
//        }

//        System.out.println(cnt);

        ImmutableList celist = LearningSequence.ceList(nba,options);
        ImmutableList ce2 = ImmutableList.of(celist.subList(0,5),null);
        LearnerNBALOmega learner = LearningSequence.refinedLearner(nba,options,celist);
        System.out.println(learner.toSVG());
    }

}
