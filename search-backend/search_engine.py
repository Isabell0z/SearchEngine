import json
import math
from collections import Counter
from indexer import stem, remove_stopwords, extract_phrases

class SearchEngine:
    def __init__(self):
        # load data
        with open('data/body_index.json') as f:
            self.body_index = json.load(f)
        with open('data/title_index.json') as f:
            self.title_index = json.load(f)
        with open('data/doc_meta.json') as f:
            self.meta = json.load(f)
        self.total_docs = len(self.meta)

    def search(self, query):
        terms = stem(remove_stopwords(query))
        phrases = extract_phrases(query)

        idf_vec = self._build_idf_vector(terms)
        candidates = self._find_candidate_docs(terms)
        unique_terms = Counter(terms)
        scores = []
        top_docs = []
        for doc in candidates:
            score = self._cosine_similarity(doc, idf_vec, unique_terms)
            if self._title_hit(doc, unique_terms):
                score *= 2.0
            scores.append((doc, score))

        scores.sort(key=lambda x: x[1], reverse=True)
        for doc, score in scores[:50]:
            top_docs.append({"content":self.meta[doc],
                             "score":score})
        return top_docs

    def _build_idf_vector(self, terms):
        # Compute idf of each term
        vec = {}
        unique_terms = set(terms)
        for term in unique_terms:
            df = len(self.body_index.get(term, [])) or 1
            idf = math.log(self.total_docs / df)
            vec[term] = idf
        return vec

    def _find_candidate_docs(self, terms):
        # Use boolean filter (OR)
        docs = set()
        for term in terms:
            postings = self.body_index.get(term, [])
            for posting in postings:
                docs.add(posting)
        return docs

    def _cosine_similarity(self, doc, idf_vec, terms):
        dot = 0
        doc_len = self.meta[doc]['length']
        for term in terms:
            tf = self._get_tf(term, doc)
            max_tf = self.meta[doc]['max_tf']
            idf = idf_vec[term]
            doc_weight = (tf / max_tf) * idf
            dot += terms[term] * doc_weight
        query_len = math.sqrt(sum(v**2 for v in terms.values()))
        return dot / (doc_len * query_len + 1e-6)

    def _get_tf(self, term, doc_id):
        postings = self.body_index.get(term, {})
        if doc_id in postings:
            return postings[doc_id]['tf']
        return 0

    def _title_hit(self, doc_id, terms):
        for term in terms:
            for p in self.title_index.get(term, []):
                if p == doc_id:
                    return True
        return False
