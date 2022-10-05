FROM digitalasset/daml-sdk:2.2.0

COPY /daml /home/daml/templates
COPY /ci/sandbox-daml.yaml /home/daml/daml.yaml
COPY /ci/sandbox.conf /ci/sandbox.conf

EXPOSE 6865
EXPOSE 7575

USER root

ENTRYPOINT ["daml", "start"]