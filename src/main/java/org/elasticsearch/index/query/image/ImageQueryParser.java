package org.elasticsearch.index.query.image;


import net.semanticmetadata.lire.builders.DocumentBuilder;
import net.semanticmetadata.lire.imageanalysis.features.Extractor;
import net.semanticmetadata.lire.imageanalysis.features.LireFeature;
import net.semanticmetadata.lire.utils.ImageUtils;
import org.apache.lucene.search.Query;
import org.elasticsearch.ElasticsearchImageProcessException;
import org.elasticsearch.common.io.stream.ByteBufferStreamInput;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.mapper.image.GlobalFeatureEnum;
import org.elasticsearch.index.query.QueryParseContext;
import org.elasticsearch.index.query.QueryParser;
import org.elasticsearch.index.query.QueryParsingException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ImageQueryParser implements QueryParser {

    public static final String NAME = "image";

    @Override
    public String[] names() {
        return new String[] {NAME};
    }

    @Override
    public Query parse(QueryParseContext parseContext) throws IOException, QueryParsingException {
        XContentParser parser = parseContext.parser();

        XContentParser.Token token = parser.nextToken();
        if (token != XContentParser.Token.FIELD_NAME) {
            throw new QueryParsingException(parseContext, "[image] query malformed, no field");
        }

        float boost = 1.0f;
        GlobalFeatureEnum globalfeatureEnum = null;
        byte[] image=null;
        token = parser.nextToken();
        if (token == XContentParser.Token.START_OBJECT) {
            String currentFieldName = null;
            while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                if (token == XContentParser.Token.FIELD_NAME) {
                    currentFieldName = parser.currentName();
                } else {
                    if ("feature".equals(currentFieldName)) {
                        globalfeatureEnum = GlobalFeatureEnum.getByName(parser.text());
                    } else if ("image".equals(currentFieldName)) {
                        image = parser.binaryValue();
                    }else if ("boost".equals(currentFieldName)) {
                        boost = parser.floatValue();
                    }
                    /*else {
                        throw new QueryParsingException(parseContext, "[image] query does not support [" + currentFieldName + "]");
                    }*/
                }
            }
            parser.nextToken();
        }

        if (globalfeatureEnum == null) {
            throw new QueryParsingException(parseContext, "No feature specified for image query");
        }

        LireFeature feature = null;

        if (image != null) {
            try {
                feature = globalfeatureEnum.getGlobalFeatureClass().newInstance();
                BufferedImage img = ImageIO.read(new ByteBufferStreamInput(ByteBuffer.wrap(image)));
                if (Math.max(img.getHeight(), img.getWidth()) > DocumentBuilder.MAX_IMAGE_DIMENSION) {
                    img = ImageUtils.scaleImage(img, DocumentBuilder.MAX_IMAGE_DIMENSION);
                }
                ((Extractor)feature).extract(img);
            } catch (Exception e) {
                throw new ElasticsearchImageProcessException("Failed to parse image", e);
            }
        }

        return new ImageQuery(feature,globalfeatureEnum,boost);
    }
}
