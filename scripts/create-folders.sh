#!/bin/sh
for folder in 'data' 'charts' 'PM10-data' 'PM10-summary'; do
  mkdir -p "/experiment/$folder"
  chmod 777 "/experiment/$folder"
done
