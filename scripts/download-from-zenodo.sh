#!/bin/sh
RESPONSE="$(curl -sL -H "Accept: application/json" https://zenodo.org/api/records/7546592)"
LINK_AND_MD5="$(
  echo "$RESPONSE" | jq '
    .files
    | map(select(.filename | match("\\.json\\.zstd$")))
    | first
    | { name: .filename, link: .links.download, checksum: .checksum }
  '
)"
FILE="/output/$(echo "$LINK_AND_MD5" | jq -r '.name')"
MD5="$(echo "$LINK_AND_MD5" | jq -r '.checksum'))"
if [ -s "$FILE" ] && [ "$(md5sum "$FILE" | awk '{print $1}')" = "$MD5" ]; then
  echo "File $FILE already exists and checksum matches $MD5, skipping download"
else
  echo "Downloading $FILE"
  curl -sL "$(echo "$LINK_AND_MD5" | jq -r '.link')" -o "/output/$FILE"
fi
