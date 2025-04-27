from nltk.stem import PorterStemmer
from nltk.corpus import stopwords
from nltk.tokenize import word_tokenize
import re
import nltk
nltk.download('stopwords')
nltk.download('punkt')
# 初始化 stemmer 和停用词表
stemmer = PorterStemmer()
stop_words = set(stopwords.words('english'))
global_mapping = {}
# 去除停用词并只保留英文单词
def remove_stopwords(text: str) -> list:
    tokens = word_tokenize(text.lower())
    filtered = [word for word in tokens if word.isalpha() and word not in stop_words]
    return filtered

# 对单词列表进行词干化处理
def stem(words: list) -> list:
    #return [stemmer.stem(word) for word in words]
    global global_mapping  # 使用全局映射表
    stemmed_words = []
    
    for word in words:
        stemmed_word = stemmer.stem(word)
        stemmed_words.append(stemmed_word)
        global_mapping[word] = stemmed_word  # 更新全局映射表
    
    return stemmed_words,global_mapping

# 提取bigram
def extract_bigrams(query: str) -> list:
    query = query.lower()
    words = re.findall(r'\w+', query)
    words = [word for word in words if word not in stop_words]
    words = [stemmer.stem(word) for word in words]
    bigrams = [f'{words[i]}_{words[i+1]}' for i in range(len(words)-1)]
    return bigrams

def get_original_word(stemmed_word) -> list:
    # 找到所有原始单词与目标词干相匹配的项
    original_words = [word for word, stem in global_mapping.items() if stem == stemmed_word]
    return original_words
