# daml-spring-boot-example || Simple Stock Exchange

This example DAML solution and matching backend Java SpringBoot API is made as a teaching tool and example framework, solving this training problem:

This exercise is meant for people with a rudimentary or theoretical understanding of DAML, looking to turn that in to practical knowledge and advance their understanding of how to write DAML.

This simple stock exchange needs to have three different types of party: 1 Exchange owner, 1+ Traders, and 1+ Corporations. Traders and Corporations need to be on-boarded in to the system, at which point they can request for shares or cash to be created within the system. 

Only Corporations can create shares, and those shares are for that corporation. Both can create cash, and you must be able to manage different currency types. Both Traders and Corporations must be able to create a "Buy Offer" or "Sell Offer" to buy/sell X shares at Y price per share. The Exchange Owner must match valid Buy/Sell Offers together and transfer ownership of the cash/shares. You must be able to process partial matches (i.e. Alice wants to sell 50 shares at 5 per share, Bob wants to buy 10 shares at 5 per share, so the partial processing would sell the 10 shares to Bob and leave Alice's Sell Offer with 40 remaining).

You must also include test Scripts for this DAML. This exercise is not complete if there is insufficient testing of your smart contracts!

Try to think about underlying assumptions, how this would translate in to a full-stack application, what validation is necessary for each step in the process, and try to consider the difference between "functional" DAML (that completes the above) versus "elegant" DAML (that is user friendly, easy to read, and extensible).

#  Required  software
-   DAML Server -> https://docs.daml.com/getting-started/installation.html
-   Spring Boot Web App -> [JDK 8 or higher](https://www.azul.com/downloads/zulu/) and [Maven](https://maven.apache.org/download.cgi?Preferred=ftp://ftp.osuosl.org/pub/apache/)

#  How to start the applications
-	DAML Server -> `daml start` this command run from the project directory will start Sandbox ledger and UI, and opens automatically a new browser tab in the right address and port.
-	Spring Boot Web App -> To start the embedded Netty server from the command line use `mvn spring-boot:run` , or just run it from your IDE.
-   Docker -> The CI directory contains two Dockerfiles, one to build a basic [Canton](https://docs.daml.com/canton/tutorials/tutorials.html) network, and one to build a sandbox network. The Canton network still requires the DAML SDK, because the DAR file must exist to create the image. The sandbox network uses the Docker image for the SDK, so does not require you to install the SDK on the host machine.

# Project Features

- Swagger -> Full API endpoint list and definition, default URL: http://localhost:8083/webjars/swagger-ui/index.html
- NIO Server (Netty) with Spring WebFlux -> Full reactive stack from the API to Ledger, this framework is able to handle NIO and IO request in a single development pattern.
- DAML Java CodeGen -> Java classes for DAML templates are generated after executing `mvn generate-sources`. This avoids boilerplate code in a very elegant coding way.
- DAML SDK -> It has been upgraded to version 2.2.0
- Lombok -> Framework to reduce boilerplate code for any Java Class.
- Postman -> Inside the project you will find a postman collection to perform requests to the server for the current endpoints. Note that postman works in a IO manner and is useless for testing NIO endpoints.
- Global Error Handler -> There is an initial approach for error handling, for development stages it’s good to have all the information in the API response but for security reasons in a production environment we should show a very accurate message to the client.
- Spring DevTools -> Spring DevTools are enabled, it provides many things to speed up development. e.g. It restarts your server automatically after java changes are detected like JRebel does.
- Sleuth -> Provides tracing for each request, to match response to log messages
