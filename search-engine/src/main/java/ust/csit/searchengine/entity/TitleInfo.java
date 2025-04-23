package ust.csit.searchengine.entity;

import java.util.Map;


public class TitleInfo {

    // docId : tf
    private Map<Integer, Integer> titleDocMap;


    public Map<Integer, Integer> getTitleDocMap() {
        return titleDocMap;
    }
    public void setTitleDocMap(Map<Integer, Integer> titleDocMap) {
        this.titleDocMap = titleDocMap;
    }

//    private Integer docId;
//
//    // 词频
//    private Integer tf;



}
