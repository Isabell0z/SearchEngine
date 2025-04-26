import json
import math
from collections import Counter
from indexer import stem, remove_stopwords, extract_phrases
from elasticsearch import Elasticsearch

es = Elasticsearch("http://localhost:9200")
class SearchEngine:
    def __init__(self):
        # load data
        body = {
            "query": {
                "match_all": {
                }
            }
        }
        self.total_docs = 10000
        self.body_index = es.search(index="body_index", body=body)
        self.title_index = es.search(index="title_index", body=body)
        #self.meta = es.search(index="title_index", body=body)
        with open('data/doc_meta.json') as f:
            self.meta = json.load(f)

    def search(self, query):
        terms = stem(remove_stopwords(query))
        phrases = extract_phrases(query)
        idf_vec = self._build_idf_vector(terms)
        candidates = self._find_candidate_docs(terms)
        unique_terms = Counter(terms)
        unique_term = set(terms)
        scores = []
        top_docs = []
        for doc in candidates:
            score = self._cosine_similarity(doc, idf_vec, unique_terms)
            if self._title_hit(doc, unique_term):
                score *= 2.0
            scores.append((doc, score))

        scores.sort(key=lambda x: x[1], reverse=True)
        for doc, score in scores[:50]:
            top_docs.append({"content":self.meta['doc1'],
                             "score":score})
        return top_docs

    def _build_idf_vector(self, terms):
        # Compute idf of each term
        vec = {}
        unique_terms = set(terms)
        body = {
            "query": {
                "terms": {
                    "term": list(unique_terms)
                }
            }
        }
        indexs = es.search(index="body_index", body=body)
        for index in indexs['hits']['hits']:
            df = len(index['_source']['bodyDocMap']) or 1  ####
            idf = math.log(self.total_docs / df)
            vec[index['_source']['term']] = idf
        return vec

    def _find_candidate_docs(self, terms):
        # Use boolean filter (OR)
        docs = set()
        body = {
            "query": {
                "terms": {
                    "term": terms
                }
            }
        }
        indexs = es.search(index="body_index", body=body)
        for index in indexs['hits']['hits']:
            postings = index['_source']['bodyDocMap']  ##
            for posting in postings:
                docs.add(posting)
        return docs

    def _cosine_similarity(self, doc, idf_vec, terms):
        dot = 0
        doc_len = 100###########self.meta[doc]['length']
        for term in terms:
            tf = self._get_tf(term, doc)
            max_tf = 100#########self.meta[doc]['max_tf']
            idf = idf_vec[term]
            doc_weight = (tf / max_tf) * idf
            dot += terms[term] * doc_weight
        query_len = math.sqrt(sum(v**2 for v in terms.values()))
        return dot / (doc_len * query_len + 1e-6)

    def _get_tf(self, term, doc_id):
        body = {
            "query": {
                "term": {
                    "term": term
                }
            }
        }
        indexs = es.search(index="body_index", body=body)
        postings = indexs['hits']['hits'][0]['_source']['bodyDocMap']  ##
        if str(doc_id) in postings:
            return postings[str(doc_id)]
        return 0

    def _title_hit(self, doc_id, terms):
        body = {
            "query": {
                "terms": {
                    "term": list(terms)
                }
            }
        }
        indexs = es.search(index="title_index", body=body)
        for index in indexs['hits']['hits']:
            for p in index['_source']['titleDocMap']:
                if p == doc_id:
                    return True
        return False
