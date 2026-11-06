from flask import Flask
from scanner.routes import bp

def create_app():
    app = Flask(__name__)
    # RECUSA UPLOADS ACIMA DE 20 MB
    app.config["MAX_CONTENT_LENGTH"] = 20 * 1024 * 1024
    app.register_blueprint(bp)
    return app

if __name__ == "__main__":
    create_app().run(host="127.0.0.1", port=8000)