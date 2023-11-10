from flask import Flask, send_file
import os

os.chmod('test.png', 0o644)

app = Flask(__name__)

@app.route('/img')
def serve_img():
    return send_file('test.png', mimetype='image/png')

if __name__ == '__main__':
    app.run(host='localhost', port=5001)
