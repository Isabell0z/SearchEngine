package ust.csit.searchengine.entity;

import java.util.Map;


public class BodyInfo {

    // docId : tf
    private Map<Integer, Integer> bodyDocMap;

//    private Integer docId;
//
//    // 词频
//    private Integer tf;

    // 位置信息
//    private List<Integer> positions;


    public Map<Integer, Integer> getBodyDocMap() {
        return bodyDocMap;
    }

    public void setBodyDocMap(Map<Integer, Integer> bodyDocMap) {
        this.bodyDocMap = bodyDocMap;
    }
}
