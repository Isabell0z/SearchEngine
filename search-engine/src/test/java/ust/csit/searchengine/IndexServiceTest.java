package ust.csit.searchengine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ust.csit.searchengine.service.IndexService;

import javax.annotation.Resource;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class IndexServiceTest {


    @Resource
    private IndexService indexService;

    @Test
    public void testBuildRevertedIndex() {


        indexService.buildRevertedIndex();

    }



}
