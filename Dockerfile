FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25@sha256:7afde2c565ed2664bbaf507c2ff02e050cfcb3e5c414ee69e24a8f80e9d1e31c

WORKDIR /app
COPY build/install/innkrevingshendelser/lib/ /app/lib/
USER 65532:65532
EXPOSE 8080
ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.utenlandsadresser.innkrevingshendelser.ApplicationKt"]
