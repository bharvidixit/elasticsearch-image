package org.elasticsearch.index.query.image;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by zengde on 2016-03-01.
 */
public abstract class AbstractImageWeight extends Weight{

    /**
     * Sole constructor, typically invoked by sub-classes.
     *
     * @param query the parent query
     */
    protected AbstractImageWeight(Query query) {
        super(query);
    }

    @Override
    public String toString() {
        return "weight(" + AbstractImageWeight.this + ")";
    }

    @Override
    public float getValueForNormalization() {
        return 1f;
    }

    @Override
    public void normalize(float queryNorm, float topLevelBoost) {
    }

    @Override
    public Explanation explain(LeafReaderContext context, int doc) throws IOException {
        Scorer scorer = scorer(context);
        boolean exists = (scorer != null && scorer.advance(doc) == doc);
        if(exists){
            float score = scorer.score();
            List<Explanation> details=new ArrayList<>();
            if (getQuery().getBoost() != 1.0f) {
                details.add(Explanation.match(getQuery().getBoost(), "boost"));
                score = score / getQuery().getBoost();
            }
            details.add(Explanation.match(score ,"image score (1/distance)"));
            return Explanation.match(
                    score, AbstractImageWeight.this.toString() + ", product of:",details);
        }else{
            return Explanation.noMatch(AbstractImageWeight.this.toString() + " doesn't match id " + doc);
        }
    }

    @Override
    public void extractTerms(Set<Term> terms) {
    }
}
