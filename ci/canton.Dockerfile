FROM digitalasset/canton-open-source:2.2.0

COPY ci/canton.conf /conf/canton.conf
COPY ci/bootstrap-script.canton /conf/bootstrap-script.canton
COPY .daml/dist/daml-spring-boot-example-1.0.0.dar /app/daml-spring-boot-example.dar

EXPOSE 5011
EXPOSE 5012
EXPOSE 5018
EXPOSE 5019
EXPOSE 5021
EXPOSE 5022

ENTRYPOINT ["bin/canton", "-c", "/conf/canton.conf", "--bootstrap", "/conf/bootstrap-script.canton"]