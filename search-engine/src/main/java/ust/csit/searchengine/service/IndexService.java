package ust.csit.searchengine.service;


import org.springframework.stereotype.Service;
import ust.csit.searchengine.dao.EsClient;
import ust.csit.searchengine.entity.*;
import ust.csit.searchengine.utils.StopStemer;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;

@Service
public class IndexService {

    @Resource
    private EsClient esClient;

    @Resource
    private StopStemer stopStemer;

    @Resource
    private PageRankService pageRankService;

    private static final String SOURCE_INDEX = "web_pages";
    private static final String LINK_INDEX = "web_page_structure";
    private static final String REVERSE_LINK_INDEX = "reverse_web_page_structure";


    private static final String TITLE_INDEX = "title_index";
    private static final String BODY_INDEX = "body_index";
    private static final String META_DOC_INDEX = "meta_doc";

    private static final int BATCH_SIZE = 500;


    private final Map<String, TitleInfo> titleMap = new HashMap<>();
    private final Map<String, BodyInfo> bodyMap = new HashMap<>();

    private final Map<Integer, List<Integer>> parentMap = new HashMap<>();  // pageId -> parentLinks
    private final Map<Integer, List<Integer>> childMap = new HashMap<>();   // pageId -> childLinks

    public void buildRevertedIndex() {
        try {
            // 获取父子链接
            getLinks();

            // 分批获取所有文档
            getAllDocuments();

            // 计算 PageRank
            pageRankService.calculatePageRank();

            // 写入索引
            writeInvertedIndexToEs();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void getLinks() {
        int from = 0;
        while (true) {
            List<WebPageStructure> structures = null;
            try {
                structures = esClient.search(
                        LINK_INDEX,
                        from,
                        BATCH_SIZE,
                        WebPageStructure.class
                );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (structures.isEmpty()) {
                break;
            }

            // 构建父子关系映射
            for (WebPageStructure structure : structures) {
                int parentId = structure.getParentPageId();
                int childId = structure.getChildPageId();

                // 添加子链接
                childMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(childId);

                // 添加父链接
                parentMap.computeIfAbsent(childId, k -> new ArrayList<>()).add(parentId);
            }

            from += BATCH_SIZE;
        }
    }

    private void getAllDocuments() {
        int from = 0;
        while (true) {
            List<WebPage> batch = null;
            try {
                batch = esClient.search(SOURCE_INDEX,
                        from,           // 起始位置
                        BATCH_SIZE,     // 每批大小
                        WebPage.class
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            if (batch.isEmpty()) {
                break;
            }

            processBatch(batch);
            from += BATCH_SIZE;
        }
    }

    private void processBatch(List<WebPage> batch) {

        List<MetaDoc> metaDocs = new ArrayList<>();

        for (WebPage page : batch) {

            Integer docId = page.getPageId();
            int maxTf = 0;

            // 处理标题
            String processedTitle = stopStemer.removeStopwordAndStem(page.getTitle());
            String[] titleWords = processedTitle.split("\\s+");
            for (int i = 0; i < titleWords.length; i++) {
                String term = titleWords[i];
                if (term.isEmpty()) {
                    continue;
                }
                TitleInfo titleInfo = titleMap.get(term);
                // term not exist
                if (titleInfo == null) {
                    titleInfo = new TitleInfo();
                    Map<Integer, Integer> titleDocMap = new HashMap<>();
                    titleDocMap.put(docId, 1);
                    titleInfo.setTitleDocMap(titleDocMap);
                    titleMap.put(term, titleInfo);
                } else {
                    // term exist
                    Map<Integer, Integer> titleDocMap = titleInfo.getTitleDocMap();
                    Integer tf = titleDocMap.get(docId);
                    if (tf == null) {
                        titleDocMap.put(docId, 1);
                        maxTf = Math.max(maxTf, 1);
                    } else {
                        titleDocMap.put(docId, tf + 1);
                        maxTf = Math.max(maxTf, tf + 1);
                    }
                }

                // 处理 bi-gram
                if (i < titleWords.length - 1) {
                    String nextTerm = titleWords[i + 1];
                    if (!nextTerm.isEmpty()) {
                        String biGramTerm = term + "_" + nextTerm;
                        TitleInfo biTitleInfo = titleMap.get(biGramTerm);
                        // term not exist
                        if (biTitleInfo == null) {
                            biTitleInfo = new TitleInfo();
                            Map<Integer, Integer> titleDocMap = new HashMap<>();
                            titleDocMap.put(docId, 1);
                            biTitleInfo.setTitleDocMap(titleDocMap);
                            titleMap.put(biGramTerm, biTitleInfo);
                        } else {
                            // term exist
                            Map<Integer, Integer> titleDocMap = biTitleInfo.getTitleDocMap();
                            Integer tf = titleDocMap.get(docId);
                            if (tf == null) {
                                titleDocMap.put(docId, 1);
                            } else {
                                titleDocMap.put(docId, tf + 1);
                            }
                        }
                    }
                }

            }


            // 处理正文
            PriorityQueue<TermFreq> priorityQueue = new PriorityQueue<>(10); // 小顶堆保存top10
            Map<String, Integer> termFreqMap = new HashMap<>(); // 临时统计当前文档term频率
            String processedBody = stopStemer.removeStopwordAndStem(page.getContent());
            String[] bodyWords = processedBody.split("\\s+");
            for (int i = 0; i < bodyWords.length; i++) {
                String term = bodyWords[i];
                if (term.isEmpty()) {
                    continue;
                }

                // 更新词频统计
                termFreqMap.merge(term, 1, Integer::sum);

                BodyInfo bodyInfo = bodyMap.get(term);
                // term not exist
                if (bodyInfo == null) {
                    bodyInfo = new BodyInfo();
                    Map<Integer, Integer> bodyDocMap = new HashMap<>();
                    bodyDocMap.put(docId, 1);
                    bodyInfo.setBodyDocMap(bodyDocMap);
                    bodyMap.put(term, bodyInfo);
                } else {
                    // term exist
                    Map<Integer, Integer> bodyDocMap = bodyInfo.getBodyDocMap();
                    Integer tf = bodyDocMap.get(docId);
                    if (tf == null) {
                        bodyDocMap.put(docId, 1);
                        maxTf = Math.max(maxTf, 1);
                    } else {
                        bodyDocMap.put(docId, tf + 1);
                        maxTf = Math.max(maxTf, tf + 1);
                    }
                }

                // 处理 bi-gram
                if (i < bodyWords.length - 1) {
                    String nextTerm = bodyWords[i + 1];
                    if (!nextTerm.isEmpty()) {
                        String biGramTerm = term + "_" + nextTerm;
                        BodyInfo biBodyInfo = bodyMap.get(biGramTerm);
                        // term not exist
                        if (biBodyInfo == null) {
                            biBodyInfo = new BodyInfo();
                            Map<Integer, Integer> bodyDocMap = new HashMap<>();
                            bodyDocMap.put(docId, 1);
                            biBodyInfo.setBodyDocMap(bodyDocMap);
                            bodyMap.put(biGramTerm, biBodyInfo);
                        } else {
                            // term exist
                            Map<Integer, Integer> bodyDocMap = biBodyInfo.getBodyDocMap();
                            Integer tf = bodyDocMap.get(docId);
                            if (tf == null) {
                                bodyDocMap.put(docId, 1);
                            } else {
                                bodyDocMap.put(docId, tf + 1);
                            }
                        }
                    }
                }
            }
            termFreqMap.forEach((t, freq) -> {
                if (priorityQueue.size() < 10) {
                    priorityQueue.offer(new TermFreq(t, freq));
                } else if (freq > priorityQueue.peek().getFrequency()) {
                    priorityQueue.poll();
                    priorityQueue.offer(new TermFreq(t, freq));
                }
            });
            // 取出 top 10 词
            List<TermFreq> top10Terms = new ArrayList<>();
            while (!priorityQueue.isEmpty()) {
                top10Terms.add(0, priorityQueue.poll()); // 从头部插入，实现降序
            }


            // 处理metaData
            MetaDoc metaDoc = createMetaDoc(page, maxTf, bodyWords.length, top10Terms);
            metaDocs.add(metaDoc);

        }
        // 批量写入 MetaDoc
        try {
            esClient.createIndex(META_DOC_INDEX);
            esClient.bulkIndexMetaDoc(META_DOC_INDEX, metaDocs);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private MetaDoc createMetaDoc(WebPage page, int maxTf, int length, List<TermFreq> top10Terms) {
        MetaDoc metaDoc = new MetaDoc();
        metaDoc.setPageId(page.getPageId());
        metaDoc.setUrl(page.getUrl());
        metaDoc.setTitle(page.getTitle());
        metaDoc.setLastModifyTime(page.getLastModifyTime());
        metaDoc.setSize(page.getSize());
        metaDoc.setMaxTf(maxTf);
        metaDoc.setParentLinks(parentMap.get(page.getPageId()));
        metaDoc.setChildLinks(childMap.get(page.getPageId()));
        metaDoc.setLength(length);
        metaDoc.setTermFreqList(new ArrayList<>(top10Terms));
        return metaDoc;
    }

    private void writeInvertedIndexToEs() throws IOException {
        // 转换并写入
        List<TitleIndex> titleIndexList = convertTitleMap();
        List<BodyIndex> bodyIndexList = convertBodyMap();

        esClient.createIndex(TITLE_INDEX);
        esClient.createIndex(BODY_INDEX);
        esClient.bulkIndex(TITLE_INDEX, titleIndexList);
        esClient.bulkIndex(BODY_INDEX, bodyIndexList);

        // 清理资源
        titleMap.clear();
        bodyMap.clear();
    }

    private List<TitleIndex> convertTitleMap() {
        List<TitleIndex> titleIndexList = new ArrayList<>();
        for (Map.Entry<String, TitleInfo> entry : titleMap.entrySet()) {
            TitleIndex titleIndex = new TitleIndex();
            titleIndex.setTerm(entry.getKey());
            titleIndex.setTitleDocMap(entry.getValue().getTitleDocMap());
            titleIndexList.add(titleIndex);
        }
        return titleIndexList;
    }

    private List<BodyIndex> convertBodyMap() {
        List<BodyIndex> bodyIndexList = new ArrayList<>();
        for (Map.Entry<String, BodyInfo> entry : bodyMap.entrySet()) {
            BodyIndex bodyIndex = new BodyIndex();
            bodyIndex.setTerm(entry.getKey());
            bodyIndex.setBodyDocMap(entry.getValue().getBodyDocMap());
            bodyIndexList.add(bodyIndex);
        }
        return bodyIndexList;
    }


}
