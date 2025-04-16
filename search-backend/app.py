from flask import Flask, request, jsonify
from search_engine import SearchEngine
from flask_cors import CORS

app = Flask(__name__)
CORS(app)
CORS(app, resources={r"/search": {"origins": "http://127.0.0.1:8080"}})
CORS(app, origins="http://127.0.0.1:8080")
engine = SearchEngine()

@app.route('/search', methods=['GET', 'POST'])
def search():
    data = request.json # 获取前端发送的 JSON 数据
    print(data)
    query= data.get('data')  # 获取数据中的 'data' 字段
    print(query)
    results = engine.search(query)
    print(results)
    return results

@app.route('/search', methods=['OPTIONS'])
def options_request():
    return '', 200

@app.route('/login', methods=['POST'])
def login():
    data = request.json
    username = data.get('username')
    password = data.get('password')
    if username == 'admin' and password == '123456':
        return jsonify({'success': True})
    else:
        return jsonify({'success': False})

if __name__ == '__main__':
    app.run(debug=True)
