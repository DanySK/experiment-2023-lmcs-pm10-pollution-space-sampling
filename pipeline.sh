#!/usr/bin/env sh
if [ "$CI" = 'true' ]; then
  echo CI: running reduced version
  docker compose run --build --rm reduce-dataset
else
  echo Production: running the full pipeline
  docker compose run --build decompress-dataset
fi
docker compose run --build finish
docker compose down
