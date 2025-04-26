package ust.csit.searchengine.service;

import org.springframework.stereotype.Service;
import ust.csit.searchengine.dao.EsClient;
import ust.csit.searchengine.entity.MetaDoc;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Thread.sleep;

@Service
public class PageRankService {
    private static final String META_DOC_INDEX = "meta_doc";

    private static final double DAMPING_FACTOR = 0.85; // 阻尼系数
    private static final double EPSILON = 1e-8;        // 收敛阈值
    private static final int MAX_ITERATIONS = 100;     // 最大迭代次数

    @Resource
    private EsClient esClient;

    public void calculatePageRank() throws Exception {
        // 1. 获取所有文档
        List<MetaDoc> docs = getAllDocs();
        int n = docs.size();

        // 2. 初始化 PageRank 值
        Map<Integer, Double> pageRanks = new HashMap<>();
        Map<Integer, MetaDoc> docMap = new HashMap<>();

        for (MetaDoc doc : docs) {
            pageRanks.put(doc.getPageId(), 1.0 / n);
            docMap.put(doc.getPageId(), doc);
        }

        // 3. 迭代计算
        boolean converged = false;
        int iteration = 0;

        while (!converged && iteration < MAX_ITERATIONS) {
            Map<Integer, Double> newPageRanks = new HashMap<>();
            double diff = 0.0;

            for (MetaDoc doc : docs) {
                double sum = 0.0;
                // 获取所有指向当前页面的文档（父链接）
                List<Integer> parentLinks = doc.getParentLinks();

                if (parentLinks != null) {
                    for (Integer parentId : parentLinks) {
                        MetaDoc parentDoc = docMap.get(parentId);
                        if (parentDoc != null && parentDoc.getChildLinks() != null) {
                            // 计算转移概率
                            sum += pageRanks.get(parentId) / parentDoc.getChildLinks().size();
                        }
                    }
                }

                // 计算新的 PageRank 值
                double newPR = (1 - DAMPING_FACTOR) / n + DAMPING_FACTOR * sum;
                newPageRanks.put(doc.getPageId(), newPR);

                // 计算与上一次迭代的差值
                diff += Math.abs(newPR - pageRanks.get(doc.getPageId()));
            }

            // 更新 PageRank 值
            pageRanks = newPageRanks;

            // 检查是否收敛
            converged = diff < EPSILON;
            iteration++;
        }

        // 4. 更新文档的 PageRank 值
        Map<String, Map<String, Object>> updates = new HashMap<>();
        for (MetaDoc doc : docs) {
            Map<String, Object> fields = new HashMap<>();
            fields.put("page_rank", pageRanks.get(doc.getPageId()));
            updates.put(String.valueOf(doc.getPageId()), fields);

        }

        // 5. 批量更新 ES
        esClient.bulkPartialUpdate(META_DOC_INDEX, updates);


    }


    private List<MetaDoc> getAllDocs() throws Exception {
        sleep(1000); // 等待索引创建完成
        int from = 0;
        int size = 500;
        List<MetaDoc> allDocs = new ArrayList<>();

        while (true) {
            List<MetaDoc> batch = esClient.search(META_DOC_INDEX, from, size, MetaDoc.class);
            if (batch.isEmpty()) {
                break;
            }
            allDocs.addAll(batch);
            from += size;
        }

        return allDocs;
    }
}
