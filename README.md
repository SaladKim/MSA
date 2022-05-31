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
## Kafka란?
Kafka는 Pub-Sub 모델의 메시지 큐, 분산환경에 특화되어있는 특징을 가지고 있음

## 구성요소
1. Event            
Event는 kafka에서 Producer와 Consumer가 데이터를 주고 받는 단위, 이벤트 또는 메시지라고 표기
 
2. Producer        
Producer는 kafka에 이벤트를 게시(post)하는 클라이언트 어플리케이션을 의미

3. Consumer     
Consumer는 이러한 Topic을 구독하고 이로부터 얻어낸 이벤트를 처리하는 클라이언트 어플리케이션

4. Topic        
이벤트가 쓰이는 곳입니다. Producer는 이 Topic에 이벤트를 게시합니다. 그리고 Consumer는 Topic으로 부터 이벤트를 가져와 처리, Topic은 파일시스템의 폴더와 유사하며, 이벤트는 폴더안의 파일과 유사
Topic에 저장된 이벤트는 필요한 만큼 다시 읽을 수 있음.

5. Partition        
Topic는 여러 Broker에 분산되어 저장되며, 이렇게 분산된 Topic을 Partition이라고 한다. 어떤 이벤트가 Partition에 저장될지는 이벤트의 key(키)에 의해 정해지며, 같은 키를 가지는 이벤트는 항상 같은 Partition에 저장        
Kafka는 Topic의 Partition에 지정된 Consumer가 항상 정확히 동일한 순서로 Partition의 이벤트를 읽을것을 보장.

## 특징  
1. Producer/Consumer 분리       
Kafka의 Producer와 Consumer는 완전 별개로 동작, Producer는 Broker의 Topic에 메시지를 게시하기만 하면되며, Consumer는 Broker의 특정 Topic에서 메시지를 가져와 처리를 하기만 한다.        
이 덕분에 Kafka는 높은 확장성을 제공, Producer 또는 Consumer를 필요에 의해 스케일 인 아웃하기에 용이한 구조

2. Push 와 Pull 모델        
    1. 다양한 소비자의 처리 형태와 속도를 고려하지 않아도 된다.반대의 경우인 Push모델에서는, Broker가 데이터 전송 속도를 제어하기 때문에, 다양한 메시지 스트림의 소비자를 다루기가 어렵지만, Pull 모델은 Consumer가 처리 가능한 때에 메시지를 가져와 처리하기 때문에 다양한 소비자를 다루기가 쉽다.                                     

    2. 불필요한 지연없이 일괄처리를 통해 성능향상 도모.Push 모델의 경우에는, 요청을 즉시 보내거나, 더 많은 메시지를 한번에 처리하도록 하기 위해 Buffering을 할 수 있다. 하지만 이런 경우, Consumer가 현재 메시지를 처리할 수 있음에도, 대기를 해야한다. 그렇다고 전송 지연시간을 최소로 변경하면, 한번에 하나의 메시지만을 보내도록 하는것과 같으므로 매우 비효율적이다. pull 모델의 경우, 마지막으로 처리된 메시지 이후의 메시지를 Consumer가 처리가능한 때에 모두 가져오기 때문에, 이 문제를 해결함. 따라서 불필요한 지연 없이 최적의 일괄처리를 할 수 있습니다.
