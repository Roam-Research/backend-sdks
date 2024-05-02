#!/usr/bin/env bash

set -euo pipefail

rm -rf dist/

echo "Building new python dist..."

docker build -t roam-python -f - . << 'EOF'
FROM python:latest
WORKDIR /usr/roam
RUN curl -sSL https://install.python-poetry.org | python3 -
ENV PATH="/root/.local/bin:$PATH"
RUN poetry self update
RUN poetry completions bash >> ~/.bash_completion
EOF
docker run -w "/usr/roam" -v "$PWD:/usr/roam"  roam-python sh -c "poetry install && poetry build && poetry run roam"
