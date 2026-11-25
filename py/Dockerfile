FROM python:3.12-slim

RUN apt-get update \
 && apt-get install -y --no-install-recommends tesseract-ocr tesseract-ocr-por tesseract-ocr-eng \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app.py .
COPY scanner ./scanner

RUN useradd --create-home appuser
USER appuser

ENV TESSERACT_LANG=por+eng
# Render define PORT; localmente cai em 8000. 1 worker + 2 threads para caber na memoria.
CMD ["sh", "-c", "exec gunicorn -w 1 --threads 2 -t 180 -b 0.0.0.0:${PORT:-8000} 'app:create_app()'"]