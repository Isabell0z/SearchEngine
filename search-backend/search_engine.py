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
        self.total_docs = es.search(index="meta_doc", body=body)['hits']['total']['value']
        self.body_index = es.search(index="body_index", body=body)
        self.title_index = es.search(index="title_index", body=body)
        self.meta = es.search(index="meta_doc", body=body)['hits']['hits']

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
        docs = [int(doc) for doc,_ in scores[:50]]
        body = {
            "query": {
                "terms": {
                     "page_id": docs
                }
            }
        }
        response = es.search(index="meta_doc", body=body, size=50)
        pageid_to_content = {}
        for hit in response["hits"]["hits"]:
            source = hit["_source"]
            page_id = source["page_id"]
            pageid_to_content[page_id] = source            
        for doc, score in scores[:50]:
            content = pageid_to_content.get(int(doc))  
            if content:  
                top_docs.append({
                    "content": content,
                    "score": score
                })
                
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
            df = len(index['_source']['bodyDocMap']) or 1 
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
            postings = index['_source']['bodyDocMap']  
            for posting in postings:
                docs.add(posting)
        
        return docs

    def _cosine_similarity(self, doc, idf_vec, terms):
        body = {
            "query": {
                "term": {
                    "page_id": doc
                }
            }
        }
        indexs = es.search(index="meta_doc", body=body)
        dot = 0
        doc_len = indexs['hits']['hits'][0]['_source']['size']  ######????
        for term in terms:
            tf = self._get_tf(term, doc)
            max_tf = indexs['hits']['hits'][0]['_source']['max_tf']
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
