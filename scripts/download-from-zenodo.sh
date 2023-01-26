#!/bin/sh
RESPONSE="$(curl -sL -H 'Accept: application/json' 'https://zenodo.org/api/records/7546592')"
echo "Received response: $RESPONSE"
LINK_AND_MD5="$(
  echo "$RESPONSE" | jq '
    .files
    | map(select(.filename | match("\\.json\\.zstd$")))
    | first
    | { name: .filename, link: .links.download, checksum: .checksum }
  '
)"
echo "Filtered response: $LINK_AND_MD5"
FILE="/output/$(echo "$LINK_AND_MD5" | jq -r '.name')"
MD5="$(echo "$LINK_AND_MD5" | jq -r '.checksum')"
FILE_MD5="$(md5sum "$FILE" | awk '{print $1}')"
echo "Expected MD5: $MD5, actual MD5: $FILE_MD5"
if [ -s "$FILE" ] && [ "$FILE_MD5" = "$MD5" ]; then
  echo "File $FILE already exists and checksum matches $MD5, skipping download"
else
  URL="$(echo "$LINK_AND_MD5" | jq -r '.link')"
  echo "Downloading $FILE from $URL"
  curl -sL "$URL" -o "$FILE"
fi
