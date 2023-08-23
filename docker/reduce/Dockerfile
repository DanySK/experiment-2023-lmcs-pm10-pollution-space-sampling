FROM danysk/kotlin:1.9.10

COPY scripts/reduce-large-file.main.kts /usr/local/bin/reduce-large-file.main.kts
RUN /usr/local/bin/reduce-large-file.main.kts || true
ENTRYPOINT ["/usr/local/bin/reduce-large-file.main.kts"]
