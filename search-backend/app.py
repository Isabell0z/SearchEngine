from flask import Flask, request, jsonify
from flask_jwt_extended import JWTManager, create_access_token, jwt_required, get_jwt_identity
from flask_jwt_extended import verify_jwt_in_request
from search_engine import SearchEngine
from flask_cors import CORS
import mysql.connector
from elasticsearch import Elasticsearch


app = Flask(__name__)
es = Elasticsearch("http://localhost:9200")

CORS(app)
CORS(app, resources={r"/search": {"origins": "http://127.0.0.1:8080"}})
CORS(app, origins="http://127.0.0.1:8080")
engine = SearchEngine()
app.config['JWT_SECRET_KEY'] = 'test1'
jwt = JWTManager(app)
db = mysql.connector.connect(
    host="localhost",
    user="root",
    password="zhouchuqiao0213",
    database="User"
)
cursor = db.cursor(dictionary=True)

@app.route('/search', methods=['POST'])
def search():
    verify_jwt_in_request(optional=True)

    # 获取当前用户
    current_user = get_jwt_identity()
    print("user:",current_user)
    data = request.json
    if not data:
        return jsonify({'msg': 'Missing JSON body'}), 400
    query= data.get('data')
    if not query:
        return jsonify({'msg': 'Missing query data'}), 400
    if current_user:
        cursor.execute(
            "SELECT 1 FROM search_log WHERE username = %s AND query = %s LIMIT 1",
            (current_user, query)
        )
        exists = cursor.fetchone()
        if not exists:
            cursor.execute(
                "INSERT INTO search_log (username, query) VALUES (%s, %s)",
                (current_user, query)
            )
            db.commit()
    return jsonify(engine.search(query))





    results = engine.search(query)
    print({"user": current_user, "result": results})
    return results

@app.route('/login', methods=['POST'])
def login():
    data = request.json
    username = data.get('username')
    password = data.get('password')
    cursor.execute("SELECT * FROM users WHERE username = %s AND password = %s", (username, password))
    user = cursor.fetchone()
    if user:
        token = create_access_token(identity=username)  # 存 ID 也可以存 username
        print("Generated JWT Token:", token)
        return jsonify(access_token=token)
    else:
        return jsonify(msg="Invalid credentials"), 401

@app.route('/register', methods=['POST'])
def register():
    data = request.json
    username = data.get('username')
    password = data.get('password')
    print(f"username = '{username}', password = '{password}'")
    cursor.execute("SELECT * FROM users WHERE username = %s", (username,))
    user = cursor.fetchone()
    print(user)
    if user:
        return jsonify({'msg': 'Invalid username'}), 401
    cursor.execute(
        "INSERT INTO users (username, password) VALUES (%s, %s)",
        (username, password)
    )
    db.commit()
    return jsonify({'msg': 'User registered successfully'}), 201

@app.route('/history', methods=['GET'])
@jwt_required()
def history():
    username = get_jwt_identity()
    cursor.execute("SELECT * FROM search_log WHERE username = %s ORDER BY created_at DESC", (username,))
    history = cursor.fetchall()
    return jsonify([item['query'] for item in history])

if __name__ == '__main__':
    app.run(debug=True,port=5001)
