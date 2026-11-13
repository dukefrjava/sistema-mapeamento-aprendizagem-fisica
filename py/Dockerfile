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
EXPOSE 8000
CMD ["gunicorn", "-w", "2", "-t", "120", "-b", "0.0.0.0:8000", "app:create_app()"]

