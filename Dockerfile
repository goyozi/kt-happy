FROM amazoncorretto:21-alpine-jdk

RUN mkdir src/

ADD build/libs/*-all.jar happy.jar

WORKDIR src/

ENTRYPOINT ["java", "-jar", "../happy.jar"]