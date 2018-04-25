package roll.notebook;

public interface JupyterLearner<M> {
    M getHypothesis();
    boolean isTable();
}

