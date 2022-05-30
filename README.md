# MSA의 트랜잭션의 종류
## 1. Two-Phase Commit
## 2. Saga Pattern
   
> ## 1. Two-Phase Commit

Two-Phase Commit이란 2단계에 거쳐서 영속하는 작업, 영속성 컨테스트와는 다르다

![1](https://img1.daumcdn.net/thumb/R1280x0/?scode=mtistory2&fname=https%3A%2F%2Fblog.kakaocdn.net%2Fdn%2FsoYaj%2FbtqMqBzDjed%2FuJfEtdeMSgCPgAY4SkmdNK%2Fimg.png)

분산 DB 환경에서는 위 그림과 같이 주 DB와 보조 DB로 나뉜다. 그러나 실제 모놀리틱에서 연결된 메인 DB는 Primary DB인데, 이들이 이중화 된 데이터베이스 형태를 가지려면 그들이 동기화 형태로 되어야한다. 

Two-Phase Commit은 이러한 주 DB와 보조 DB 사이에 트랜잭션을 조율하는 조정자 (Coordinator)가 존재하며 이의 역할은 트랜잭션 요청이 들어왔을 때, 아래의 두 단계를 거쳐 트랜잭션 담당을 진행한다.
- Prepare
- Commit

여기서 Prepare라는 작업이 모놀리틱과의 차이점 모놀리틱을 가진 형태는 그들의 인스턴스를 같이 사용하기 떄문에 트랜잭션을 적용하려는 DB가 트랜잭션이 가능한 상태인지를 알아야 할 필요가 없습니다. 그러나 인스턴스가 분리된 MSA에서는 대상 DB가 트랜잭션이 가능한 상태인지 미리 확인되어야 합니다.

![2](https://img1.daumcdn.net/thumb/R1280x0/?scode=mtistory2&fname=https%3A%2F%2Fblog.kakaocdn.net%2Fdn%2FouOBn%2FbtqMknbDb9T%2FZK6H2T1kFh3q1dwF2fKKy1%2Fimg.png)

위 그림은 쇼핑몰에서 상품을 주문했을 때, 발생하는 MSA 내의 데이터 변화를 도식한 것입니다. 먼저 주문을 하게 되면 사용자의 정보를 가져와야 하고, 그에 따른 결제가 이뤄지면 배송 기록이 남아야 합니다. 따라서 주문이 발생하면 상품과, 배송 정보, 사용자 정보 등에서 DB의 트랜잭션이 발생해야 하는데, 여기서는 간단하게 사용자와 배송 정보 데이터베이스만을 그림으로 그렸습니다.

 

주문이 발생하면 Order API로부터 요청 받은 DB는 Commit 작업을 위한 준비를 진행합니다. 이후 이와 연결되어 있는 DB의 영속 여부가 확인되면 조정자에게 준비가 완료되었음을 알리고, 위와 같이 Commit을 진행하게 됩니다.

![3](https://img1.daumcdn.net/thumb/R1280x0/?scode=mtistory2&fname=https%3A%2F%2Fblog.kakaocdn.net%2Fdn%2FbuCmsz%2FbtqMiZotltv%2FdwrMxsc8m1eNcxVYk5p1U1%2Fimg.png)

반대로 관련된 DB 중 하나의 인스턴스라도 트랜잭션의 준비가 안된 상태라면 바로 Rollback을 실행해야 합니다. 그러므로 연관된 DB와 같이 트랜잭션이 이루어지기 때문에 트랜잭션의 범위는 처리하려는 DB와 연관된 DB가 전체로 진행됩니다.

 

이 순서를 간략하게 요약하면 다음과 같습니다.
1. 조정자가 연관된 DB로 전달한 메시지에 대해 응답을 기다린다.
2. 모든 메시지가 수신이 성공적으로 완료될 경우 commit을 진행한다.
3. commit 단계에서 조정자는 연관된 DB에 데이터를 저장하라는 명령 메시지를 보낸다
4. 관련 DB들은 데이터를 영속화 한다.

Two-Phase Commit의 단점
Two-Phase Commit의 설계를 보면 분산 트랜잭션 형태를 지니고 있습니다. 따라서 분산 트랜잭션을 사용할 수 있는 애플리케이션이라면 어떤 데이터베이스든지 가능합니다.

 

그러나 최근에는 NoSQL이 자주 사용되고 있는데, 공교롭게도 NoSQL에서는 분산 트랜잭션을 지원하지 않습니다. 따라서 함꼐 사용하는 DB가 동일한 미들웨어이어야 하므로 DBMS polyglot 구성은 어렵습니다.

 

또한 DB 이중화 구조와 비슷하기 때문에 하나의 API 서버에서 요청이 들어오고 내부적으로 DB가 분산되어 있을 때 사용하는 형태로 되어 있어, MSA와 같이 API가 분리되어 있고, 각기 다른 특징을 가진 DB를 분리한 MSA에서는 구현이 쉽지 않다는 것도 이에 포함될 수 있습니다.

> ## 2. Saga Pattern
Saga는  분산 컴퓨팅 아키텍처인 Eventual Consistency를 바탕으로 둔 로컬 트랜잭션을 연속적으로 업데이트 수행하는 패턴

트랜잭션의 관리 주체가 DB 서버 자신들이 아닌 애플리케이션에 있으며 애플리케이션이 분산되었을 때 각 애플리케이션 하위에 존재하는 DB는 자신의 트랜잭션만 처리하는 구조입니다.

 

Two-Phase Commit과 다르게 본인들의 트랜잭션만을 처리하며 애플리케이션 개발자가 트랜잭션 로직을 구현해야 하는 형태입니다.

 

Saga 패턴에는 아래의 2가지 종류가 있습니다.

1) Choreograpgy-Based Saga
2) Orchestration-Based Saga

Saga 인스턴스를 별도로 사용하여 처리하느냐 그렇지 않느냐의 차이인데

## <span style="color:#00BBBB">1. Choreograpgy-Based Saga</span>
![4](https://img1.daumcdn.net/thumb/R1280x0/?scode=mtistory2&fname=https%3A%2F%2Fblog.kakaocdn.net%2Fdn%2FcYFOFr%2FbtqMknbEmNP%2FmZkM0mOOMeBJRkm8ugQDAk%2Fimg.png)

Choreography-Based Saga 패턴은 자신이 보유한 서비스 내 DB만의 트랜잭션을 관리하며 트랜잭션이 종료되면 완료 이벤트를 발행합니다. 이어 수행해야 할 트랜잭션이 있다면 해당 애플리케이션으로 완료 이벤트를 발행하고, 해당 이벤트를 받은 애플리케이션에서 계속 트랜잭션을 이어 수행합니다. 마지막에 도달하면 메인 애플리케이션에 그 결과를 전달하여 최종적으로 DB에 영속하는 방법입니다.

이벤트 발행과 구독을 위해 RabbitMQ, Kafka와 같은 메시지 큐 미들웨어를 이용하여 비동기 방식 혹은 분산 처리 형태로 전달할 수 있습니다.

![5](https://img1.daumcdn.net/thumb/R1280x0/?scode=mtistory2&fname=https%3A%2F%2Fblog.kakaocdn.net%2Fdn%2FbajYPP%2FbtqMmptwKKC%2FUbCUp61GvMwAkfTausMJK1%2Fimg.png)

Rollback의 경우 각 애플리케이션에서 트랜잭션을 관리하는 로직을 구현하여 중간에 트랜잭션이 실패하면 해당 트랜잭션 취소 처리를 실패한 애플리케이션에서 보상 Event를 발행하여 Rollback 처리를 실행합니다.

RabbitMQ의 RPC나 Publish-Subscribed 패턴만 잘 이용해본다면 쉽게 구현해볼 수 있습니다.

## <span style="color:#00BBBB">2. Orchestration-Based Saga</span>
![6](https://img1.daumcdn.net/thumb/R1280x0/?scode=mtistory2&fname=https%3A%2F%2Fblog.kakaocdn.net%2Fdn%2FxdQeD%2FbtqMi0AXiqM%2F9Do9Wfvr6UKV7rWHKqmWY0%2Fimg.png)
Orchestration-Based Saga는 트랜잭션 처리를 위한 인스턴스가 별도로 존재하며 이를 우리는 Manager라 부릅니다. 중계적인 역할을 하지만 클라이언트에서의 요청은 한 API에서 한정적이기 때문에 이 인스턴스는 클라이언트의 요청을 받을 애플리케이션과 서비스 인스턴스로도 움직일 수 있습니다. 위의 그림이 바로 그런 형태입니다.

 

트랜잭션을 수행하는 모든 애플리케이션은 Saga 인스턴스 매니저에 의하여 점진적으로 트랜잭션을 수행하여 결과를 Manager에게 전달하는 형태입니다. 비즈니스 로직상 마지막 트랜잭션이 끝나면 Saga 인스턴스는 전체 트랜잭션이 종료한 뒤, 인스턴스는 소멸됩니다. 

![7](https://img1.daumcdn.net/thumb/R1280x0/?scode=mtistory2&fname=https%3A%2F%2Fblog.kakaocdn.net%2Fdn%2FeHQlkk%2FbtqMqA1QaY4%2Fkk7mVxuJh3ugVvbUFvP6A0%2Fimg.png)
만약 중간에 실패하게 되면, Manager가 보상 트랜잭션을 실행하여 일관성을 유지하도록 해줍니다. 모든 관리를 중앙의 매니저가 모두 알아서 해주기 때문에  MSA에서도 트랜잭션을 중앙에서 해주는 구조가 만들어집니다. 가장 안정적이고 관리가 편하며 모놀리틱한 형태를 그대로 구현해줄 수 있다는 장점이 있습니다.

 

그러나 중간에 매니저는 인스턴스 형태이기 때문에 인프라 엔지니어링 입장에서는 번거로움이 많습니다. 마치 서비스 구조를 하나 만드는데, 있어 관리해야 할 미들웨어 내지 소프트웨어가 추가되는 것이기 때문이죠.

 

Spring Framework에서 대표적으로 Orchestration-Based Saga를 사용할 수 있는 방법으로 Axon Framework가 있습니다.


# Apache Kafka
## 특징  
1. Producer/Consumer 분리
2. 메세지를 여러 Consumer에게 허용 (EX. Hadoop,Serch Engine, Monitoring, Email)
3. 높은 처리량을 위한 메시지 최적화
4. Scale-out 가능
5. Eco-system

## Kafka Broker
- 실행 된 Kafka 애플리케이션 서버
- 3대 이상의 Broker Cluster 구성
- Zookeeper 연동
    - 역할 : 메타데이터 (Broker ID, Controller ID 등) 저장
    - Controller 정보 저장
- n개 Broker 중 1대는 Controller 기능 수행
    - Controller 역할
        - 각 Broker에게 담당 파티션 할당 수행
        - Broker 정상 동작 모니터링 관리

## Kafka Client
- kafka와 데이터를 주고받기 위해 사용하는 java library
- Producer, Consumer, Admin, Stream 등 Kafka 관련 API 제공
- 다양한 3rd party library 존재: C/C++, Node.js, Python, .Net 등(<http://cwiki.apache.org/confluence/display/KAFKA/Clients>)

## Kafka Connect
- Kafka Connect를 통해 Data를 Import/Export 가능
- 코드 없이 Configuration으로 데이터를 이동
- Standalone mode, Distribution mode 지원
    - RESTful APU를 통해 지원
    - Stream 또는 Batch 형태로 데이터 전송 가능
    - 커스텀 Connector를 통한 다양한 Plugin 제공(File,S3,Hive,Mysql,etc...)



