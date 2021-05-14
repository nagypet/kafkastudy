# kafkastudy

## Introduction
- Adam Mautner: [Introducing Apache Kafka](https://imarcats.wordpress.com/2019/02/13/introducing-apache-kafka/)

## Installation
- Adam Mautner: [Installing Kafka using Docker](https://imarcats.wordpress.com/2019/02/13/installing-kafka-using-docker/)

I have slightly modified the `docker-compose.yml`.

Installation of mc in the kafka container: apk add mc

`docker exec -it kafkastudy_kafka /bin/bash`

Within the kafka container:
`kafka-topics.sh --list --zookeeper zookeeper:2181`
`kafka-console-producer.sh --broker-list kafka:9092 --topic test`
`kafka-console-consumer.sh --bootstrap-server kafka:9092 --topic test --from-beginning`
