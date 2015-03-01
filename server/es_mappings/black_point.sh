curl -XDELETE "http://localhost:9200/srp-point"
curl -XPOST "http://localhost:9200/srp-point" -d '{
    settings: {
        index: {
            analysis: {
                filter: {
                    substring: {
                        type: "nGram",
                        min_gram: 1,
                        max_gram: 20
                    }
                },
                analyzer: {
                    str_index_analyzer: {
                        filter: [
                            "lowercase", "substring"
                        ],
                        tokenizer: "keyword"
                    },
                    str_search_analyzer: {
                        filter: [
                            "lowercase"
                        ],
                        tokenizer: "keyword"
                    }
                }
            },
            number_of_shards: 5,
            uuid: "ihIPT3QDQluQOzBTpKVijw",
            version: {
                created: 1030199
            },
            number_of_replicas: 1
        }
    },
    "mappings" : {
        "point":{
            "properties":{
                "rate":{
                    "type":"integer"
                },
                "road":{
                    "type":"string",
                    search_analyzer: "str_search_analyzer",
                    index_analyzer: "str_index_analyzer"
                },
                "km":{
                    "type":"string",
                    "index":"not_analyzed"
                },
                "location":{
                    "type":"geo_point"
                }
            }
        }
    }
}'