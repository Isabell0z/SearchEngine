package ust.csit.searchengine.utils;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashSet;

@Component
public class StopStemer {

    @Value("${stopwords.path:src/main/resources/static/stopwords.txt}")
    private String stopwordsPath;

    private Porter porter;
    private HashSet<String> stopWords;


    @PostConstruct
    public void init() {
        porter = new Porter();
        stopWords = new HashSet<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(stopwordsPath));
            String cur;
            while ((cur = reader.readLine()) != null) {
                stopWords.add(cur);
            }
            reader.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load stopwords", e);
        }
    }

    private boolean isStopWord(String str) {
        return stopWords.contains(str);
    }


    private String stem(String str) {
        return porter.stripAffixes(str);
    }


    public String removeStopwordAndStem(String text) {

        StringBuilder result = new StringBuilder();

        // 只保留英文字母和空格
        String cleanedText = text.replaceAll("[^a-zA-Z\\s]", " ").toLowerCase();
        String[] words = cleanedText.split("\\s+");

        for (String word : words) {
            // 忽略空字符串
            if (word.trim().isEmpty()) {
                continue;
            }

            // 如果不是停用词，进行词干提取并添加到结果中
            if (!isStopWord(word.toLowerCase())) {
                String stemmed = stem(word.toLowerCase());
                result.append(stemmed).append(" ");
            }
        }

        return result.toString().trim();
    }
}

