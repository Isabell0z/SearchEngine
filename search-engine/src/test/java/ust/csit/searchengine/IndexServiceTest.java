package ust.csit.searchengine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ust.csit.searchengine.dao.EsClient;
import ust.csit.searchengine.entity.WebPage;
import ust.csit.searchengine.service.IndexService;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class IndexServiceTest {


    @Resource
    private IndexService indexService;

    @Resource
    private EsClient esClient;

    @Test
    public void testBuildRevertedIndex() {


        indexService.buildRevertedIndex();

    }

    @Test
    public void testCreateIndex() {
        try {
            esClient.createIndex("webpages");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testIndex() {
        try {
            List<WebPage> webpages = esClient.search("webpages",
                    0,           // 起始位置
                    100,     // 每批大小
                    WebPage.class);
            for (WebPage webpage : webpages) {
                System.out.println(webpage);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



}
