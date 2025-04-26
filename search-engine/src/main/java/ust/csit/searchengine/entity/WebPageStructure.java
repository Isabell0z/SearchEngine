package ust.csit.searchengine.entity;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class WebPageStructure {
    private Integer parentPageId;
    private Integer childPageId;


    public Integer getParentPageId() {
        return parentPageId;
    }

    public void setParentPageId(Integer parentPageId) {
        this.parentPageId = parentPageId;
    }

    public Integer getChildPageId() {
        return childPageId;
    }

    public void setChildPageId(Integer childPageId) {
        this.childPageId = childPageId;
    }
}
