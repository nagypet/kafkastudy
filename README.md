# kafkastudy

## Introduction
Reference: [Introducing Apache Kafka by Adam Mautner](https://imarcats.wordpress.com/2019/02/13/introducing-apache-kafka/)

## Installation
Reference: [Installing Kafka using Docker by Adam Mautner](https://imarcats.wordpress.com/2019/02/13/installing-kafka-using-docker/)

I have slightly modified the `docker-compose.yml`. See in the folder: `kafkastudy\docker-compose\kafkastudy\`.

```
c:\np\github\kafkastudy\docker-compose\kafkastudy>docker ps
CONTAINER ID        IMAGE                       COMMAND                  CREATED             STATUS              PORTS                                                NAMES
3910223c60f2        wurstmeister/kafka:latest   "start-kafka.sh"         40 minutes ago      Up 40 minutes       0.0.0.0:9094->9094/tcp                               kafkastudy_kafka
f9dbe26440e0        wurstmeister/zookeeper      "/bin/sh -c '/usr/sb…"   40 minutes ago      Up 40 minutes       22/tcp, 2888/tcp, 3888/tcp, 0.0.0.0:2181->2181/tcp   kafkastudy_zookeper
```

Test the installation from the command line:

`docker exec -it kafkastudy_kafka /bin/bash`

Within the kafka container:
- Optional: install mc in the kafka container: `apk add mc`
- `kafka-topics.sh --list --zookeeper zookeeper:2181`
- `kafka-console-producer.sh --broker-list kafka:9092 --topic test`
- `kafka-console-consumer.sh --bootstrap-server kafka:9092 --topic test --from-beginning`
