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

package roll.learner.dfa.table;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import roll.automata.DFA;
import roll.automata.StateDFA;
import roll.learner.LearnerDFA;
import roll.main.Options;
import roll.oracle.MembershipOracle;
import roll.query.Query;
import roll.query.QuerySimple;
import roll.table.ExprValue;
import roll.table.HashableValue;
import roll.table.ObservationRow;
import roll.table.ObservationTableAbstract;
import roll.words.Alphabet;
import roll.words.Word;

public abstract class LearnerDFATable extends LearnerDFA {
    
    protected ObservationTableAbstract observationTable;
    
    public LearnerDFATable(Options options, Alphabet alphabet
            , MembershipOracle<HashableValue> membershipOracle) {
        super(options, alphabet, membershipOracle);
        this.observationTable = getTableInstance();
    }
    
    protected Query<HashableValue> processMembershipQuery(ObservationRow row, int offset, ExprValue valueExpr) {
        Query<HashableValue> query = new QuerySimple<>(row, row.getWord(), valueExpr.get(), offset);
        HashableValue result = membershipOracle.answerMembershipQuery(query);
        Query<HashableValue> queryResult = new QuerySimple<>(row, row.getWord(), valueExpr.get(), offset);
        queryResult.answerQuery(result);
        return queryResult;
    }
    
    protected void initialize() {
        
        observationTable.clear();
        Word wordEmpty = alphabet.getEmptyWord();
        observationTable.addUpperRow(wordEmpty);
        ExprValue exprValue = getInitialColumnExprValue();
        
        // add empty word column
        observationTable.addColumn(exprValue);
        // add every alphabet
        for(int letterNr = 0; letterNr < alphabet.getLetterSize(); letterNr ++) {
            observationTable.addLowerRow(alphabet.getLetterWord(letterNr));
        }
        
        // ask initial queries for upper table
        processMembershipQueries(observationTable.getUpperTable()
                , 0, observationTable.getColumns().size());
        // ask initial queries for lower table
        processMembershipQueries(observationTable.getLowerTable()
                , 0, observationTable.getColumns().size());
        
        makeTableClosed();
        
    }
    
    protected void processMembershipQueries(List<ObservationRow> rows
            , int colOffset, int length) {
        List<Query<HashableValue>> results = new ArrayList<>();
        List<ExprValue> columns = observationTable.getColumns();
        int endNr = length + colOffset;
        for(ObservationRow row : rows) {
            for(int colNr = colOffset; colNr < endNr; colNr ++) {
                results.add(processMembershipQuery(row, colNr, columns.get(colNr)));
            }
        }
        putQueryAnswers(results);
    }
        
    protected void putQueryAnswers(List<Query<HashableValue>> queries) {
        for(Query<HashableValue> query : queries) {
            putQueryAnswers(query);
        }
    }
    
    protected void putQueryAnswers(Query<HashableValue> query) {
        ObservationRow row = query.getPrefixRow();
        HashableValue result = query.getQueryAnswer();
        assert result != null;
        row.set(query.getSuffixColumn(), result);
    }
    
    protected void makeTableClosed() {
        ObservationRow lowerRow = observationTable.getUnclosedLowerRow();
        
        while(lowerRow != null) {
            // 1. move to upper table
            observationTable.moveRowFromLowerToUpper(lowerRow);
            // 2. add one letter to lower table
            List<ObservationRow> newLowerRows = new ArrayList<>();
            for(int letterNr = 0; letterNr < alphabet.getLetterSize(); letterNr ++) {
                Word newWord = lowerRow.getWord().append(letterNr);
                ObservationRow row = observationTable.getTableRow(newWord); // already existing
                if(row != null) continue;
                ObservationRow newRow = observationTable.addLowerRow(newWord);
                newLowerRows.add(newRow);
            }
            // 3. process membership queries
            processMembershipQueries(newLowerRows, 0, observationTable.getColumns().size());
            lowerRow = observationTable.getUnclosedLowerRow();
        }
        
        constructHypothesis();
    }
    