3.  소비된 메시지 추적 (Commit과 Offset)

    ![8](https://t1.daumcdn.net/cfile/tistory/9912C33B5FC70CB319)

     메세지는 지정된 Topic에 전달됩니다. Topic은 다시 여러 Partition으로 나뉠 수도 있습니다. 위 그림에서 각 파티션의 한칸한칸은 로그라고 칭합니다. 또한 메시지는 로그에 순차적으로 append 됩니다. 그리고 이 메시지의 상대적인 위치를 offset이라고 칭합니다.
    메시징 시스템은 Broker에서 소비된 메시지에 대한 메타데이터를 유지합니다. 즉, 메시지가 Consumer에게 전달되면 Broker는 이를 로컬에 기록하거나, 소비자의 승인을 기다립니다.

    Commit과 Offset

    Consumer의 poll()은 이전에 commit한 offset이 존재하면, 해당 offset 이후의 메시지를 읽어오게 됩니다. 또 읽어온 뒤, 마지막 offset을 commit을 합니다. 이어서 poll()이 실행되면 방금전 commit한 offset이후의 메시지를 읽어와 처리하게 됩니다.
    메시지 소비중에는 다음과 같은 문제들이 발생할 수도 있습니다.

    1. 소비된 메시지 기록시점

        Broker가 메시지를 네트워크를 통해 Consumer에게 전달할때 마다, 즉시, 소비 된 것으로 기록하면, Consumer가 메시지 처리를 실패하면 해당 메시지가 손실됩니다.

        이로 인해서, Broker는 메시지가 소비되었음을 기록하기 위해서, Consumer의 승인을 기다립니다. 하지만 이런식으로 메시지를 처리하게 되면 아래와 같은 문제점이 또 발생합니다.

    2. 중복 메시지 전송과 멱등성

        우선 Consumer가 메시지를 성공적으로 처리하고, 승인을 보내기전에 Broker가 실패하였다고 판단하고 다시 메시지를 보내게 되면, Consumer는 같은 메시지를 두번 처리하게 됩니다.

        따라서, Consumer는 멱등성을 고려하여야 합니다. 즉, 같은 메시지를 특수한 상황에 의해 여러번 받아서 여러번 처리하더라도, 한번 처리한것과 같은 결과를 가지도록 설계해야 합니다.
4. Consumer Group       
    Consumer Group은 하나의 Topic을 구독하는 여러 Consumer들의 모음입니다. Topic을 구독하는 Consumer들을 Group화 하는 이유는, 가용성 때문입니다. 하나의 Topic을 처리하는 Consumer가 1개인 것보다 여러개라면 당연히 가용성은 증가할 것입니다.

    아래에서 설명드릴 내용이지만, Consumer Group의 각 Consumer들은 하나의 Topic의 각기 다른 Partition의 내용만을 처리할 수 있는데요, 이를 통해서, Kafka는 메시지 처리 순서를 보장한다고 합니다. 이 때문에, 특정 Partition을 처리하던, Consumer가 처리 불가 상태가 된다면, 해당 Partition의 메시지를 처리할 수 없는 상태가 되어버립니다. 이때문에 Consumer Group이 필요합니다.

    Rebalance

    Partition을 담당하던 Consumer가 처리불가 상태가 되어버리면, Partition과 Consumer를 재조정하여, 남은 Consumer Group내의 Consumer들이 Partition을 적절하게 나누어 처리하게 됩니다.

    또한 Consumer Group내에서 Consumer들간에 Offset 정보를 공유하고 있기 때문에, 특정 Consumer가 처리불가 상태가 되었을때, 해당 Consumer가 처리한 마지막 Offset이후 부터 처리를 이어서 할 수 있습니다.

    이렇게 Partition을 나머지 Consumer들이 다시 나누어 처리하도록 하는 것을 Rebalance라고 하며, 이를 위해 Consumer Group이 필요합니다.

    Consumer 확장

    앞서 말씀드린, Consumer Group과 Partition의 관계에에 대해 알고 계셔야, Consumer의 성능 향상을 위한 확장을 제대로 하실수가 있습니다.     
![9](https://t1.daumcdn.net/cfile/tistory/9933DB485FC70CCC19)

    Consumer의 성능이 부족해, Consumer를 확장한다고 했을때, 앞서 설명드린것과 같이 Consumer Group내의 Consumer는 무조건 각기 다른 Partition에만 연결을 할 수 있다고 했습니다. 때문에, Consumer만을 확장하였고, 이때, Partition보다 Consumer의 수가 많으면 당연히, 새 Consumer는 놀게 됩니다.

![10](https://t1.daumcdn.net/cfile/tistory/997CE33F5FC70CD31A)

    따라서, Consumer를 확장할 때에는, Partition도 같이 늘려주어야 합니다.

5. 메시지(이벤트) 전달 컨셉
    kafka는 메시지 전달을 할때 보장하는 여러가지 방식이 있음

    At most once(최대 한번)
    메시지가 손실될 수 있지만, 재전달은 하지 않습니다.

    At least once(최소 한번)
    메시지가 손실되지 않지만, 재전달이 일어납니다.

    Exactly once(정확히 한번)
    메시지는 정확히 한번 전달이 됩니다.
6. 메세지를 여러 Consumer에게 허용 (EX. Hadoop,Serch Engine, Monitoring, Email)
7. 높은 처리량을 위한 메시지 최적화
8. Scale-out 가능
9. Eco-system

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



