package ust.csit.searchengine.service;


import org.springframework.stereotype.Service;
import ust.csit.searchengine.dao.EsClient;
import ust.csit.searchengine.entity.*;
import ust.csit.searchengine.utils.StopStemer;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class IndexService {

    @Resource
    private EsClient esClient;

    @Resource
    private StopStemer stopStemer;

    private static final String SOURCE_INDEX = "web_pages";
    private static final String LINK_INDEX = "web_page_structure";
    private static final String REVERSE_LINK_INDEX = "reverse_web_page_structure";


    private static final String TITLE_INDEX = "title_index";
    private static final String BODY_INDEX = "body_index";
    private static final String META_DOC_INDEX = "meta_doc";

    private static final int BATCH_SIZE = 100;


    private final Map<String, TitleInfo> titleMap = new HashMap<>();
    private final Map<String, BodyInfo> bodyMap = new HashMap<>();

    private final Map<Integer, List<Integer>> parentMap = new HashMap<>();  // pageId -> parentLinks
    private final Map<Integer, List<Integer>> childMap = new HashMap<>();   // pageId -> childLinks

    public void buildRevertedIndex() {

        // 获取父子链接
        getLinks();

        // 分批获取所有文档
        getAllDocuments();

        try {
            // 写入索引
            writeInvertedIndexToEs();
        } catch (IOException e) {
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

            }


            // 处理正文
            String processedBody = stopStemer.removeStopwordAndStem(page.getContent());
            String[] bodyWords = processedBody.split("\\s+");
            for (int i = 0; i < bodyWords.length; i++) {
                String term = bodyWords[i];
                if (term.isEmpty()) {
                    continue;
                }
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
            }
            // 处理metaData
            processMetaData(page, maxTf);

        }


        List<TitleIndex> titleIndexList = new ArrayList<>();
        for (Map.Entry<String, TitleInfo> entry : titleMap.entrySet()) {
            TitleIndex titleIndex = new TitleIndex();
            titleIndex.setTerm(entry.getKey());
            titleIndex.setTitleDocMap(entry.getValue().getTitleDocMap());
            titleIndexList.add(titleIndex);
        }

        List<BodyIndex> bodyIndexList = new ArrayList<>();
        for (Map.Entry<String, BodyInfo> entry : bodyMap.entrySet()) {
            BodyIndex bodyIndex = new BodyIndex();
            bodyIndex.setTerm(entry.getKey());
            bodyIndex.setBodyDocMap(entry.getValue().getBodyDocMap());
            bodyIndexList.add(bodyIndex);
        }
    }

    private void processMetaData(WebPage page, int maxTf) {
        // 处理元数据
        List<MetaDoc> metaDocs = new ArrayList<>();
        MetaDoc metaDoc = new MetaDoc();
        metaDoc.setPageId(page.getPageId());
        metaDoc.setUrl(page.getUrl());
        metaDoc.setTitle(page.getTitle());
        metaDoc.setLastModifyTime(page.getLastModifyTime());
        metaDoc.setSize(page.getSize());
        metaDoc.setMaxTf(maxTf);
        metaDoc.setParentLinks(parentMap.get(page.getPageId()));
        metaDoc.setChildLinks(childMap.get(page.getPageId()));

        metaDocs.add(metaDoc);
        // 批量写入ES
        try {
            esClient.createIndex(META_DOC_INDEX);
            esClient.bulkIndex(META_DOC_INDEX, metaDocs);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
