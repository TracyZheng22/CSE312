FROM ubuntu:16.04

RUN apt-get update

# Set the home directory to /root
ENV HOME /root

# cd into the home directory
WORKDIR /root

# Copy all app files into the image
COPY . .

RUN ls

#https://hub.docker.com/_/openjdk
FROM openjdk:8

# Allow port 8000 to be accessed
# from outside the container
EXPOSE 8000

# Run the app
RUN javac *.java
CMD ["java", "Server"]

