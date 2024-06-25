
명령어 : 토픽 목록을 확인
```shell
docker-compose exec kafka /opt/bitnami/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092
```
결과
```text
__consumer_offsets
error.products.productsGroup
error.recommendations.recommendationsGroup
error.reviews.reviewsGroup
products
recommendations
reviews
```
- error로 시작하는 토픽은 데드 레터 대기열
- RabbitMQ에서 사용하던 auditGroup은 찾을 수 없다
- 모든 소비자가 토픽에 있는 모든 메시지를 처리할 수 있다


명령어 : 특정 토픽의 파티션 확인
```shell
docker-compose exec kafka /opt/bitnami/kafka/bin/kafka-topics.sh --describe --topic products --bootstrap-server localhost:9092
```

결과
```text
Topic: products TopicId: sY3Wbv2LQBK3MqIEiDYt8g PartitionCount: 2       ReplicationFactor: 1    Configs:
        Topic: products Partition: 0    Leader: 1001    Replicas: 1001  Isr: 1001
        Topic: products Partition: 1    Leader: 1001    Replicas: 1001  Isr: 1001
```


명령어 : 특정 토픽의 모든 메시지 확인
```shell
docker-compose exec kafka /opt/bitnami/kafka/bin/kafka-console-consumer.sh --topic products --bootstrap-server localhost:9092
```

결과
```text
{"eventType":"CREATE","key":1,"data":{"productId":1,"name":"product 1","weight":300,"serviceAddress":null},"eventCreatedAt":"2024-06-25T02:26:18.886178"}
{"eventType":"CREATE","key":2,"data":{"productId":2,"name":"product 2","weight":300,"serviceAddress":null},"eventCreatedAt":"2024-06-25T02:26:35.078688"}
```