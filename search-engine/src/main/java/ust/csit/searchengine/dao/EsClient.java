package ust.csit.searchengine.dao;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class EsClient {

    @Value("${elasticsearch.host}")
    private String host;

    @Value("${elasticsearch.port}")
    private int port;

    private ElasticsearchClient client;

    @PostConstruct
    public void init() {
        RestClient restClient = RestClient.builder(new HttpHost(host, port)).build();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        this.client = new ElasticsearchClient(transport);
    }

    /**
     * 创建索引
     * @param indexName
     * @throws IOException
     */
    public void createIndex(String indexName) throws IOException {
        // 检查索引是否存在
        boolean exists = client.indices().exists(e -> e.index(indexName)).value();
        if (exists) {
            return;
        }
        client.indices().create(c -> c.index(indexName));
    }


    // 单条写入
    public <T> String index(String index, T document) throws IOException {
        IndexResponse response = client.index(i -> i
                .index(index)
                .document(document)
        );
        return response.id();
    }

    // 批量写入
    public <T> void bulkIndex(String indexName, Map<String, T> dataMap) throws IOException {
        BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();

        for (Map.Entry<String, T> entry : dataMap.entrySet()) {
            String term = entry.getKey();
            T info = entry.getValue();

            // 构建索引请求
            bulkBuilder.operations(op -> op
                    .index(idx -> idx
                            .index(indexName)
                            .id(term)  // 使用 term 作为文档 ID
                            .document(info)
                    )
            );
        }

        // 执行批量写入
        BulkResponse response = client.bulk(bulkBuilder.build());

        // 检查是否有错误
        if (response.errors()) {
            handleBulkError(response);
        }
    }

    private void handleBulkError(BulkResponse response) {
        // 处理批量写入错误
        StringBuilder errorMessage = new StringBuilder("Bulk indexing failed:\n");
        for (BulkResponseItem item : response.items()) {
            if (item.error() != null) {
                errorMessage.append(String.format("ID: %s, Error: %s\n",
                        item.id(), item.error().reason()));
            }
        }
        throw new RuntimeException(errorMessage.toString());
    }

    // 批量写入
    public <T> BulkResponse bulkIndex(String index, List<T> documents) throws IOException {
        List<BulkOperation> operations = new ArrayList<>();
        for (T doc : documents) {
            operations.add(BulkOperation.of(o -> o
                    .index(i -> i
                            .document(doc)
                            .index(index)
                    )
            ));
        }

        return client.bulk(b -> b
                .operations(operations)
        );
    }

    // 单条查询
    public <T> T get(String index, String id, Class<T> clazz) throws IOException {
        GetResponse<T> response = client.get(g -> g
                        .index(index)
                        .id(id),
                clazz
        );
        return response.source();
    }

    // 批量查询
    public <T> List<T> search(String index, String queryString, int from, int size, Class<T> clazz) throws IOException {
        SearchResponse<T> response = client.search(s -> s
                        .index(index)
                        .query(q -> q
                                .match(m -> m
                                        .field("content")
                                        .query(FieldValue.of(queryString))
                                )
                        )
                        .from(from)
                        .size(size),
                clazz
        );

        List<T> results = new ArrayList<>();
        response.hits().hits().forEach(hit -> results.add(hit.source()));
        return results;
    }
}
