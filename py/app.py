import hmac
import os

from flask import Flask, jsonify, request

from scanner.routes import bp as scanner_bp

LIMITE_UPLOAD = 20 * 1024 * 1024  # 20 MB por requisição


def create_app():
    app = Flask(__name__)
    app.config["MAX_CONTENT_LENGTH"] = LIMITE_UPLOAD
    app.register_blueprint(scanner_bp)
    app.json.sort_keys = False
    app.json.ensure_ascii = False

    token = os.environ.get("SCANNER_TOKEN", "")

    @app.before_request
    def exigir_token():
        # /health fica aberto (o Render usa para checar se o servico subiu).
        if not token or request.path == "/health":
            return None
        enviado = request.headers.get("X-Scanner-Token", "")
        if not hmac.compare_digest(enviado, token):
            return jsonify(erro="Nao autorizado"), 401
        return None

    @app.errorhandler(413)
    def grande(_):
        return jsonify(erro="Arquivo grande demais"), 413

    return app


if __name__ == "__main__":
    # Apenas para desenvolvimento local. No Docker roda via gunicorn.
    create_app().run(host="127.0.0.1", port=8000)