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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jupyter.Displayer;
import jupyter.Displayers;
import jupyter.MIMETypes;
import roll.automata.DFA;
import roll.automata.FASimple;
import roll.automata.NBA;
import roll.learner.LearnerBase;
import roll.main.Options;
import roll.words.Alphabet;

/**
 * @author Jianlin Li, Yong Li (liyong@ios.ac.cn)
 * */

public class ROLLNotebook {
    
    static {
        register();
    }
    
    static String dotToSVG(String dot) {
        return NativeTool.dot2SVG(dot);
    }
    
    static void register(){
        // for FASimple
        Displayers.register(FASimple.class, new Displayer<FASimple>() {
            @Override
            public Map<String, String> display(FASimple automaton) {
                return new HashMap<String, String>() {
                    /**
                    * 
                    */
                    private static final long serialVersionUID = 1L;

                    {
                        put(MIMETypes.HTML, dotToSVG(automaton.toDot()));
                    }
                };
            }
        });
        
        Displayers.register(LearnerBase.class, new Displayer<LearnerBase>() {
            @Override
            public Map<String, String> display(LearnerBase learner) {
                return new HashMap<String, String>() {
                    /**
                    * 
                    */
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
    }
    
    public static Alphabet alphabet;
    
    public static void createAlphabet(List<Character> array) {
        alphabet = new Alphabet();
        for(Character letter : array) {
            alphabet.addLetter(letter);
        }
    }
    
    public static NBA createNBA() {
        if(alphabet == null) throw new UnsupportedOperationException("Alphabet is empty");
        return new NBA(alphabet);
    }
    
    public static DFA createDFA() {
        if(alphabet == null) throw new UnsupportedOperationException("Alphabet is empty");
        return new DFA(alphabet);
    }
}
