package roll.query;

import roll.words.Alphabet;

public class CE {
    Alphabet alphabet;
    static public CE withAlphabet(Alphabet alphabet) {
        return new CE(alphabet);
    }

    private CE(Alphabet alphabet) {
        this.alphabet = alphabet;
    }

    public QuerySimple omega(String leading, String period){
        return new QuerySimple(
                this.alphabet.getWordFromString(leading),
                this.alphabet.getWordFromString(period)
        );
    }

    public QuerySimple finite(String s){
        return new QuerySimple(
                this.alphabet.getWordFromString(s)
        );
    }
}
