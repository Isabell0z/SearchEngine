# Search Engine

## Components
### Crawler
- Collect web pages starting from seed URL (https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm) and parses text content
### Indexer
- Built with SpringBoot
- Build inverted index (title & body) and stores metadata for fast retrieval.
### Retrieval Function
- Built with Flask
- Based on the Vector Space Model (TF-IDF + Cosine Similarity).
- Supports:
  - Phrase search (e.g., "hong kong")
  - Keyword search
### Web Interface
- Built with Vue 3 + Element Plus.
- Main features:
  - Search box with autocomplete
  - Sidebar keyword filtering
  - Dual-mode sorting (by Relevance or Authority)
  - User login and search history tracking
  - Automatically detect and correct minor typos in search queries (e.g., "moive" -> "movie")

## Requirements

- Python 3

- Java 17

- SpringBoot 2.6

- ElasticSearch 7.0

- MySQL 8.0

## Setup
### Crawler
- Start the Crawler: ```python spider.py```
### Indexer
- Start the Indexer: ```javac IndexServiceTest.java```
### Backend 
- Start the Search API Server: ```python app.py```
- Create tables in MySQL:
```
 CREATE TABLE users (
         id INT PRIMARY KEY AUTO_INCREMENT,
         username VARCHAR(50) UNIQUE NOT NULL,
         password VARCHAR(255) NOT NULL
     );
CREATE TABLE search_log (
         id INT PRIMARY KEY AUTO_INCREMENT,
         username VARCHAR(50) NOT NULL,
         query TEXT,
         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
     );
```
### Frontend
- Start the Search Web UI: ```npm run serve```