    // return counter example for hypothesis
    @Override
    public void refineHypothesis(Query<HashableValue> ceQuery) {
        
        ExprValue exprValue = getCounterExampleWord(ceQuery);
        HashableValue result = ceQuery.getQueryAnswer();
        if(result == null) {
            result = processMembershipQuery(ceQuery);
        }
        CeAnalyzer analyzer = getCeAnalyzerInstance(exprValue, result);
        analyzer.analyze();
        observationTable.addColumn(analyzer.getNewExpriment()); // add new experiment
        processMembershipQueries(observationTable.getUpperTable(), observationTable.getColumns().size() - 1, 1);
        processMembershipQueries(observationTable.getLowerTable(), observationTable.getColumns().size() - 1, 1);
        
        makeTableClosed();
        
    }
    
    
    // Default learner for DFA
    protected void constructHypothesis() {
//        oldHyp = dfa;
//        if (oldHyp != null) {
//            oldHyp.colored = ImmutableList.of(stateToSplit);
//        }
        dfa = new DFA(alphabet);
        
        List<ObservationRow> upperTable = observationTable.getUpperTable();
        
        for(int rowNr = 0; rowNr < upperTable.size(); rowNr ++) {
            dfa.createState();
        }
        
        for(int rowNr = 0; rowNr < upperTable.size(); rowNr ++) {
            StateDFA state = dfa.getState(rowNr);
            for(int letterNr = 0; letterNr < alphabet.getLetterSize(); letterNr ++) {
                int succNr = getSuccessorRow(rowNr, letterNr);
                state.addTransition(letterNr, succNr);
            }
            
            if(getStateLabel(rowNr).isEmpty()) {
                dfa.setInitial(rowNr);
            }
            
            if(isAccepting(rowNr)) {
                dfa.setFinal(rowNr);
            }
        }
//        dfa.previous = oldHyp;
    }
    
    // a state is accepting iff it accepts empty language
    protected boolean isAccepting(int state) {
        ObservationRow stateRow = observationTable.getUpperTable().get(state);
        int emptyNr = observationTable.getColumnIndex(getExprValueWord(alphabet.getEmptyWord()));
        assert emptyNr != -1 : "index -> " + emptyNr;
        return stateRow.getValues().get(emptyNr).isAccepting();
    }

    protected int getSuccessorRow(int state, int letter) {
        ObservationRow stateRow = observationTable.getUpperTable().get(state);
        Word succWord = stateRow.getWord().append(letter);

        // search in upper table
        for(int succ = 0; succ < observationTable.getUpperTable().size(); succ ++) {
            ObservationRow succRow = observationTable.getUpperTable().get(succ);
            if(succRow.getWord().equals(succWord)) {
                return succ;
            }
        }
        // search in lower table
        ObservationRow succRow = observationTable.getLowerTableRow(succWord);
        assert succRow != null;
        for(int succ = 0; succ < observationTable.getUpperTable().size(); succ ++) {
            ObservationRow upperRow = observationTable.getUpperTable().get(succ);
            if(succRow.valuesEqual(upperRow)) {
                return succ;
            }
        }
        assert false : "successor values not found";
        return -1;
    }

    
    public String toString() {
        return observationTable.toString();
    }
    
    @Override
    public Word getStateLabel(int state) {
        return observationTable.getUpperTable().get(state).getWord();
    }
    
    protected ObservationTableAbstract getTableInstance() {
        return new ObservationTableDFA();
    }
    
    protected class CeAnalyzerTable extends CeAnalyzer {

        public CeAnalyzerTable(ExprValue exprValue, HashableValue result) {
            super(exprValue, result);
        }

        @Override
        protected void update(CeAnalysisResult result) {
            Word wordCE = getWordExperiment();
            wordExpr = getExprValueWord(wordCE.getSuffix(result.breakIndex + 1));  // y[j+1..n]
        }
    }

    
}
