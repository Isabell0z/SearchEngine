import json
import math
from collections import Counter
from indexer import stem, remove_stopwords, extract_bigrams,get_original_word
from elasticsearch import Elasticsearch
import re
from spell import correction

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
        terms, oriTerms = stem(remove_stopwords(query))
        phrases,oriTerm = extract_bigrams(query)
        oriTerms.update(oriTerm)
        all_terms = terms + phrases
        idf_vec = self._build_idf_vector(all_terms)
        candidates = self._find_candidate_docs(all_terms)
        
        # 简单的纠错，e.g. moive-> movie
        if not candidates:
            corrected_query = "".join([correction(term) for term in all_terms])
            terms, oriTerms = stem(remove_stopwords(corrected_query))
            phrases,oriTerm = extract_bigrams(corrected_query)
            oriTerms.update(oriTerm)
            all_terms = terms + phrases
            idf_vec = self._build_idf_vector(all_terms)
            candidates = self._find_candidate_docs(all_terms)
        
        unique_terms = Counter(all_terms)
        query_vec = {}
        for term, tf in unique_terms.items():
            idf = idf_vec.get(term, 0)
            query_vec[term] = tf * idf
        scores = []
        top_docs = []
        for doc in candidates:
            score, key_term = self._cosine_similarity(doc, idf_vec, query_vec)
            scores.append((doc,score,key_term))


        scores.sort(key=lambda x: x[1], reverse=True)
        docs = [int(doc) for doc,_,_ in scores[:50]]
        body = {
            "query": {
                "terms": {
                     "page_id": docs
                }
            },
            "size": 50
        }
        response = es.search(index="meta_doc", body=body)
        response_content = es.search(index="web_pages", body=body)
        text_content = {}
        for hit in response_content["hits"]["hits"]:
            source = hit["_source"]
            page_id = source["page_id"]
            text_content[page_id] = source    
        pageid_to_content = {}
        for hit in response["hits"]["hits"]:
            source = hit["_source"]
            page_id = source["page_id"]
            pageid_to_content[page_id] = source          
        
        links_ids = []
        for doc, score,_ in scores[:50]:
            content = pageid_to_content.get(int(doc))
            if content:
                new_links = set(content.get("child_links") +content.get("parent_links"))
                # 仅添加不在 links_ids 中的链接
                links_ids.extend([link for link in new_links if link not in links_ids])
                
        if links_ids:
            body = {
                "query": {
                    "terms": {
                        "page_id": links_ids
                    }
                }
            }
            resp = es.search(index="meta_doc", body=body, size=300)
            print(resp["hits"]["hits"])
            for hit in resp["hits"]["hits"]:
                page_id = hit["_source"]["page_id"]
                for doc in pageid_to_content:
                    content = pageid_to_content.get(doc)
                    if content and page_id in content.get("child_links", []):
                        content['child_links'] = [link for link in content['child_links'] if link != page_id]
                        content['child_links'].append({'title': hit["_source"].get("title", ""), 'link': hit["_source"].get("url", "")})
                    if content and page_id in content.get("parent_links", []):
                        content['parent_links'] = [link for link in content['parent_links'] if link != page_id]
                        content['parent_links'].append({'title': hit["_source"].get("title", ""), 'link': hit["_source"].get("url", "")})

        for doc, score,keyword in scores[:50]:
            content = pageid_to_content.get(int(doc))
            text = text_content.get(int(doc))['content']
            snippents = extract_snippets(text,get_original_word(keyword,oriTerms)[0])
            if content:
                content['term_freq_list'] = sorted(content['term_freq_list'], key=lambda x: x['frequency'], reverse=True)[:5]
                top_docs.append({
                    "content": content,
                    "snippents":  snippents,
                    "score": score
                })

        return top_docs

    def _build_idf_vector(self, terms):
        # Compute idf of each term  using cache
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
        max_tfidf = 0.0
        key_term =""
        indexs = es.search(index="meta_doc", body=body)
        dot = 0
        doc_len = indexs['hits']['hits'][0]['_source']['length']  ######????
        max_tf = indexs['hits']['hits'][0]['_source']['max_tf']
        for term in terms.keys():
            tf = self._get_tf(term, doc)
            idf = idf_vec.get(term, 0)
            if max_tf != 0 :
                doc_weight = (tf / max_tf) * idf
            else:
                doc_weight = tf  * idf
            if self._title_hit(doc, term):
                doc_weight *= 2.0
            k = terms.get(term, 0) * doc_weight
            if k > max_tfidf:
                max_tfidf = k
                key_term = term
            dot += k
        query_len = math.sqrt(sum(v**2 for v in terms.values()))
        return dot / (doc_len * query_len + 1e-6), key_term

    def _get_tf(self, term, doc_id):
        body = {
            "query": {
                "term": {
                    "term": term
                }
            }
        }
        indexs = es.search(index="body_index", body=body)
        if indexs['hits']['hits']:
            postings = indexs['hits']['hits'][0]['_source']['bodyDocMap'] 
            if str(doc_id) in postings:
                return postings[str(doc_id)]
        return 0

    def _title_hit(self, doc_id, term):
        body = {
            "query": {
                "term": {
                    "term": term
                }
            }
        }
        indexs = es.search(index="title_index", body=body)
        for index in indexs['hits']['hits']:
            for p in index['_source']['titleDocMap']:
                if p == doc_id:
                    return True
        return False
    
def extract_snippets(text, keyword, context_window=30,max_snippets=5):
    if not keyword:  
        return []

    snippets = []

    keyword = str(keyword)

    # 处理由下划线连接的 bigram：将下划线替换为空格
    keyword = keyword.replace('_', ' ')

    # 构建正则表达式，确保关键词是精确匹配的
    keyword_pattern = re.escape(keyword)  # 转义 keyword，以防特殊字符
    pattern = re.compile(r'\b{}\b'.format(keyword_pattern), flags=re.IGNORECASE)
    for match in pattern.finditer(text):
        if len(snippets) >= max_snippets:
            break  # 达到最大数量时停止
        start = max(0, match.start() - context_window)
        end = min(len(text), match.end() + context_window)
        snippet = text[start:end]
        highlighted = pattern.sub(r'<mark>\g<0></mark>', snippet)  # 用 <mark> 标签高亮
        snippets.append(highlighted)

    return snippets