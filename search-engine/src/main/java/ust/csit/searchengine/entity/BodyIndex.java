package ust.csit.searchengine.entity;

import java.util.Map;

public class BodyIndex {

    private String term;

    private Map<Integer, Integer> bodyDocMap;


    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }


    public Map<Integer, Integer> getBodyDocMap() {
        return bodyDocMap;
    }

    public void setBodyDocMap(Map<Integer, Integer> bodyDocMap) {
        this.bodyDocMap = bodyDocMap;
    }
}
