FROM ubuntu:16.04

RUN apt-get update

#https://hub.docker.com/_/openjdk
#FROM openjdk:8

FROM maven:3.6.3-jdk-8

# Allow port 8000 to be accessed
# from outside the container
EXPOSE 8000

# Set the home directory to /root
ENV HOME /root

# cd into the home directory
WORKDIR /root

# Copy all app files into the image
COPY . .

RUN ls

WORKDIR /root

RUN mvn clean
RUN mvn compile
CMD mvn exec:java -Dexec.mainClass=main.java.Server

