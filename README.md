# Remote Code Execution Microservice Application

This microservice-based application allows for remote code execution within Docker containers. It provides a scalable and secure way to execute user-submitted code in isolated environments.

## Watch Demo

[![Watch Demo](https://i9.ytimg.com/vi_webp/7ZvlDutX2kk/mq2.webp?sqp=CLSLscUG-oaymwEmCMACELQB8quKqQMa8AEB-AHSCIAC0AWKAgwIABABGFMgWihlMA8%3D&rs=AOn4CLAJSVo2MPeV2_-sHxYHF-FrIolWgQ&retry=3)](https://www.youtube.com/watch?v=7ZvlDutX2kk)

## Features

-   Remote code execution in isolated Docker containers
-   Asynchronous communication between microservices using RabbitMQ
-   Real-time bi-directional communication via WebSockets
-   Service discovery with Eureka
-   API Gateway with JWT authentication
- Multi-language support

## Architecture

The application consists of the following microservices:

1. Submission Service: Handles user code submissions and pushes them to RabbitMQ.
2. Consumer Service: Consumes requests from RabbitMQ, launches Docker containers, executes code. You can execute interactive programs and can communicate bi-directionally via websockets.
3. Eureka Server: Provides service discovery for the microservices.
4. API Gateway: Routes requests to appropriate services and handles authentication.

## Prerequisites

-   Java 21
-   Maven
-   Docker
-   RabbitMQ

## Installation and Setup
Prerequisites: 
1. Install docker.
2. For mac users (Optional):
   1. Install socat:
   
    ```
    brew install socat
    ```

3. Clone the repository:
 
    ```
    git clone https://github.com/yourusername/remote-code-execution.git
    cd remote-code-execution
    ```

4. Build the microservices:

    ```
    mvn clean package -DskipTests
    ```

Setup:
1. Install and run rabbit mq:

    ```
    docker run -it --rm --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:4-management
    ```

2. For mac users (Optional): 

    ```
    socat -d -d TCP-LISTEN:2375,bind=127.0.0.1,reuseaddr,fork UNIX-CLIENT:$HOME/.docker/run/docker.sock
    ```

3. Start the Eureka Server:

    ```
    java -jar eureka-service/target/eureka-service-0.0.1-SNAPSHOT.jar
    ```

4. Start the Submission Service:

    ```
    java -jar submission-service/target/submission-service-0.0.1-SNAPSHOT.jar
    ```

5. Start the Consumer Service:

    ```
    java -jar consumer-service/target/consumer-service-0.0.1-SNAPSHOT.jar
    ```

6. Start the API Gateway:
    ```
    java -jar api-gateway-service/target/api-gateway-service-0.0.1-SNAPSHOT.jar
    ```

## Configuration

### Ports

-   Eureka Server: 8083
-   Submission Service: 8080
-   Consumer Service: 8081
-   API Gateway: 8020

### RabbitMQ Configuration

-   spring.rabbitmq.host=localhost
-   spring.rabbitmq.port=5672
-   spring.rabbitmq.username=guest
-   spring.rabbitmq.password=guest

## Usage

1. Submit code through the Submission Service API:
   here is a python code example: "print("hello world")":

    ```
    POST /api/execute/code
    {
      "codeContent": "print("hello world")",
      "language": "python"
    }
    ```

2. The Consumer Service will execute the code and stream the output via WebSocket.

3. Connect to the WebSocket endpoint to receive real-time output:
    ```
    ws://localhost:8081/docker-output?sessionId
    ```