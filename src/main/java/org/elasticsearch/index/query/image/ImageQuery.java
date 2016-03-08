package org.elasticsearch.index.query.image;

import net.semanticmetadata.lire.imageanalysis.features.LireFeature;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.*;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.ToStringUtils;
import java.io.IOException;

/**
 * Copied from {@link MatchAllDocsQuery}, calculate score for all docs
 */
public class ImageQuery extends Query {

    private String luceneFieldName;
    private LireFeature lireFeature;

    public ImageQuery(String luceneFieldName, LireFeature lireFeature, float boost) {
        this.luceneFieldName = luceneFieldName;
        this.lireFeature = lireFeature;
        setBoost(boost);
    }

    private class ImageScorer extends AbstractImageScorer {
        private final TwoPhaseIterator twoPhaseIterator;
        private final DocIdSetIterator disi;

        /** Constructor based on a {@link TwoPhaseIterator}. In that case the
         *  {@link Scorer} will support two-phase iteration.
         *  @param w the parent weight
         *  @param twoPhaseIterator the iterator that defines matching documents */
        public ImageScorer(IndexReader reader, Weight w, TwoPhaseIterator twoPhaseIterator) {
            super(w, luceneFieldName, lireFeature, reader, ImageQuery.this.getBoost());
            this.twoPhaseIterator = twoPhaseIterator;
            this.disi = TwoPhaseIterator.asDocIdSetIterator(twoPhaseIterator);
        }

        @Override
        public TwoPhaseIterator asTwoPhaseIterator() {
            return twoPhaseIterator;
        }

        @Override
        public int docID() {
            return disi.docID();
        }

        @Override
        public int nextDoc() throws IOException {
            return disi.nextDoc();
        }

        @Override
        public int advance(int target) throws IOException {
            return disi.advance(target);
        }

        @Override
        public long cost() {
            return disi.cost();
        }
    }

    private class ImageWeight extends AbstractImageWeight {
        protected ImageWeight(Query query) {
            super(query);
        }

        @Override
        public Scorer scorer(LeafReaderContext context) throws IOException {
            final Bits matchingDocs = new Bits.MatchAllBits(context.reader().maxDoc());
            final DocIdSetIterator approximation = DocIdSetIterator.all(context.reader().maxDoc());
            final TwoPhaseIterator twoPhase = new TwoPhaseIterator(approximation) {

                @Override
                public boolean matches() throws IOException {
                    final int doc = approximation.docID();

                    return matchingDocs.get(doc);
                }
            };
            return new ImageScorer(context.reader(), this,twoPhase);
        }

    }

    @Override
    public Weight createWeight(IndexSearcher searcher, boolean needsScores) {
        return new ImageWeight(this);
    }

    @Override
    public String toString(String field) {
        StringBuilder buffer = new StringBuilder();
        buffer.append(luceneFieldName);
        buffer.append(",");
        buffer.append(lireFeature.getClass().getSimpleName());
        buffer.append(ToStringUtils.boost(getBoost()));
        return buffer.toString();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;
        if (!(o instanceof ImageQuery))
            return false;
        ImageQuery other = (ImageQuery) o;
        return (this.getBoost() == other.getBoost())
                && luceneFieldName.equals(other.luceneFieldName)
                && lireFeature.equals(other.lireFeature);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + luceneFieldName.hashCode();
        result = 31 * result + lireFeature.hashCode();
        result = Float.floatToIntBits(getBoost()) ^ result;
        return result;
    }


}
