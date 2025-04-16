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

# 去除停用词并只保留英文单词
def remove_stopwords(text: str) -> list:
    tokens = word_tokenize(text.lower())
    filtered = [word for word in tokens if word.isalpha() and word not in stop_words]
    return filtered

# 对单词列表进行词干化处理
def stem(words: list) -> list:
    return [stemmer.stem(word) for word in words]

# 提取带双引号的短语，例如 "hong kong" universities
def extract_phrases(query: str) -> list:
    phrase_pattern = r'"(.*?)"'
    phrases = re.findall(phrase_pattern, query)
    return [phrase.lower() for phrase in phrases]
