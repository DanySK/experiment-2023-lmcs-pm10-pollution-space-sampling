#!/bin/sh
head -n2 /PM10-summary/data-summary.json | awk '{print substr($0,0,length($0)-1)"\n]"}' > /PM10-summary/test.json
