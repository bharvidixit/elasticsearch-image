package org.elasticsearch.plugin.image;

import org.elasticsearch.index.mapper.image.ImageMapper;
import org.elasticsearch.index.query.image.ImageQueryParser;
import org.elasticsearch.indices.IndicesModule;
import org.elasticsearch.plugins.Plugin;

public class ImagePlugin extends Plugin {

    @Override
    public String name() {
        return "image";
    }

    @Override
    public String description() {
        return "Elasticsearch Image Plugin";
    }

    public void onModule(IndicesModule indicesModule) {
        indicesModule.registerMapper("image", new ImageMapper.TypeParser());
        indicesModule.registerQueryParser(ImageQueryParser.class);
    }
}
