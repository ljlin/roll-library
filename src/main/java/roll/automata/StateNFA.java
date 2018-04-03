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

package roll.automata;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntObjectProcedure;
import gnu.trove.procedure.TIntProcedure;
import roll.util.sets.ISet;
import roll.util.sets.UtilISet;

/**
 * @author Yong Li (liyong@ios.ac.cn)
 * 
 * NFA is allowed to be incomplete
 * */

public class StateNFA extends StateFA {
    private final NFA nfa;
    private final TIntObjectMap<ISet> successors; // Alphabet -> 2^Q
    
    public StateNFA(final NFA nfa, final int id) {
        super(id);
        assert nfa != null;
        this.nfa = nfa;
        this.successors = new TIntObjectHashMap<>();
    }

    @Override
    public NFA getFA() {
        return nfa;
    }

    @Override
    public void addTransition(int letter, int state) {
        assert nfa.checkValidLetter(letter);
        ISet succs = successors.get(letter);
        if(succs == null) {
            succs = UtilISet.newISet();
        }
        succs.set(state);
        successors.put(letter, succs);
    }
    
    public ISet getSuccessors(int letter) {
        assert nfa.checkValidLetter(letter);
        ISet succs = successors.get(letter);
        if(succs == null) {
            return UtilISet.newISet();
        }
        return succs;
    }
    
    public ISet getEnabledLetters() {
        ISet letters = UtilISet.newISet();
        TIntProcedure procedure = new TIntProcedure() {
            @Override
            public boolean execute(int letter) {
                letters.set(letter);
                return true;
            }
        };
        successors.forEachKey(procedure);
        return letters;
    }
    
    public void forEachEnabledLetter(TIntProcedure procedure) {
        successors.forEachKey(procedure);
    }
    
    @Override
    public String toString() {
        List<String> apList = new ArrayList<>();
        for(int i = 0; i < nfa.getAlphabetSize(); i ++) {
            apList.add("" + i);
        }
        return toString(apList);
    }
    
    @Override
    public String toString(List<String> apList) {
        StringBuilder builder = new StringBuilder();
        builder.append("  " + getId() + " [label=\"" + getId() + "\"");
        if(nfa.isFinal(getId())) builder.append(", shape = doublecircle");
        else builder.append(", shape = circle");
        builder.append("];\n");
        // transitions
        TIntObjectProcedure<ISet> procedure = new TIntObjectProcedure<ISet> () {
            @Override
            public boolean execute(int letter, ISet succs) {
                for(int succ : succs) {
                    builder.append("  " + getId() + " -> " + succ
                            + " [label=\"" + apList.get(letter) + "\"];\n");
                }
                return true;
            }
        };
        successors.forEachEntry(procedure);
        return builder.toString();
    }

    @Override
    public String toDot(List<String> apList, String fillcolor) {
        final List<String> list =
                Optional.ofNullable(apList).orElse(
                        IntStream.range(0,nfa.getAlphabetSize())
                                .mapToObj(Integer::toString)
                                .collect(ImmutableList.<String>toImmutableList())
                );


        StringBuilder builder = new StringBuilder();
        builder.append("  " + getId() + " [label=\"" + getId() + "\"");
        if(nfa.isFinal(getId())) builder.append(", shape = doublecircle");
        else builder.append(", shape = circle");
        if (fillcolor !=null){
            builder.append("," +
                    "style=\"filled\", \n" +
                    "fillcolor=\""+fillcolor+"\", ");
        }
        builder.append("];\n");
        // transitions
        TIntObjectProcedure<ISet> procedure = new TIntObjectProcedure<ISet> () {
            @Override
            public boolean execute(int letter, ISet succs) {
                for(int succ : succs) {
                    builder.append("  " + getId() + " -> " + succ
                            + " [label=\"" + list.get(letter) + "\"];\n");
                }
                return true;
            }
        };
        successors.forEachEntry(procedure);
        return builder.toString();
    }

    @Override
    public String toBA() {
        StringBuilder builder = new StringBuilder();
        // transitions
        TIntObjectProcedure<ISet> procedure = new TIntObjectProcedure<ISet> () {
            @Override
            public boolean execute(int letter, ISet succs) {
                for(int succ : succs) {
                    builder.append("a" + letter + ",[" + getId() + "]->[" + succ + "]\n");
                }
                return true;
            }
        };
        successors.forEachEntry(procedure);
        return builder.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(obj == null) return false;
        if(obj instanceof StateNFA) {
            StateNFA other = (StateNFA)obj;
            return getId() == other.getId()
                && nfa == other.nfa;
        }
        return false;
    }
    
}
