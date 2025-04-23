package ust.csit.searchengine.entity;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MetaDoc {

    private Integer pageId;

    private String url;

    private String title;

    private String lastModifyTime;

    private Long size;

    private Integer maxTf;

    private List<Integer> parentLinks;

    private List<Integer> childLinks;

    private Double pageRank;

    public Integer getPageId() {
        return pageId;
    }

    public void setPageId(Integer pageId) {
        this.pageId = pageId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLastModifyTime() {
        return lastModifyTime;
    }

    public void setLastModifyTime(String lastModifyTime) {
        this.lastModifyTime = lastModifyTime;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Integer getMaxTf() {
        return maxTf;
    }

    public void setMaxTf(Integer maxTf) {
        this.maxTf = maxTf;
    }

    public List<Integer> getParentLinks() {
        return parentLinks;
    }

    public void setParentLinks(List<Integer> parentLinks) {
        this.parentLinks = parentLinks;
    }

    public List<Integer> getChildLinks() {
        return childLinks;
    }

    public void setChildLinks(List<Integer> childLinks) {
        this.childLinks = childLinks;
    }

    public Double getPageRank() {
        return pageRank;
    }

    public void setPageRank(Double pageRank) {
        this.pageRank = pageRank;
    }
}
