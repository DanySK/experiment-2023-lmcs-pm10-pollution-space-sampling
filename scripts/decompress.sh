#!/usr/bin/env sh
find /output/data-summary.json -prune -size +2097152k -type f | grep "^" || (echo decompressing && unzstd -f "/output/data-summary.json.zstd")
