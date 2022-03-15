FROM openjdk:11
RUN mkdir /opt/app
COPY ./build/libs/example-0.2.0.jar /opt/app/example.jar
EXPOSE 8080
CMD ["java", "-jar", "/opt/app/example.jar"]