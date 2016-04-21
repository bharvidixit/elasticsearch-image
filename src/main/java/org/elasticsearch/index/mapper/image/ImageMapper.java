package org.elasticsearch.index.mapper.image;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import net.semanticmetadata.lire.builders.GlobalDocumentBuilder;
import net.semanticmetadata.lire.builders.GlobalDocumentBuilder.HashingMode;
import net.semanticmetadata.lire.imageanalysis.features.Extractor;
import net.semanticmetadata.lire.imageanalysis.features.LireFeature;
import net.semanticmetadata.lire.imageanalysis.features.global.AutoColorCorrelogram;
import net.semanticmetadata.lire.imageanalysis.features.global.CEDD;
import net.semanticmetadata.lire.imageanalysis.features.global.FCTH;
import net.semanticmetadata.lire.indexers.hashing.BitSampling;
import net.semanticmetadata.lire.indexers.hashing.LocalitySensitiveHashing;
import net.semanticmetadata.lire.utils.ImageUtils;
import net.semanticmetadata.lire.utils.SerializationUtils;
import org.apache.lucene.document.BinaryDocValuesField;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.ElasticsearchGenerationException;
import org.elasticsearch.ElasticsearchImageProcessException;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import org.elasticsearch.common.io.stream.ByteBufferStreamInput;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.mapper.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

import static org.elasticsearch.index.mapper.MapperBuilders.binaryField;
import static org.elasticsearch.index.mapper.MapperBuilders.stringField;
import static org.elasticsearch.index.mapper.core.TypeParsers.parseField;

public class ImageMapper extends FieldMapper {
    public static final String CONTENT_TYPE = "image";

    public static final String HASH = "hash";
    public static final String FEATURE = "feature";

    public static final String LSH_HASH_FILE = "/hash/lshHashFunctions.obj";

    public static class Defaults {
        public static final ImageFieldType FIELD_TYPE = new ImageFieldType();
        static {
            FIELD_TYPE.setIndexOptions(IndexOptions.DOCS);
            FIELD_TYPE.setTokenized(false);
            FIELD_TYPE.freeze();
        }
    }

    static final class ImageFieldType extends MappedFieldType {
        public ImageFieldType() {}

        protected ImageFieldType(ImageMapper.ImageFieldType ref) {
            super(ref);
        }

        @Override
        public ImageMapper.ImageFieldType clone() {
            return new ImageMapper.ImageFieldType(this);
        }

        @Override
        public String typeName() {
            return CONTENT_TYPE;
        }

        public String value(Object value) {
            return value == null?null:value.toString();
        }
    }

    public static class Builder extends FieldMapper.Builder<Builder, ImageMapper> {
        private List<String> features;
        private String hash="";

        public Builder(String name) {
            super(name,Defaults.FIELD_TYPE ,Defaults.FIELD_TYPE);
            builder = this;
        }

        public void setFeatures(List<String> features) {
            this.features = features;
        }

        public void setHash(String hash) {
            this.hash = hash;
        }

        @Override
        public ImageMapper build(BuilderContext context) {
            setupFieldType(context);
            return new ImageMapper(
                    name, fieldType, defaultFieldType, context.indexSettings(), multiFieldsBuilder.build(this, context), copyTo,
                    features,hash);
        }
    }

    public static class TypeParser implements Mapper.TypeParser {
        @Override
        @SuppressWarnings("unchecked")
        public Mapper.Builder parse(String name, Map<String, Object> node, ParserContext parserContext) throws MapperParsingException {
            ImageMapper.Builder builder = new ImageMapper.Builder(name);
            parseField(builder, name, node, parserContext);
            List<String> features;
            for (Iterator<Map.Entry<String, Object>> iterator = node.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry<String, Object> entry = iterator.next();
                String propName = Strings.toUnderscoreCase(entry.getKey());
                Object propNode = entry.getValue();
                if(FEATURE.equals(propName)){
                    features= (List) propNode;
                    if (features == null || features.isEmpty()) {
                        throw new ElasticsearchGenerationException("Feature not found");
                    }
                    builder.setFeatures(features);
                    iterator.remove();
                }else if(HASH.equals(propName)){
                    if (propNode == null) {
                        throw new ElasticsearchGenerationException("Hashmode not found");
                    }
                    builder.setHash(propNode.toString());
                    iterator.remove();
                }
            }
            return builder;
        }
    }

    private List<String> features;
    private HashingMode hashingMode;

    protected ImageMapper(String simpleName, MappedFieldType fieldType, MappedFieldType defaultFieldType, Settings indexSettings, MultiFields multiFields, CopyTo copyTo,
                          List<String> features, String hash) {
        super(simpleName, fieldType, defaultFieldType, indexSettings, multiFields, copyTo);
        this.features=features;
        if(!hash.isEmpty()){
            try{
                this.hashingMode= HashingMode.valueOf(hash);
            }catch (Exception e){
                this.hashingMode=HashingMode.None;
            }
        }
        if(hashingMode.equals(HashingMode.LSH)){
            //LocalitySensitiveHashing.generateHashFunctions();
            try {
                LocalitySensitiveHashing.readHashFunctions(ImageMapper.class.getResourceAsStream(LSH_HASH_FILE));
            } catch (IOException e) {
                throw new ElasticsearchImageProcessException("Failed to initialize hash function", e);
            }
        }
    }

    @Override
    protected void parseCreateField(ParseContext context, List<Field> fields) throws IOException {
        byte[] content = null;

        XContentParser parser = context.parser();
        XContentParser.Token token = parser.currentToken();
        if (token == XContentParser.Token.VALUE_STRING) {
            content = parser.binaryValue();
        }

        if (content == null) {
            throw new MapperParsingException("No content is provided.");
        }

        GlobalDocumentBuilder globalDocumentBuilder = hashingMode.equals(HashingMode.None)? new GlobalDocumentBuilder():
                new GlobalDocumentBuilder(true,hashingMode);
        for(String featurename:features){
            globalDocumentBuilder.addExtractor(GlobalFeatureEnum.getByName(featurename).getGlobalFeatureClass());
        }

        BufferedImage img = ImageIO.read(new ByteBufferStreamInput(ByteBuffer.wrap(content)));
        Field[] imagefields=globalDocumentBuilder.createDescriptorFields(img);
        Collections.addAll(fields,imagefields);
    }

    @Override
    protected String contentType() {
        return CONTENT_TYPE;
    }

    @Override
    protected void doXContentBody(XContentBuilder builder, boolean includeDefaults, Params params) throws IOException {
        super.doXContentBody(builder, includeDefaults, params);

        builder.startArray(FEATURE);
        for(String featurename:features){
            builder.value(featurename);
        }
        builder.endArray();
        builder.field(HASH, hashingMode);
    }
}
