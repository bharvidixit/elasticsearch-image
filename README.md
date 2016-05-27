Image Plugin for Elasticsearch
==================================

[![Build Status](https://travis-ci.org/zengde/elasticsearch-image.png?branch=dev)](https://travis-ci.org/zengde/elasticsearch-image)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg?maxAge=2592000)](http://www.apache.org/licenses/LICENSE-2.0)
[![GitHub release](https://img.shields.io/github/release/zengde/elasticsearch-image.svg)](https://github.com/zengde/elasticsearch-image/releases)

The Image Plugin is an Content Based Image Retrieval Plugin for Elasticsearch using [LIRE (Lucene Image Retrieval)](https://github.com/dermotte/LIRE/). It allows users to index images and search for similar images.

It adds an `image` field type and an `image` query

In order to install the plugin, simply run: `bin\plugin install zengde/elasticsearch-image`.

|     Image Plugin          |  elasticsearch    | Release date |
|---------------------------|-------------------|:------------:|
| 2.2.0dev                  | 2.2.0             | 2016-04-21   |
| 2.1.1dev                  | 2.1.1             | 2016-04-03   |
| 2.1.1                     | 2.1.1             | 2016-02-23   |
| 1.3.0-SNAPSHOT (master)   | 1.1.0             |              |
| 1.2.0                     | 1.0.1             | 2014-03-20   |
| 1.1.0                     | 1.0.1             | 2014-03-13   |
| 1.0.0                     | 1.0.1             | 2014-03-05   |


## Example
#### Create Mapping
```sh
curl -XPUT 'localhost:9200/test/test/_mapping' -d '{
    "test": {
		"_source": {
            "excludes": ["my_img"]
        },
        "properties": {
            "my_img": {
                "type": "image",
                "feature": ["CEDD","JCD","FCTH"],
                "hash":"BitSampling",
                "store":false
            },
            "name": {
                "type": "string",
                "index": "not_analyzed"
            }
        }
    }
}'
```
`type` should be `image`. **Mandatory**

`feature` is a map of features for index. **Mandatory, at least one is required**

`hash` can be set if you want to search on hash. **Optional**

#### Index Image
```sh
curl -XPOST 'localhost:9200/test/test' -d '{
    "my_img": "... base64 encoded image ..."
}'
```

#### Search Image
```sh
curl -XPOST 'localhost:9200/test/test/_search' -d '{
    "query": {
        "image": {
            "my_img": {
                "feature": "CEDD",
                "image": "... base64 encoded image to search ...",
                "hash": "BIT_SAMPLING"
            }
        }
    }
}'
```
`feature` should be one of the features in the mapping.  **Mandatory**

`image` base64 of image to search.  **Optional if search using existing image**

`hash` should be same to the hash set in mapping.  **Optional**

`boost` score boost  **Optional**


## Supported Features
####Global Features:
[`SIMPLE_CENTRIST`](https://github.com/dermotte/LIRE/blob/master/src/main/java/net/semanticmetadata/lire/imageanalysis/features/global/centrist/SimpleCentrist.java), [`SPATIAL_PYRAMID_CENTRIST`](https://github.com/dermotte/LIRE/blob/master/src/main/java/net/semanticmetadata/lire/imageanalysis/features/global/centrist/SpatialPyramidCentrist.java), [`JOINT_HISTOGRAM`](https://github.com/dermotte/LIRE/blob/master/src/main/java/net/semanticmetadata/lire/imageanalysis/features/global/joint/JointHistogram.java), [`LOCAL_BINARY_PATTERNS_AND_OPPONENT`](https://github.com/dermotte/LIRE/blob/master/src/main/java/net/semanticmetadata/lire/imageanalysis/features/global/joint/LocalBinaryPatternsAndOpponent.java), [`RANK_AND_OPPONENT`](https://github.com/dermotte/LIRE/blob/master/src/main/java/net/semanticmetadata/lire/imageanalysis/features/global/joint/RankAndOpponent.java), [`SPACC`](https://github.com/dermotte/LIRE/blob/master/src/main/java/net/semanticmetadata/lire/imageanalysis/features/global/spatialpyramid/SPACC.java), [`SPCEDD`](https://github.com/dermotte/LIRE/blob/master/src/main/java/net/semanticmetadata/lire/imageanalysis/features/global/spatialpyramid/SPCEDD.java), [`SPFCTH`](https://github.com/dermotte/LIRE/blob/master/src/main/java/net/semanticmetadata/lire/imageanalysis/features/global/spatialpyramid/SPFCTH.java), [`SPJCD`](https://github.com/dermotte/LIRE/blob/master/src/main/java/net/semanticmetadata/lire/imageanalysis/features/global/spatialpyramid/SPJCD.java), [`SPLBP`](https://github.com/dermotte/LIRE/blob/master/src/main/java/net/semanticmetadata/lire/imageanalysis/features/global/spatialpyramid/SPLBP.java), [`AUTO_COLOR_CORRELOGRAM`](https://github.com/dermotte/LIRE/blob/master/src/main/java/net/semanticmetadata/lire/imageanalysis/features/global/AutoColorCorrelogram.java), [`BINARY_PATTERNS_PYRAMID`](https://github.com/dermotte/LIRE/blob/master/src/main/java/net/semanticmetadata/lire/imageanalysis/features/global/BinaryPatternsPyramid.java), [`CEDD`](https://github.com/dermotte/LIRE/blob/master/src/main/java/net/semanticmetadata/lire/imageanalysis/features/global/CEDD.java), [`COLOR_LAYOUT`](https://github.com/dermotte/LIRE/blob/master/src/main/java/net/semanticmetadata/lire/imageanalysis/features/global/ColorLayout.java), [`EDGE_HISTOGRAM`](https://github.com/dermotte/LIRE/blob/master/src/main/java/net/semanticmetadata/lire/imageanalysis/features/global/EdgeHistogram.java), [`FCTH`](https://github.com/dermotte/LIRE/blob/master/src/main/java/net/semanticmetadata/lire/imageanalysis/features/global/FCTH.java), [`FUZZY_COLOR_HISTOGRAM`](https://github.com/dermotte/LIRE/blob/master/src/main/java/net/semanticmetadata/lire/imageanalysis/features/global/FuzzyColorHistogram.java), [`FUZZY_OPPONENT_HISTOGRAM`](https://github.com/dermotte/LIRE/blob/master/src/main/java/net/semanticmetadata/lire/imageanalysis/features/global/FuzzyOpponentHistogram.java), [`GABOR`](https://github.com/dermotte/LIRE/blob/master/src/main/java/net/semanticmetadata/lire/imageanalysis/features/global/Gabor.java), [`JCD`](https://github.com/dermotte/LIRE/blob/master/src/main/java/net/semanticmetadata/lire/imageanalysis/features/global/JCD.java), [`JPEG_COEFFICIENT_HISTOGRAM`](https://github.com/dermotte/LIRE/blob/master/src/main/java/net/semanticmetadata/lire/imageanalysis/features/global/JpegCoefficientHistogram.java), [`LOCAL_BINARY_PATTERNS`](https://github.com/dermotte/LIRE/blob/master/src/main/java/net/semanticmetadata/lire/imageanalysis/features/global/LocalBinaryPatterns.java), [`LUMINANCE_LAYOUT`](https://github.com/dermotte/LIRE/blob/master/src/main/java/net/semanticmetadata/lire/imageanalysis/features/global/LuminanceLayout.java), [`OPPONENT_HISTOGRAM`](https://github.com/dermotte/LIRE/blob/master/src/main/java/net/semanticmetadata/lire/imageanalysis/features/global/OpponentHistogram.java), [`PHOG`](https://github.com/dermotte/LIRE/blob/master/src/main/java/net/semanticmetadata/lire/imageanalysis/features/global/PHOG.java), [`ROTATION_INVARIANT_LOCAL_BINARY_PATTERNS`](https://github.com/dermotte/LIRE/blob/master/src/main/java/net/semanticmetadata/lire/imageanalysis/features/global/RotationInvariantLocalBinaryPatterns.java), [`SCALABLE_COLOR`](https://github.com/dermotte/LIRE/blob/master/src/main/java/net/semanticmetadata/lire/imageanalysis/features/global/ScalableColor.java), [`SIMPLE_COLOR_HISTOGRAM`](https://github.com/dermotte/LIRE/blob/master/src/main/java/net/semanticmetadata/lire/imageanalysis/features/global/SimpleColorHistogram.java), [`TAMURA`](https://github.com/dermotte/LIRE/blob/master/src/main/java/net/semanticmetadata/lire/imageanalysis/features/global/Tamura.java)


### Supported Hash Mode
[`BitSampling`](https://github.com/dermotte/LIRE/blob/master/src/main/java/net/semanticmetadata/lire/indexers/hashing/BitSampling.java), [`LSH`](https://github.com/dermotte/LIRE/blob/master/src/main/java/net/semanticmetadata/lire/indexers/hashing/LocalitySensitiveHashing.java), [`MetricSpaces`](https://github.com/dermotte/LIRE/blob/master/src/main/java/net/semanticmetadata/lire/indexers/hashing/MetricSpaces.java)

Hash will increase search speed with large data sets

See [Large image data sets with LIRE ?some new numbers](http://www.semanticmetadata.net/2013/03/20/large-image-data-sets-with-lire-some-new-numbers/) 


## ChangeLog
#### 2.1.1dev (2016-04-03)
- change mapping and search rest format and ralated code
- change indexed image document structure
- upgrade to LIRE1.0b2 , new Hash mode `MetricSpaces` and some Features
- change buld tools to gradle
- remove redundant files

#### 2.1.1 (2016-02-23)
- support es 2.1.1


#### 1.2.0 (2014-03-20)

- Use multi-thread when multiple features are required to improve index speed
- Allow index metadata
- Allow query by existing image in index

#### 1.1.0 (2014-03-13)

- Added `limit` in `image` query
- Added plugin version in es-plugin.properties

#### 1.0.0 (2014-03-05)

- initial release