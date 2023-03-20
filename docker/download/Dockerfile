FROM alpine:3.17.2 as jq
RUN apk update
RUN apk add --no-cache jq oniguruma

FROM curlimages/curl:8.00.1
COPY --from=jq /usr/bin/jq /usr/bin/jq
COPY --from=jq /usr/lib/libonig.so.5 /usr/lib/libonig.so.5
COPY --from=jq /usr/lib/libonig.so.5.3.0 /usr/lib/libonig.so.5.3.0
COPY scripts/download-from-zenodo.sh /usr/local/bin/download.sh
ENTRYPOINT ["/bin/sh", "/usr/local/bin/download.sh"]
