package ust.csit.searchengine.service;


import org.springframework.stereotype.Service;
import ust.csit.searchengine.dao.EsClient;
import ust.csit.searchengine.entity.*;
import ust.csit.searchengine.utils.StopStemer;

import javax.annotation.Resource;
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
    private static final String WEB_PAGE_INDEX = "web_page_structure";
    private static final String REVERSE_WEB_PAGE_INDEX = "reverse_web_page_structure";


    private static final String TITLE_INDEX = "title_index";
    private static final String BODY_INDEX = "body_index";

    private static final int BATCH_SIZE = 100;

    public void buildRevertedIndex() {
        int from = 0;

        // 分批获取所有文档
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

        Map<String, TitleInfo> titleMap = new HashMap<>();
        Map<String, BodyInfo> bodyMap = new HashMap<>();

        for (WebPage page : batch) {
            Integer docId = page.getPageId();

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
                    } else {
                        titleDocMap.put(docId, tf + 1);
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
                    } else {
                        bodyDocMap.put(docId, tf + 1);
                    }
                }

            }

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


        // 写入新索引
        // 写入titleMap和bodyMap
        try {
            esClient.createIndex(TITLE_INDEX);
            esClient.createIndex(BODY_INDEX);
            esClient.bulkIndex(TITLE_INDEX, titleIndexList);
            esClient.bulkIndex(BODY_INDEX, bodyIndexList);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
