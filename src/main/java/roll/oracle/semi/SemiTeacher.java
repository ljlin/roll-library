package roll.oracle.semi;

import roll.oracle.MembershipOracle;
import roll.query.Query;
import roll.table.HashableValue;
import roll.table.HashableValueBoolean;

import java.util.function.BiFunction;
import java.util.function.Function;

public class SemiTeacher implements MembershipOracle<HashableValue> {
    private Function<Query<HashableValue>,Boolean>  delegate;

    public SemiTeacher(BiFunction<String,String,Boolean> f) {
        this.delegate = (query -> f.apply(
                query.getPrefix().toStringWithAlphabet(),
                query.getSuffix().toStringWithAlphabet()
            )
        );
    }

    public SemiTeacher(Function<String,Boolean> f) {
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
