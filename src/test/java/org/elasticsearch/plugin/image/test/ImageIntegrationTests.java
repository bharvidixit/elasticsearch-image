package org.elasticsearch.plugin.image.test;

import net.semanticmetadata.lire.builders.GlobalDocumentBuilder;
import org.apache.sanselan.ImageFormat;
import org.apache.sanselan.ImageWriteException;
import org.apache.sanselan.Sanselan;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import com.google.common.collect.Maps;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.mapper.image.GlobalFeatureEnum;
import org.elasticsearch.index.query.image.ImageQueryBuilder;
import org.elasticsearch.plugin.image.ImagePlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.test.ESIntegTestCase;
import org.junit.Before;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import static org.elasticsearch.client.Requests.putMappingRequest;
import static org.elasticsearch.common.io.Streams.copyToString;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertNoFailures;
import static org.hamcrest.Matchers.*;

@ESIntegTestCase.ClusterScope(scope = ESIntegTestCase.Scope.SUITE,numDataNodes=1)
public class ImageIntegrationTests extends ESIntegTestCase {

    private final static String INDEX_NAME = "test";
    private final static String DOC_TYPE_NAME = "test";


    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .build();
    }

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return pluginList(ImagePlugin.class);
    }

    @Before
    public void createEmptyIndex() throws Exception {
        logger.info("creating index [{}]", INDEX_NAME);
        createIndex(INDEX_NAME);
        ensureGreen();
    }

    @Override
    public Settings indexSettings() {
        return Settings.builder()
                .put("index.number_of_replicas", 0)
                .put("index.number_of_shards", 5)
                .put("index.image.use_thread_pool", randomBoolean())
                .build();
    }

    @Test
    public void test_index_search_image() throws Exception {
        String mapping = copyToStringFromClasspath("/mapping/test-mapping.json");
        client().admin().indices().putMapping(putMappingRequest(INDEX_NAME).type(DOC_TYPE_NAME).source(mapping)).actionGet();

        int totalImages = randomIntBetween(10, 50);

        // generate random images and index
        String nameToSearch = null;
        byte[] imgToSearch = null;
        String idToSearch = null;
        for (int i = 0; i < totalImages; i ++) {
            byte[] imageByte = getRandomImage();
            String name = randomAsciiOfLength(5);
            IndexResponse response = index(INDEX_NAME, DOC_TYPE_NAME, jsonBuilder().startObject().field("img", imageByte).field("name", name).endObject());
            if (nameToSearch == null || imgToSearch == null || idToSearch == null) {
                nameToSearch = name;
                imgToSearch = imageByte;
                idToSearch = response.getId();
            }
        }

        refresh();

        // test search with hash
        ImageQueryBuilder imageQueryBuilder = new ImageQueryBuilder("img").feature(GlobalFeatureEnum.CEDD.name()).image(imgToSearch).hash(GlobalDocumentBuilder.HashingMode.BitSampling.name());
        SearchResponse searchResponse = client().prepareSearch(INDEX_NAME)
                .setTypes(DOC_TYPE_NAME)
                .setQuery(imageQueryBuilder)
                .setSize(totalImages)
                .get();

        assertNoFailures(searchResponse);
        SearchHits hits = searchResponse.getHits();
    }

    private void assertImageScore(SearchHits hits, String name, float score) {
        for (SearchHit hit : hits) {
            if ((hit.getSource() != null && hit.getSource().get("name").equals(name))
                    || (hit.getFields() != null && !hit.getFields().isEmpty() && hit.getFields().get("name").getValue().equals(name))){
                assertThat(hit.getScore(), equalTo(score));
                return;
            }
        }
        throw new AssertionError("Image " + name + " not found");
    }

    private byte[] getRandomImage() throws IOException, ImageWriteException {
        int width = randomIntBetween(100, 1000);
        int height = randomIntBetween(100, 1000);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int j = 0; j < width; j ++) {
            for (int k = 0; k < height; k ++) {
                image.setRGB(j, k, randomInt(512));
            }
        }
        ImageFormat format = ImageFormat.IMAGE_FORMAT_TIFF;
        return Sanselan.writeImageToBytes(image, format, Maps.newHashMap());
    }

    public String copyToStringFromClasspath(String path) throws IOException {
        return copyToString(new InputStreamReader(getClass().getResource(path).openStream(), "UTF-8"));
    }
}
