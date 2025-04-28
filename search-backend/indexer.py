from nltk.stem import PorterStemmer
from nltk.corpus import stopwords
from nltk.tokenize import word_tokenize
import re
import nltk
import ssl
ssl._create_default_https_context = ssl._create_unverified_context
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
    query = query.lower()  # 将查询转换为小写
    words = re.findall(r'\w+', query)  # 找到所有的单词
    words = [word for word in words if word not in stop_words]  # 去除停止词
    stemmed_words = stem(words)  # 获取词干化后的词汇
    
    # 生成大二元组：根据词干生成原始单词
    bigrams = []
    for i in range(len(stemmed_words)-1):
        first_word_stem = stemmed_words[i]
        second_word_stem = stemmed_words[i+1]
        
        # 获取原始单词的集合
        first_word_orig = get_original_word(first_word_stem,global_mapping)
        second_word_orig = get_original_word(second_word_stem,global_mapping)
        
        # 创建大二元组，格式化为原始单词的大二元组
        for first in first_word_orig:
            for second in second_word_orig:
                bigrams.append(f'{first_word_stem}_{second_word_orig}')
                global_mapping[f'{first} {second}'] = f'{first_word_stem}_{second_word_orig}'
    
    return bigrams,global_mapping


def get_original_word(stemmed_word,mapping) -> list:
    # 找到所有原始单词与目标词干相匹配的项
    original_words = [word for word, stem in mapping.items() if stem == stemmed_word]
    return original_words
