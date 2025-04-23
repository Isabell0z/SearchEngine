import requests
from bs4 import BeautifulSoup
from collections import deque
from urllib.parse import urljoin
from elasticsearch import Elasticsearch
from datetime import datetime
from urllib import parse

# kibana 7.3.1
# elasticsearch 7.3.1
# python 3.9.1

class Spider:
    def __init__(self, start_url, max_pages):
        self.start_url = start_url
        self.max_pages = max_pages
        self.visited_urls = set()
        self.url_map = {}  
        self.to_visit_urls = deque([start_url])
        self.page_count = 0
        self.page_id_counter = 1  
        # initialize Elasticsearch
        self.es = Elasticsearch("http://localhost:9200")
        # create indices
        self.create_index('web_pages', {
            "mappings": {
                "properties": {
                    "page_id": {"type": "integer"},
                    "url": {"type": "keyword"},
                    "title": {"type": "text"},
                    "content": {"type": "text"},
                    "last_modify_time": {"type": "date"},
                    "size": {"type": "integer"}
                }
            }
        })
        # create webpage structure（parent->child）
        self.create_index('web_page_structure', {
            "mappings": {
                "properties": {
                    "parent_page_id": {"type": "integer"},
                    "child_page_id": {"type": "integer"}
                }
            }
        })
        # create inverted webpage structure（child->parent）
        self.create_index('reverse_web_page_structure', {
            "mappings": {
                "properties": {
                    "child_page_id": {"type": "integer"},
                    "parent_page_id": {"type": "integer"}
                }
            }
        })

    def create_index(self, index_name, mapping):
        if self.es.indices.exists(index=index_name):
            self.es.indices.delete(index=index_name)
            print(f"index {index_name} deleted")
        self.es.indices.create(index=index_name, body=mapping)
        print(f"index {index_name} created")

    def fetch_page_links(self, url):
        try:
            response = requests.get(url)
            if response.status_code == 200:
                # get last modified time
                last_modified = response.headers.get('Last-Modified')
                if last_modified:
                    last_modify_time = datetime.strptime(last_modified, '%a, %d %b %Y %H:%M:%S %Z')
                else:
                    last_modify_time = "1970-01-01T00:00:00Z"  # default value if not available
                soup = BeautifulSoup(response.text, 'html.parser')
                title = soup.title.string if soup.title else ''
                content =soup.body.get_text().replace('\n', '').replace('\r', '')
                size=response.raw._fp_bytes_read if response.raw._fp_bytes_read else 0 #get size of the page(bytes)

                # not visited and not assigned id
                if url not in self.url_map:
                    page_id = self.page_id_counter
                    self.page_id_counter += 1
                    self.url_map[url] = page_id
                # not visited but assigned id
                else:
                    page_id = self.url_map[url]
                    
                # store page info in Elasticsearch
                self.store_page_info(page_id, url, title, content, last_modify_time,size)

                # find all links on the page
                links = [link.get('href') for link in soup.find_all('a', href=True)]
                return page_id, links
            return None, []
        except requests.RequestException as e:
            print(f"Failed to fetch {url}: {e}")
            return None, []

    def store_page_info(self, page_id, url, title, content, last_modify_time,size):
        doc = {
            "page_id": page_id,
            "url": url,
            "title": title,
            "content": content,
            "last_modify_time": last_modify_time,
            "size": size
        }
        self.es.index(index='web_pages', id=page_id, body=doc)

    def store_page_structure(self, parent_page_id, child_page_id):
        # store parent-child relationship in Elasticsearch
        doc = {
            "parent_page_id": parent_page_id,
            "child_page_id": child_page_id
        }
        self.es.index(index='web_page_structure', body=doc)
        # store child-parent relationship in Elasticsearch
        reverse_doc = {
            "child_page_id": child_page_id,
            "parent_page_id": parent_page_id
        }
        self.es.index(index='reverse_web_page_structure', body=reverse_doc)

    def crawl(self):
        while self.to_visit_urls and self.page_count < self.max_pages:
            url = self.to_visit_urls.popleft()
            if url in self.visited_urls:
                continue
            print(f"Crawling: {url}")
            self.visited_urls.add(url)
            self.page_count += 1
            parent_page_id, links = self.fetch_page_links(url)
            if parent_page_id:
                for link in links:
                    new_full_url = urljoin(url, link)
                    
                    # not visited and not assigned id
                    if new_full_url not in self.url_map:
                        child_page_id = self.page_id_counter
                        self.page_id_counter += 1
                        self.url_map[new_full_url] = child_page_id
                        self.to_visit_urls.append(new_full_url)
                    # not visited but assigned id
                    elif new_full_url in self.url_map and new_full_url not in self.visited_urls:
                        child_page_id = self.url_map[new_full_url]
                        self.to_visit_urls.append(new_full_url)
                    # visited and assigned id
                    elif new_full_url in self.visited_urls:
                        child_page_id = self.url_map[new_full_url]
                    
                    self.store_page_structure(parent_page_id, child_page_id)
    
    def view_web_pages_data(self):
        query = {
            "query": {
                "match_all": {}
            },
            "size": 1000
        }
        result = self.es.search(index='web_pages', body=query)
        for hit in result['hits']['hits']:
            print(hit['_source'])

    def view_web_page_structure_data(self):
        query = {
            "query": {
                "match_all": {}
            },
            "size": 1000
        }
        result = self.es.search(index='web_page_structure', body=query)
        for hit in result['hits']['hits']:
            print(hit['_source'])
            
    def view_reverse_web_page_structure_data(self):
        query = {
            "query": {
                "match_all": {}
            },
            "size": 1000
        }
        result = self.es.search(index='reverse_web_page_structure', body=query)
        for hit in result['hits']['hits']:
            print(hit['_source'])


if __name__ == "__main__":
    start_url = 'https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm'
    max_pages = 300
    spider = Spider(start_url, max_pages)
    spider.crawl()