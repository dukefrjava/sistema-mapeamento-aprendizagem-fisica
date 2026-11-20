import hmac, os

from flask import Flask, jsonify, request
from scanner.routes import bp


TOKEN = os.environ.get("SCANNER_TOKEN", "")

def create_app():
    app = Flask(__name__)
    app.json.sort_keys = False
    app.json.ensure_ascii = False
    # RECUSA UPLOADS ACIMA DE 20 MB
    app.config["MAX_CONTENT_LENGTH"] = 20 * 1024 * 1024
    app.register_blueprint(bp)

    @app.before_request
    def exigir_token():
        if not TOKEN or request.path == "/health":
            return None
        if not hmac.compare_digest(request.headers.get("X-Scanner-Token", ""), TOKEN):
            return jsonify(erro="não autorizado"), 401
    
    return app

if __name__ == "__main__":
    create_app().run(host="127.0.0.1", port=8000)