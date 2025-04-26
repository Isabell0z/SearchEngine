package ust.csit.searchengine.entity;

public class TermFreq implements Comparable<TermFreq> {
    private String term;
    private int frequency;


    public TermFreq() {
    }

    public TermFreq(String term, int frequency) {
        this.term = term;
        this.frequency = frequency;
    }

    @Override
    public int compareTo(TermFreq other) {
        return Integer.compare(this.frequency, other.frequency); // 小顶堆
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }
}
