package org.elasticsearch.index.query.image;

import net.semanticmetadata.lire.imageanalysis.features.LireFeature;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.util.ToStringUtils;
import org.elasticsearch.ElasticsearchImageProcessException;
import org.elasticsearch.index.mapper.image.GlobalFeatureEnum;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Created by zengde on 2016/3/25.
 * Copied from {@link MatchAllDocsQuery}, calculate score for all docs
 */
public class ImageQuery  extends Query {
    protected Logger logger = Logger.getLogger(getClass().getName());
    protected String fieldName, codebookName;
    protected LireFeature cachedInstance = null;


    protected double maxDistance=-1d;
    private LireFeature feature;

    @SuppressWarnings("deprecation")
    public ImageQuery(LireFeature feature, GlobalFeatureEnum globalfeatureEnum, float boost) {
        try {
            this.feature=feature;
            this.fieldName = feature.getFieldName();
            this.cachedInstance = globalfeatureEnum.getGlobalFeatureClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ElasticsearchImageProcessException("Failed to initial image feature class", e);
        }
        setBoost(boost);
    }

    /**
     * Main similarity method called for each and every document in the index.
     *
     * @param document
     * @param lireFeature
     * @return the distance between the given feature and the feature stored in the document.
     */
    protected double getDistance(Document document, LireFeature lireFeature) {
        if (document.getField(fieldName).binaryValue() != null && document.getField(fieldName).binaryValue().length > 0) {
            cachedInstance.setByteArrayRepresentation(document.getField(fieldName).binaryValue().bytes, document.getField(fieldName).binaryValue().offset, document.getField(fieldName).binaryValue().length);
            return lireFeature.getDistance(cachedInstance);
        } else {
            logger.warning("No feature stored in this document! (" + lireFeature.getFeatureName() + ")");
        }
        return 0d;
    }

    @Override
    public Weight createWeight(IndexSearcher searcher, boolean needsScores) throws IOException {
        return new ConstantScoreWeight(this){
            @Override
            public String toString() {
                return "weight(" + ImageQuery.this + ")";
            }
            @Override
            public Scorer scorer(LeafReaderContext context) throws IOException {
                return new ImageScorer(context.reader(), this,DocIdSetIterator.all(context.reader().maxDoc()),getBoost());
            }
        };
    }

    @Override
    public String toString(String field) {
        return fieldName +
                "," +
                cachedInstance.getClass().getSimpleName() +
                ToStringUtils.boost(getBoost());
    }

    //ConstantScoreScorer
    private class ImageScorer extends Scorer{
        private final TwoPhaseIterator twoPhaseIterator;
        private final DocIdSetIterator disi;
        private final IndexReader reader;
        private final float boost;

        public ImageScorer(LeafReader reader, Weight imageWeight, DocIdSetIterator disi,float boost) {
            super(imageWeight);
            this.reader = reader;
            this.twoPhaseIterator = null;
            this.disi = disi;
            this.boost=boost;
        }

        @Override
        public float score() throws IOException {
            assert docID() != NO_MORE_DOCS;
            Document doc=reader.document(docID());
            double tmpDistance=getDistance(doc,feature);
            assert (tmpDistance >= 0);
            if (tmpDistance > maxDistance) maxDistance = tmpDistance;
            float score=(float) tmpDistance;
            if (Float.compare(score, 1.0f) <= 0) { // distance less than 1, consider as same image
                score = 2f - score;
            } else {
                score = 1 / score;
            }
            return score * boost;
        }

        @Override
        public int freq() throws IOException {
            return 1;
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
}
