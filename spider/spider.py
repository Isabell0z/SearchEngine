import requests
from bs4 import BeautifulSoup
from collections import deque
from urllib.parse import urljoin
from elasticsearch import Elasticsearch
from datetime import datetime
from urllib import parse
import re

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
        self.page_info_batch = []  # 暂存网页信息
        self.page_structure_batch = []  # 暂存网页结构信息
        self.reverse_page_structure_batch = []  # 暂存反向网页结构信息
        self.page_extended_info_batch = []  # 暂存网页扩展信息
        
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
        self.create_index('web_extended_info', {
            "mappings": {
                "properties": {
                    "page_id": {"type": "integer"},
                    "genre": {"type": "keyword"},  # 新增属性
                    "plot_summary": {"type": "text"},  # 新增属性
                    "plot_keywords": {"type": "keyword"},  # 新增属性
                    "country": {"type": "keyword"},  # 新增属性
                    "language": {"type": "keyword"},  # 新增属性
                    "company": {"type": "keyword"} ,  # 新增属性
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
                soup = BeautifulSoup(response.text, 'lxml')
                title = soup.title.string if soup.title else ''
                content = soup.get_text(separator='\n')  # 获取网页正文内容
                size=response.raw._fp_bytes_read if response.raw._fp_bytes_read else 0 #get size of the page(bytes)

                # 提取信息
                genre_match = re.search(  r'Genre:\s*(.*?)(?=\n(?!\n)(?=[A-Z])|more)', content, re.DOTALL)
                if genre_match:
                    genre = genre_match.group(1).strip().split('/')
                    genre = [g.strip() for g in genre if g.strip()]  # 去除空格和空字符串
                
                plot_summary_match = re.search(r'Plot Summary:\s*(.*?)(?=\n(?!\n)(?=[A-Z])|more)', content, re.DOTALL)
                if plot_summary_match:
                    plot_summary = plot_summary_match.group(1).strip()
                
                plot_keywords_match = re.search( r'Plot Keywords:\s*([\s\S]*?)(?=\n[^\n:]*:|more|\Z)', content, re.DOTALL)
                if plot_keywords_match:
                    plot_keywords = plot_keywords_match.group(1).strip().split('/')
                    plot_keywords = [pk.replace('\xa0',' ').strip() for pk in plot_keywords if pk.strip()]   
                
                country_match = re.search(r'Country:\s*(.*?)(?=\n\w+:|\Z)', content, re.DOTALL)
                if country_match:
                    country = country_match.group(1).strip().split('/')
                    country = [c.strip() for c in country if c.strip()]   
                
                language_match = re.search(r'Language:\s*(.*?)(?=\n\w+:|\Z)', content, re.DOTALL)
                if language_match:
                    language = language_match.group(1).strip().split('/')
                    language = [l.strip() for l in language if l.strip()]   
                
                company_match = re.search(r'Company:\s*(.*?)(?=\n(?!\n)(?=[A-Z])|more)', content, re.DOTALL)
                if company_match:
                    company = company_match.group(1).strip().split('/')
                    company = [c.strip() for c in company if c.strip()]  
                
                # not visited and not assigned id
                if url not in self.url_map:
                    page_id = self.page_id_counter
                    self.page_id_counter += 1
                    self.url_map[url] = page_id
                # not visited but assigned id
                else:
                    page_id = self.url_map[url]
                    
                # store page info in Elasticsearch
                # self.store_page_info(page_id, url, title, content, last_modify_time,size)

                self.page_info_batch.append({
                    "page_id": page_id,
                    "url": url,
                    "title": title,
                    "content": content.replace('\n', '').replace('\r', ''),
                    "last_modify_time": last_modify_time,
                    "size": size,
                })
                self.page_extended_info_batch.append({
                    "page_id": page_id,
                    "genre": genre if 'genre' in locals() else [],
                    "plot_summary": plot_summary if 'plot_summary' in locals() else '',
                    "plot_keywords": plot_keywords if 'plot_keywords' in locals() else [],
                    "country": country if 'country' in locals() else [],
                    "language": language if 'language' in locals() else [],
                    "company": company if 'company' in locals() else []
                })
                
                
                # find all links on the page
                links = [link.get('href') for link in soup.find_all('a', href=True)]
                return page_id, links
            return None, []
        except requests.RequestException as e:
            print(f"Failed to fetch {url}: {e}")
            return None, []

    def store_page_info(self):
        for doc in self.page_info_batch:
            self.es.index(index='web_pages', id=doc['page_id'], body=doc)
        self.page_info_batch=[]  
        for doc in self.page_extended_info_batch:
            self.es.index(index='web_extended_info', id=doc['page_id'], body=doc)
        self.page_extended_info_batch=[] 

    def store_page_structure(self):
        for doc in self.page_structure_batch:
            self.es.index(index='web_page_structure', body=doc)
        self.page_structure_batch = []
        for doc in self.reverse_page_structure_batch:
            self.es.index(index='reverse_web_page_structure', body=doc)
        self.reverse_page_structure_batch = []

    def crawl(self,batch_size=20):
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
                    
                    # self.store_page_structure(parent_page_id, child_page_id)
                    self.page_structure_batch.append({
                        "parent_page_id": parent_page_id,
                        "child_page_id": child_page_id
                    })
                    self.reverse_page_structure_batch.append({
                        "child_page_id": child_page_id,
                        "parent_page_id": parent_page_id
                    })

            if len(self.page_structure_batch) >= batch_size:
                    self.store_page_info()
            if len(self.reverse_page_structure_batch) >= batch_size:
                self.store_page_structure()
        if self.page_info_batch:
            self.store_page_info()
        if self.page_structure_batch or self.reverse_page_structure_batch:
            self.store_page_structure()
            


if __name__ == "__main__":
#def run():
    start_url = 'https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm'
    max_pages = 300
    spider = Spider(start_url, max_pages)
    spider.crawl()