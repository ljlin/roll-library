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

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * @author Yong Li (liyong@ios.ac.cn)
 * */

// all DFA state will be complete in the sense that
// it has successors for every letter
public class StateDFA extends StateFA {
    private final DFA dfa;
    private final int[] successors; // // Alphabet -> Q
    
    public StateDFA(final DFA dfa, final int id) {
        super(id);
        assert dfa != null;
        this.dfa = dfa;
        this.successors = new int[dfa.getAlphabetSize()];
        Arrays.fill(successors, -1); // with initial value -1
    }

    @Override
    public DFA getFA() {
        return dfa;
    }

    @Override
    public void addTransition(int letter, int state) {
        assert dfa.checkValidLetter(letter);
        successors[letter] = state;
    }
    
    public int getSuccessor(int letter) {
        assert dfa.checkValidLetter(letter);
        return successors[letter];
    }
    
    @Override
    public String toString() {
        List<String> apList = new ArrayList<>();
        for(int i = 0; i < dfa.getAlphabetSize(); i ++) {
            apList.add("" + i);
        }
        return toString(apList);
    }
    
    @Override
    public String toString(List<String> apList) {
        return toDot(apList,null);
//        StringBuilder builder = new StringBuilder();
//        builder.append("  " + getId() + " [label=\"" + getId() + "\"");
//        if(dfa.isFinal(getId())) builder.append(", shape = doublecircle");
//        else builder.append(", shape = circle");
//        builder.append("];\n");
//        // transitions
//        for(int i = 0; i < successors.length; i ++) {
//            builder.append("  " + getId() + " -> " + successors[i]
//                    + " [label=\"" + apList.get(i) + "\"];\n");
//        }
//        return builder.toString();
    }
    public String toDot(List<String> apList,String fillcolor){
        final List<String> list =
                Optional.ofNullable(apList).orElse(
                        IntStream.range(0,dfa.getAlphabetSize())
                                .mapToObj(Integer::toString)
                                .collect(ImmutableList.<String>toImmutableList())
                );
        StringBuilder builder = new StringBuilder();
        builder.append("  " + getId() + " [label=\"" + getId() + "\"");
        if(dfa.isFinal(getId())) builder.append(", shape = doublecircle");
        else builder.append(", shape = circle");
        if (fillcolor !=null){
            builder.append("," +
                    "style=\"filled\", \n" +
                    "fillcolor=\""+fillcolor+"\", ");
        }
        builder.append("];\n");

        // transitions
        for(int i = 0; i < successors.length; i ++) {
            builder.append("  " + getId() + " -> " + successors[i]
                    + " [label=\"" + list.get(i) + "\"];\n");
        }
        return builder.toString();
    }
    @Override
    public String toBA() {
        StringBuilder builder = new StringBuilder();
        // transitions
        for(int i = 0; i < successors.length; i ++) {
            builder.append("a" + i + ",[" + getId() + "]->[" + successors[i] + "]\n");
        }
        return builder.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(obj == null) return false;
        if(obj instanceof StateDFA) {
            StateDFA other = (StateDFA)obj;
            return getId() == other.getId()
                && dfa == other.dfa;
        }
        return false;
    }
    
}
