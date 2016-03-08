package org.elasticsearch.index.query.image;

import net.semanticmetadata.lire.imageanalysis.features.LireFeature;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.ToStringUtils;
import org.elasticsearch.common.lucene.search.Queries;

import java.io.IOException;
import java.util.*;

/**
 * Query by hash first and only calculate score for top n matches
 */
public class ImageHashLimitQuery extends Query {

    private String hashFieldName;
    private int[] hashes;
    private int maxResult;
    private String luceneFieldName;
    private LireFeature lireFeature;


    public ImageHashLimitQuery(String hashFieldName, int[] hashes, int maxResult, String luceneFieldName, LireFeature lireFeature, float boost) {
        this.hashFieldName = hashFieldName;
        this.hashes = hashes;
        this.maxResult = maxResult;
        this.luceneFieldName = luceneFieldName;
        this.lireFeature = lireFeature;
        setBoost(boost);
    }


    final class ImageHashScorer extends AbstractImageScorer {
        private int doc = -1;
        private final int maxDoc;
        private final int docBase;
        private final BitSet bitSet;

        ImageHashScorer(Weight weight, BitSet bitSet, LeafReaderContext context) {
            super(weight, luceneFieldName, lireFeature, context.reader(), ImageHashLimitQuery.this.getBoost());
            this.bitSet = bitSet;
            maxDoc = context.reader().maxDoc();
            docBase = context.docBase;
        }

        @Override
        public int docID() {
            return doc;
        }

        @Override
        public int nextDoc() throws IOException {
            int d;
            do {
                d = bitSet.nextSetBit(docBase + doc + 1);
                if (d == -1 || d >= maxDoc + docBase) {
                    doc = NO_MORE_DOCS;
                } else {
                    doc = d - docBase;
                }
            } while (doc != NO_MORE_DOCS && d < maxDoc + docBase);
            return doc;
        }

        @Override
        public int advance(int target) throws IOException {
            doc = target-1;
            return nextDoc();
        }

        @Override
        public long cost() {
            return maxDoc;
        }
    }

    final class ImageHashLimitWeight extends Weight {
        private final BitSet bitSet;
        private final IndexSearcher searcher;

        public ImageHashLimitWeight(IndexSearcher searcher, BitSet bitSet)
                throws IOException {
            super(ImageHashLimitQuery.this);
            this.bitSet = bitSet;
            this.searcher = searcher;
        }

        @Override
        public String toString() { return "weight(" + ImageHashLimitQuery.this + ")"; }

        @Override
        public float getValueForNormalization() {
            return 1f;
        }

        @Override
        public void normalize(float queryNorm, float topLevelBoost) {
        }

        @Override
        public Scorer scorer(LeafReaderContext context) throws IOException {
            return new ImageHashScorer(this, bitSet, context);
        }

        @Override
        public Explanation explain(LeafReaderContext context, int doc) throws IOException {
            Scorer scorer = scorer(context);
            boolean exists = (scorer != null && scorer.advance(doc) == doc);
            if(exists){
                float score = scorer.score();
                List<Explanation> details=new ArrayList<>();
                if (getBoost() != 1.0f) {
                    details.add(Explanation.match(getBoost(), "boost"));
                    score = score / getBoost();
                }
                details.add(Explanation.match(score ,"image score (1/distance)"));
                return Explanation.match(
                        score, ImageHashLimitQuery.this.toString() + ", product of:",details);
            }else{
                return Explanation.noMatch(ImageHashLimitQuery.this.toString() + " doesn't match id " + doc);
            }
        }

        @Override
        public void extractTerms(Set<Term> terms) {
        }
    }


    @Override
    public Weight createWeight(IndexSearcher searcher, boolean needsScores) throws IOException {
        IndexSearcher indexSearcher = new IndexSearcher(searcher.getIndexReader());
        indexSearcher.setSimilarity(new SimpleSimilarity());

        BooleanQuery.Builder builder=new BooleanQuery.Builder();
        for (int h : hashes) {
            builder.add(new BooleanClause(new TermQuery(new Term(hashFieldName, Integer.toString(h))), BooleanClause.Occur.SHOULD));
        }
        TopDocs topDocs = indexSearcher.search(builder.build(), maxResult);

        if (topDocs.scoreDocs.length == 0) {  // no result find
            return Queries.newMatchNoDocsQuery().createWeight(searcher,needsScores);
        }

        BitSet bitSet = new BitSet(topDocs.scoreDocs.length);
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            bitSet.set(scoreDoc.doc);
        }

        return new ImageHashLimitWeight(searcher, bitSet);
    }

    @Override
    public String toString(String field) {
        StringBuilder buffer = new StringBuilder();
        buffer.append(hashFieldName);
        buffer.append(",");
        buffer.append(Arrays.toString(hashes));
        buffer.append(",");
        buffer.append(maxResult);
        buffer.append(",");
        buffer.append(luceneFieldName);
        buffer.append(",");
        buffer.append(lireFeature.getClass().getSimpleName());
        buffer.append(ToStringUtils.boost(getBoost()));
        return buffer.toString();
    }


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ImageHashLimitQuery))
            return false;

        ImageHashLimitQuery that = (ImageHashLimitQuery) o;

        if (maxResult != that.maxResult) return false;
        if (!hashFieldName.equals(that.hashFieldName)) return false;
        if (!Arrays.equals(hashes, that.hashes)) return false;
        if (!lireFeature.equals(that.lireFeature)) return false;
        if (!luceneFieldName.equals(that.luceneFieldName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + hashFieldName.hashCode();
        result = 31 * result + Arrays.hashCode(hashes);
        result = 31 * result + maxResult;
        result = 31 * result + luceneFieldName.hashCode();
        result = 31 * result + lireFeature.hashCode();
        return result;
    }


    final class SimpleSimilarity extends DefaultSimilarity{
        @Override
        public float tf(float freq) {
            return 1;
        }

        @Override
        public float idf(long docFreq, long numDocs) {
            return 1;
        }

        @Override
        public float coord(int overlap, int maxOverlap) {
            return 1;
        }

        @Override
        public float queryNorm(float sumOfSquaredWeights) {
            return 1;
        }

        @Override
        public float lengthNorm(FieldInvertState state) {
            return 1;
        }

        @Override
        public float sloppyFreq(int distance) {
            return 1;
        }
    }
}
