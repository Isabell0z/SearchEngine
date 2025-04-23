package ust.csit.searchengine.entity;

import java.util.Map;

public class TitleIndex {

    private String term;

    private Map<Integer, Integer> titleDocMap;

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public Map<Integer, Integer> getTitleDocMap() {
        return titleDocMap;
    }

    public void setTitleDocMap(Map<Integer, Integer> titleDocMap) {
        this.titleDocMap = titleDocMap;
    }
}
