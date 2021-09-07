![image](https://user-images.githubusercontent.com/31404198/125399943-366f8f80-e3ec-11eb-9e78-357dd4b19e01.png)

# 공유 모빌리티

- 체크포인트 : https://workflowy.com/s/assessment-check-po/T5YrzcMewfo4J6LW


# Table of contents

- [공유 모빌리티](#---)
  - [서비스 시나리오](#서비스-시나리오)
  - [체크포인트](#체크포인트)
  - [분석/설계](#분석설계)
  - [구현](#구현)
    - [게이트웨이 적용](#게이트웨이-적용)
    - [DDD 의 적용](#ddd-의-적용)
    - [동기식 호출 과 Fallback 처리](#동기식-호출-과-fallback-처리)
    - [비동기식 호출 과 Eventual Consistency](#비동기식-호출--시간적-디커플링--장애격리--최종-eventual-일관성-테스트)
    - [CQRS](#cqrs)
    - [폴리글랏 퍼시스턴스](#폴리글랏-퍼시스턴스)
  - [운영](#운영)
    - [Deploy / Pipeline](#deploy--pipeline)
    - [무정지 재배포(Readiness Probe)](#무정지-재배포readiness-probe)
    - [Self-healing(Liveness Probe)](#self-healing-liveness-probe)
    - [Config Map](#config-map)
    - [동기식 호출 / 서킷 브레이킹](#circuit-breaker)
    - [오토스케일 아웃](#오토스케일-아웃)


# 서비스 시나리오

기능적 요구사항
1. 사용자가 킥보드를 예약한다.
1. 예약한 킥보드에 대해서 결제한다.
1. 결제 후 사용승인이 되면 킥보드를 대여한다.
1. 킥보드가 대여가 되면 재고가 감소된다.
1. 사용자가 킥보드 예약을 취소한다.
1. 예약을 취소하면 결제가 취소된다.
1. 사용자가 킥보드를 반납한다.
1. 반납요청이 확인되면 재고가 증가한다.
1. 사용자는 대여상태를 대시보드에서 확인한다.

비기능적 요구사항
1. 트랜잭션
    1. 결제가 되지 않은 예약 건은 아예 거래가 성립되지 않아야 한다 -> Sync 호출
1. 장애격리
    1. 대여 기능이 수행되지 않더라도 예약는 365일 24시간 가능해야 한다 -> Async (event-driven), Eventual Consistency
    1. 결제시스템이 과중되면 사용자를 잠시동안 받지 않고 결제를 잠시후에 하도록 유도한다 -> Circuit breaker, fallback
1. 성능
    1. 사용자가 자주 예약관리에서 확인할 수 있는 예약상태를 예약시스템(프론트엔드)에서 확인할 수 있어야 한다 -> CQRS
    1. 예약상태에 따라 카톡 등으로 알림을 줄 수 있어야 한다 -> Event driven


# 체크포인트

- 분석 설계

  - 이벤트스토밍
    - 스티커 색상별 객체의 의미를 제대로 이해하여 헥사고날 아키텍처와의 연계 설계에 적절히 반영하고 있는가?
    - 각 도메인 이벤트가 의미있는 수준으로 정의되었는가?
    - 어그리게잇: Command와 Event 들을 ACID 트랜잭션 단위의 Aggregate 로 제대로 묶었는가?
    - 기능적 요구사항과 비기능적 요구사항을 누락 없이 반영하였는가?    
  - 서브 도메인, 바운디드 컨텍스트 분리
    - 팀별 KPI 와 관심사, 상이한 배포주기 등에 따른  Sub-domain 이나 Bounded Context 를 적절히 분리하였고 그 분리 기준의 합리성이 충분히 설명되는가?
      - 적어도 3개 이상 서비스 분리
    - 폴리글랏 설계: 각 마이크로 서비스들의 구현 목표와 기능 특성에 따른 각자의 기술 Stack 과 저장소 구조를 다양하게 채택하여 설계하였는가?
    - 서비스 시나리오 중 ACID 트랜잭션이 크리티컬한 Use 케이스에 대하여 무리하게 서비스가 과다하게 조밀히 분리되지 않았는가?
  - 컨텍스트 매핑 / 이벤트 드리븐 아키텍처 
    - 업무 중요성과  도메인간 서열을 구분할 수 있는가? (Core, Supporting, General Domain)
    - Request-Response 방식과 이벤트 드리븐 방식을 구분하여 설계할 수 있는가?
    - 장애격리: 서포팅 서비스를 제거 하여도 기존 서비스에 영향이 없도록 설계하였는가?
    - 신규 서비스를 추가 하였을때 기존 서비스의 데이터베이스에 영향이 없도록 설계(열려있는 아키택처)할 수 있는가?
    - 이벤트와 폴리시를 연결하기 위한 Correlation-key 연결을 제대로 설계하였는가?
  - 헥사고날 아키텍처
    - 설계 결과에 따른 헥사고날 아키텍처 다이어그램을 제대로 그렸는가?
    
- 구현

  - [DDD] 분석단계에서의 스티커별 색상과 헥사고날 아키텍처에 따라 구현체가 매핑되게 개발되었는가?
    - Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 데이터 접근 어댑터를 개발하였는가
    - [헥사고날 아키텍처] REST Inbound adaptor 이외에 gRPC 등의 Inbound Adaptor 를 추가함에 있어서 도메인 모델의 손상을 주지 않고 새로운 프로토콜에 기존 구현체를 적응시킬 수 있는가?
    - 분석단계에서의 유비쿼터스 랭귀지 (업무현장에서 쓰는 용어) 를 사용하여 소스코드가 서술되었는가?
  - Request-Response 방식의 서비스 중심 아키텍처 구현
    - 마이크로 서비스간 Request-Response 호출에 있어 대상 서비스를 어떠한 방식으로 찾아서 호출 하였는가? (Service Discovery, REST, FeignClient)
    - 서킷브레이커를 통하여  장애를 격리시킬 수 있는가?
  - 이벤트 드리븐 아키텍처의 구현
    - 카프카를 이용하여 PubSub 으로 하나 이상의 서비스가 연동되었는가?
    - Correlation-key:  각 이벤트 건 (메시지)가 어떠한 폴리시를 처리할때 어떤 건에 연결된 처리건인지를 구별하기 위한 Correlation-key 연결을 제대로 구현 하였는가?
    - Message Consumer 마이크로서비스가 장애상황에서 수신받지 못했던 기존 이벤트들을 다시 수신받아 처리하는가?
    - Scaling-out: Message Consumer 마이크로서비스의 Replica 를 추가했을때 중복없이 이벤트를 수신할 수 있는가
    - CQRS: Materialized View 를 구현하여, 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이) 도 내 서비스의 화면 구성과 잦은 조회가 가능한가?
  - 폴리글랏 플로그래밍
    - 각 마이크로 서비스들이 하나이상의 각자의 기술 Stack 으로 구성되었는가?
    - 각 마이크로 서비스들이 각자의 저장소 구조를 자율적으로 채택하고 각자의 저장소 유형 (RDB, NoSQL, File System 등)을 선택하여 구현하였는가?
  - API 게이트웨이
    - API GW를 통하여 마이크로 서비스들의 집입점을 통일할 수 있는가?
    - 게이트웨이와 인증서버(OAuth), JWT 토큰 인증을 통하여 마이크로서비스들을 보호할 수 있는가?

- 운영

  - SLA 준수
    - 셀프힐링: Liveness Probe 를 통하여 어떠한 서비스의 health 상태가 지속적으로 저하됨에 따라 어떠한 임계치에서 pod 가 재생되는 것을 증명할 수 있는가?
    - 서킷브레이커, 레이트리밋 등을 통한 장애격리와 성능효율을 높힐 수 있는가?
    - 오토스케일러 (HPA) 를 설정하여 확장적 운영이 가능한가?
    - 모니터링, 앨럿팅 
  - 무정지 운영 CI/CD (10)
    - Readiness Probe 의 설정과 Rolling update을 통하여 신규 버전이 완전히 서비스를 받을 수 있는 상태일때 신규버전의 서비스로 전환됨을 siege 등으로 증명 
    - Contract Test :  자동화된 경계 테스트를 통하여 구현 오류나 API 계약위반를 미리 차단 가능한가?


# 분석/설계


## AS-IS 조직 (Horizontally-Aligned)
  ![image](https://user-images.githubusercontent.com/31404198/125080475-ccab6900-e0ff-11eb-819f-7fdd7c12d9d6.png)

## TO-BE 조직 (Vertically-Aligned)
  ![image](https://user-images.githubusercontent.com/31404198/125080676-0ed4aa80-e100-11eb-8707-e7a178193d14.png)

## Event Storming 결과
* MSAEz 로 모델링한 이벤트스토밍 결과:  http://www.msaez.io/#/storming/qTPVkyZojONcrS0xJzeIbYjPXMl1/385eb70fafd285bf582522ab97f45e92


### 이벤트 도출
![image](https://user-images.githubusercontent.com/30138356/132278885-21b80f61-1acf-4dbf-b1fa-baf4750ee152.PNG)

### 부적격 이벤트 탈락
![image](https://user-images.githubusercontent.com/30138356/132279000-ca1a4e60-c83a-4854-ba8a-c63834683e47.PNG)

    - 과정중 도출된 잘못된 도메인 이벤트들을 걸러내는 작업을 수행함

### 액터, 커맨드 부착하여 읽기 좋게
![image](https://user-images.githubusercontent.com/30138356/132279314-a88ec597-14f3-4ac6-aa7c-b197a194159a.PNG)

### 어그리게잇으로 묶기
![image](https://user-images.githubusercontent.com/30138356/132279320-a994d99c-cf2c-483a-9764-46c3963fd6ec.PNG)

    - 예약, 대여처리, 결제정보, 재고는 그와 연결된 command 와 event 들에 의하여 트랜잭션이 유지되어야 하는 단위로 그들 끼리 묶어줌

### 바운디드 컨텍스트로 묶기

![image](https://user-images.githubusercontent.com/30138356/132279332-1318d543-7bff-48af-a2a7-394e095232cc.PNG)

    - 도메인 서열 분리 
        - Core Domain:  예약(front), 대여 : 없어서는 안될 핵심 서비스이며, 연견 Up-time SLA 수준을 99.999% 목표, 배포주기는 예약의 경우 1주일 1회 미만, 대여의 경우 1개월 1회 미만
        - Supporting Domain: 재고   : 경쟁력을 내기위한 서비스이며, SLA 수준은 연간 60% 이상 uptime 목표, 배포주기는 각 팀의 자율이나 표준 스프린트 주기가 1주일 이므로 1주일 1회 이상을 기준으로 함.
        - General Domain:   결제 : 결제서비스로 3rd Party 외부 서비스를 사용하는 것이 경쟁력이 높음 (핑크색으로 이후 전환할 예정)

### 폴리시 부착 (괄호는 수행주체, 폴리시 부착을 둘째단계에서 해놔도 상관 없음. 전체 연계가 초기에 드러남)

![image](https://user-images.githubusercontent.com/30138356/132279347-36b5ac95-ffc7-4e05-b172-8e0e04fabe3c.PNG)

### 폴리시의 이동과 컨텍스트 매핑 (점선은 Pub/Sub, 실선은 Req/Resp)

![image](https://user-images.githubusercontent.com/30138356/132279364-889d41f4-0354-4e2e-a97d-b4738bb66bcc.PNG)

### 완성된 1차 모형

![image](https://user-images.githubusercontent.com/30138356/132279371-14fab015-10a4-485f-9711-684a7855dae2.PNG)

    - View Model 추가

### 1차 완성본에 대한 기능적/비기능적 요구사항을 커버하는지 검증

![image](https://user-images.githubusercontent.com/30138356/132279384-29ab955b-b06c-45b4-9544-cf2cac7bd0e8.PNG)

    - 사용자가 킥보드 선택 후 예약한다. (ok)
    - 예약한 킥보드에 대해서 결제한다. (ok)
    - 결제 후 사용승인이 되면 킥보드를 대여한다. (ok)
    - 킥보드가 대여가 되면 재고가 감소된다. (ok)

![image](https://user-images.githubusercontent.com/30138356/132279391-39f600c6-124c-4bfe-bd31-37b7d10e524c.PNG)

    - 사용자가 킥보드 예약을 취소한다. (ok)
    - 예약을 취소하면 결제가 취소된다. (ok)

![image](https://user-images.githubusercontent.com/30138356/132279404-a2755d06-28ac-455d-bfbd-fe5092300591.PNG)

    - 사용자가 킥보드를 반납한다. (ok)
    - 반납요청이 확인되면 재고가 증가한다. (ok)
    - 사용자는 대여상태를 대시보드에서 확인한다. (View-green sticker 의 추가로 ok)

### 비기능 요구사항에 대한 검증

![image](https://user-images.githubusercontent.com/30138356/132279412-456fa535-eb57-4064-9eb0-ca12f308bbb6.PNG)

    - 마이크로 서비스를 넘나드는 시나리오에 대한 트랜잭션 처리
    - 고객 예약시 결제처리:  결제가 완료되지 않은 예약은 절대 대여를 할 수 없기 때문에, ACID 트랜잭션 적용. 예약완료시 결제처리에 대해서는 Request-Response 방식 처리
    - 결제 완료시 대여연결 및 재고처리:  예약(front)에서 대여 마이크로서비스로 대여요청이 전달되는 과정에 있어서 대여 마이크로 서비스가 별도의 배포주기를 가지기 때문에 Eventual Consistency 방식으로 트랜잭션 처리함.
    - 나머지 모든 inter-microservice 트랜잭션: 예약상태, 대여상태 등 모든 이벤트에 대해 카톡을 처리하는 등, 데이터 일관성의 시점이 크리티컬하지 않은 모든 경우가 대부분이라 판단, Eventual Consistency 를 기본으로 채택함.

## 헥사고날 아키텍처 다이어그램 도출
    
![image](https://user-images.githubusercontent.com/30138356/132279422-474f5e99-5237-4875-95fd-7a57c1f01aa9.PNG)

    - Chris Richardson, MSA Patterns 참고하여 Inbound adaptor와 Outbound adaptor를 구분함
    - 호출관계에서 PubSub 과 Req/Resp 를 구분함
    - 서브 도메인과 바운디드 컨텍스트의 분리:  각 팀의 KPI 별로 아래와 같이 관심 구현 스토리를 나눠가짐

# 구현

- 분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 808n 이다)
```
  cd order
  mvn spring-boot:run

  cd payment
  mvn spring-boot:run 

  cd rent
  mvn spring-boot:run  

  cd stock
  mvn spring-boot:run 

  cd gateway
  mvn spring-boot:run
```

## 게이트웨이 적용
```yml
spring:
  profiles: default
  cloud:
    gateway:
      routes:
        - id: Order
          uri: http://localhost:8081
          predicates:
            - Path=/order/** 
        - id: Stock
          uri: http://localhost:8082
          predicates:
            - Path=/stock/** 
        - id: Payment
          uri: http://localhost:8083
          predicates:
            - Path=/payment/** 
        - id: Dashboard
          uri: http://localhost:8084
          predicates:
            - Path= /dashboard/**
        - id: Rent
          uri: http://localhost:8085
          predicates:
            - Path=/rent/** 
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true


---

spring:
  profiles: docker
  cloud:
    gateway:
      routes:
        - id: order
          uri: http://order:8080
          predicates:
            - Path=/order/** 
        - id: stock
          uri: http://stock:8080
          predicates:
            - Path=/stock/** 
        - id: payment
          uri: http://payment:8080
          predicates:
            - Path=/payment/** 
        - id: dashboard
          uri: http://dashboard:8080
          predicates:
            - Path= /dashboard/**
        - id: rent
          uri: http://rent:8080
          predicates:
            - Path=/rent/** 
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true
```
- gateway Service yml 에 loadBalancer 적용
```yml
apiVersion: v1
kind: Service
metadata:
  name: gateway
  labels:
    app: gateway
spec:
  ports:
    - port: 8080
      targetPort: 8080
  selector:
    app: gateway
  type: LoadBalancer

```
- 적용 이미지
![게이트웨이](https://user-images.githubusercontent.com/30138356/125386847-edaddb80-e3d7-11eb-9738-5c8904b3a28e.PNG)

## DDD 의 적용

- 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언하였다: (예시는 PaymentInfo 마이크로 서비스). 이때 가능한 현업에서 사용하는 언어 (유비쿼터스 랭귀지)를 그대로 사용하였다. 
``` JAVA
  package sharedmobility;

  import javax.persistence.*;
  import org.springframework.beans.BeanUtils;

  @Entity
  @Table(name="PaymentInfo_table")
  public class PaymentInfo {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long payId;
    private Long orderId;
    private Long price;
    private String payDate;
    private String payStatus;
    private String payCancelDate;
    private Long customerId;

    @PostPersist
    public void onPostPersist(){
        // 결제 완료 후 KAFKA 전송
        if(this.payStatus == "PAIED"){
            PaymentApproved paymentApproved = new PaymentApproved();
            BeanUtils.copyProperties(this, paymentApproved);
            paymentApproved.publishAfterCommit();
        }

    }
    @PostUpdate
    public void onPostUpdate(){
        if(this.payStatus == "CANCEL"){
        PaymentCanceled paymentCanceled = new PaymentCanceled();
        BeanUtils.copyProperties(this, paymentCanceled);
        paymentCanceled.publishAfterCommit();
        }
    }

    public Long getPayId() {
        return payId;
    }
    public void setPayId(Long payId) {
        this.payId = payId;
    }
    public Long getOrderId() {
        return orderId;
    }
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    public Long getPrice() {
        return price;
    }
    public void setPrice(Long price) {
        this.price = price;
    }
    public String getPayDate() {
        return payDate;
    }
    public void setPayDate(String payDate) {
        this.payDate = payDate;
    }
    public String getPayStatus() {
        return payStatus;
    }
    public void setPayStatus(String payStatus) {
        this.payStatus = payStatus;
    }
    public String getPayCancelDate() {
        return payCancelDate;
    }
    public void setPayCancelDate(String payCancelDate) {
        this.payCancelDate = payCancelDate;
    }

    public Long getCustomerId() {
        return customerId;
    }
    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }
  }

```
- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다
```JAVA
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="orderInfos", path="orderInfos")
public interface OrderInfoRepository extends PagingAndSortingRepository<OrderInfo, Long>{
    List<OrderInfo> findByOrderId(Long orderId);
}

```
### 적용 후 REST API 의 테스트

  - 사용신청(order) 발생 시, req/res 방식으로 결제(payment) 서비스를 호출하고 결제 완료 후 발생하는 PayApproved Event 가 카프카로 송출된다. 
```
  # orderInfo 서비스의 킥보드 사용 신청(주문) 
  http POST http://a3649a0c9c28b482c85ab06fe0a8a7f4-1255737767.ap-northeast-2.elb.amazonaws.com:8080/order orderId=100 customerId=99
```  
  ![order](https://user-images.githubusercontent.com/30138356/125385992-82afd500-e3d6-11eb-9f7a-64451dd0931f.PNG)
```
  # 주문 후 결제 상태 확인 ( payStatus = PAID )
  http http://a3649a0c9c28b482c85ab06fe0a8a7f4-1255737767.ap-northeast-2.elb.amazonaws.com:8080/payment/1
```
  ![Payment 상태](https://user-images.githubusercontent.com/30138356/125385995-83486b80-e3d6-11eb-9bac-c9c8b175f72b.PNG)


  - PayApproved 를 수신한 렌트(rent) 서비스가 전달받은 OrderId 로 렌트승인(APPROVE) 상태인 데이터를 생성한다.
  ```
  # 주문 후 결제 상태 확인 ( rentStatus = APPROVE )
  http http://a3649a0c9c28b482c85ab06fe0a8a7f4-1255737767.ap-northeast-2.elb.amazonaws.com:8080/rent/100
  ```
  ![rent 상태](https://user-images.githubusercontent.com/30138356/125385996-83e10200-e3d6-11eb-94d5-ff5dad5431bf.PNG)

  - 이후 렌트승인 상태인 OrderId 에 대해 렌트신청 할 경우, 렌트(RENT) 상태로 변경되며 rent Event 가 카프카로 송출된다.
```
# 렌트 신청 ( rentStatus = APPROVE 상태가 아니면 렌트 불가, 렌트 성공 시, rentStatus = RENT 로 변경 )
  http PUT http://a3649a0c9c28b482c85ab06fe0a8a7f4-1255737767.ap-northeast-2.elb.amazonaws.com:8080/rent/100
```
  ![rent 후 rent 상태](https://user-images.githubusercontent.com/30138356/125386338-11bced00-e3d7-11eb-9e10-0a1b051706fc.PNG)

- 재고(stock) 서비스에서는 해당 rent Event 수신 후, 재고차감 이력을 기록한다. 
```
  # 렌트 후 Rent Event 수신한 Stock 서비스의 재고 차감 확인 ( 재고 차감/증가 이력만 남김 )
  ```
  ![재고이력소스](https://user-images.githubusercontent.com/30138356/125386433-40d35e80-e3d7-11eb-81df-06e1ddf8d29d.PNG)
```
  # 재고 차감 내역 콘솔에서 확인
```
  ![8](https://user-images.githubusercontent.com/30138356/125185587-a81ad280-e260-11eb-99d6-307c009821ca.PNG)

## Correlation-key
- 사용 반납 작업을 통해, Correlation-key 연결을 검증한다

```
# 사용 신청 
```
![사용신청된Order](https://user-images.githubusercontent.com/30138356/125393664-53539500-e3e3-11eb-9d64-ee001b5ab887.PNG)
```
# 렌트 신청 
```
![렌트처리](https://user-images.githubusercontent.com/30138356/125393661-52bafe80-e3e3-11eb-9cd7-62c22ff4b225.PNG)
```
# 반납 처리
```
![반납처리](https://user-images.githubusercontent.com/30138356/125393660-52bafe80-e3e3-11eb-99d0-0e405e39bfc3.PNG)
```
# 사용신청 내역과 렌트 내역 확인 ( 상태가 RETURN 으로 변경됨 ) 
```
![오더와 렌트상태](https://user-images.githubusercontent.com/30138356/125393657-5189d180-e3e3-11eb-91fb-3df9210e4a86.PNG)

## 동기식 호출 과 Fallback 처리
- 분석단계에서의 조건 중 하나로 사용신청(orderInfo)->결제(paymentInfo) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 
호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다.

결제서비스를 호출하기 위하여 Stub과 (FeignClient) 를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현 ( url 은 Config Map 적용 )
``` JAVA
# (orderInfo) PaymentInfoService.java

@FeignClient(name="payment", url="http://${api.url.order}")
public interface PaymentInfoService {
    @RequestMapping(method= RequestMethod.POST, path="/payment")
    public boolean pay(@RequestBody PaymentInfo paymentInfo);
}
```
- 사용신청 직후(@PostPersist) 결제를 요청하도록 처리
``` JAVA
# OrderInfo.java (Entity)

  // 해당 엔티티 저장 후
  @PostPersist
  public void onPostPersist(){

      // 사용 주문 들어왔을 경우
      if("USE".equals(this.orderStatus)){
          // 결제 진행
          PaymentInfo paymentInfo = new PaymentInfo();
          paymentInfo.setOrderId(this.orderId);
          paymentInfo.setPrice(this.price);
          paymentInfo.setCustomerId(this.customerId);

          OrderApplication.applicationContext.getBean(PaymentInfoService.class)
              .pay(paymentInfo);

          /*
              Kafka 송출
          */
          Ordered ordered = new Ordered();
          BeanUtils.copyProperties(this, ordered);
          ordered.publishAfterCommit();   // ordered 카프카 송출
      }
  }
```
- 동기식 호출에서는 호출 시간에 따른 타임 커플링이 발생하며, 결제 시스템이 장애가 나면 주문도 못받는다는 것을 확인:
```
  # 결제(paymentSystem) 서비스를 잠시 내려놓음

  # 사용 신청 처리
  http POST localhost:8088/order customerId=11 time=3 orderId=20  # Fail
```
![12](https://user-images.githubusercontent.com/30138356/125189944-aa3b5c00-e275-11eb-81c2-514085209b99.PNG)
```
  # 결제서비스 재기동
  cd payment
  mvn spring-boot:run

  # 사용 신청 처리
  http POST localhost:8088/order customerId=11 time=3 orderId=20  #Success
```
![13](https://user-images.githubusercontent.com/30138356/125189975-cfc86580-e275-11eb-9b0c-dec97c2ede61.PNG)

과도한 요청시에 서비스 장애 벌어질 수 있음에 유의

## 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트
결제가 이루어진 후에 렌트승인 시스템으로 이를 알려주는 행위는 동기식이 아니라 비동기식으로 처리하여 대여를 위하여 결제가 블로킹 되지 않도록 처리한다.

이를 위하여 결제시스템에 기록을 남긴 후에 곧바로 결제완료이 되었다는 도메인 이벤트를 카프카로 송출한다(Publish)
``` JAVA
  ...
    @PostPersist
    public void onPostPersist(){
        // 결제 완료 후 KAFKA 전송
        if(this.payStatus == "PAIED"){
            PaymentApproved paymentApproved = new PaymentApproved();
            BeanUtils.copyProperties(this, paymentApproved);
            paymentApproved.publishAfterCommit();
        }

    }
```
렌트승인 서비스에서는 결제완료 이벤트에 대해서 이를 수신하여 자신의 정책을 처리하도록 PolicyHandler 를 구현한다:
``` JAVA
public class PolicyHandler{
 ...
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaymentApproved_Approve(@Payload PaymentApproved paymentApproved){
        // 유휴 킥보드에 접근하여 해당 Order ID 의 렌트승인 상태로 변경
        // 렌트 승인 상태인 Order Id 는 기기 접근 시 승인 처리됨.
        if(!paymentApproved.validate()) return;

        System.out.println("\n\n##### listener Approve : " + paymentApproved.toJson() + "\n\n");
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat ("yyyy-MM-dd hh:mm:ss");
        String today =  sdf.format(timestamp);

        // 결제 승인 시, 렌트 가능한 상태로 변경
        RentInfo rentInfo = new RentInfo(); // 신규 생성
        rentInfo.setOrderId(paymentApproved.getOrderId());  // orderId 저장
        rentInfo.setRentStatus("APPROVE");  // 렌트 상태 저장
        rentInfo.setApproveDate(today);  // 승인 날짜

        rentInfoRepository.save(rentInfo);
    }
```
렌트승인 시스템은 사용신청/결제와 완전히 분리되어있으며, 이벤트 수신에 따라 처리되기 때문에, 렌트승인이 유지보수로 인해 잠시 내려간 상태라도 사용신청을 받는데 문제가 없다:
```
# 렌트승인 서비스 (lectureSystem) 를 잠시 내려놓음
# 사용신청 처리 후 사용신청 및 결제 처리 Event 진행확인
```
![9](https://user-images.githubusercontent.com/30138356/125189677-3fd5ec00-e274-11eb-9aee-f68b40516ce7.PNG)
![10](https://user-images.githubusercontent.com/30138356/125189710-6e53c700-e274-11eb-9cdf-8c1c66830a35.PNG)
```
# 렌트승인 서비스 기동
cd rent
mvn spring-boot:run

# 렌트 상태 Update 확인
```
![11](https://user-images.githubusercontent.com/30138356/125189746-9fcc9280-e274-11eb-8ede-260754fa66d9.PNG)


## CQRS

- CQRS: Materialized View 를 구현하여, 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이) 도 내 서비스의 화면 구성과 잦은 조회가 가능하도록 구현한다

주문 / 결제 / 렌트 서비스의 전체 현황 및 상태 조회를 제공하기 위해 dashboard를 구성하였다.

dashboard의 어트리뷰트는 다음과 같으며

![image](https://user-images.githubusercontent.com/22028798/125186287-79066000-e264-11eb-94a6-ee4a85aa8851.png)

ordered, paymentApproved, canceled, returned, paymentCanceled 이벤트에 따라 주문상태, 반납상태, 취소상태를 업데이트 하는 모델링을 진행하였다.

자동생성된 소스 샘플은 아래와 같다
Dashboard.java
``` JAVA
package sharedmobility;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name="Dashboard_table")
public class Dashboard {

        @Id
        @GeneratedValue(strategy=GenerationType.AUTO)
        private Long dashboardId;
        private Long customerId;
        private Long orderId;
        private Long paymentId;
        private Long rentId;
        private String payStatus;
        private String orderStatus;
        private String orderDate;
        private String cancelDate;
        private String returnDate;
        private String payDate;
        private Long price;
        private String payCancelDate;


        public Long getDashboardId() {
            return dashboardId;
        }

        public void setDashboardId(Long dashboardId) {
            this.dashboardId = dashboardId;
        }
        public Long getCustomerId() {
            return customerId;
        }

        public void setCustomerId(Long customerId) {
            this.customerId = customerId;
        }
        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }
        public Long getPaymentId() {
            return paymentId;
        }

        public void setPaymentId(Long paymentId) {
            this.paymentId = paymentId;
        }
        public Long getRentId() {
            return rentId;
        }

        public void setRentId(Long rentId) {
            this.rentId = rentId;
        }
        public String getPayStatus() {
            return payStatus;
        }

        public void setPayStatus(String payStatus) {
            this.payStatus = payStatus;
        }
        public String getOrderStatus() {
            return orderStatus;
        }

        public void setOrderStatus(String orderStatus) {
            this.orderStatus = orderStatus;
        }
        public String getOrderDate() {
            return orderDate;
        }

        public void setOrderDate(String orderDate) {
            this.orderDate = orderDate;
        }
        public String getCancelDate() {
            return cancelDate;
        }

        public void setCancelDate(String cancelDate) {
            this.cancelDate = cancelDate;
        }
        public String getReturnDate() {
            return returnDate;
        }

        public void setReturnDate(String returnDate) {
            this.returnDate = returnDate;
        }
        public String getPayDate() {
            return payDate;
        }

        public void setPayDate(String payDate) {
            this.payDate = payDate;
        }
        public Long getPrice() {
            return price;
        }

        public void setPrice(Long price) {
            this.price = price;
        }
        public String getPayCancelDate() {
            return payCancelDate;
        }

        public void setPayCancelDate(String payCancelDate) {
            this.payCancelDate = payCancelDate;
        }

}
```
DashboardRepository.java
```JAVA
package sharedmobility;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DashboardRepository extends CrudRepository<Dashboard, Long> {

    List<Dashboard> findByOrderId(Long orderId);

}
```
DashboardViewHandler.java
```JAVA
package sharedmobility;

import sharedmobility.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class DashboardViewHandler {


    @Autowired
    private DashboardRepository dashboardRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whenOrdered_then_CREATE_1 (@Payload Ordered ordered) {
        try {

            if (!ordered.validate()) return;

            // view 객체 생성
            Dashboard dashboard = new Dashboard();
            // view 객체에 이벤트의 Value 를 set 함
            dashboard.setOrderId(ordered.getOrderId());
            dashboard.setOrderStatus(ordered.getOrderStatus());
            dashboard.setCustomerId(ordered.getCustomerId());
            dashboard.setOrderDate(ordered.getOrderDate());
            // view 레파지 토리에 save
            dashboardRepository.save(dashboard);


        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whenPaymentApproved_then_UPDATE_1(@Payload PaymentApproved paymentApproved) {
        try {
            if (!paymentApproved.validate()) return;
                // view 객체 조회
                    List<Dashboard> dashboardList = dashboardRepository.findByOrderId(paymentApproved.getOrderId());
                    for(Dashboard dashboard : dashboardList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    dashboard.setPaymentId(paymentApproved.getPayId());
                    dashboard.setPayDate(paymentApproved.getPayDate());
                    dashboard.setPayStatus(paymentApproved.getPayStatus());
                    dashboard.setPrice(paymentApproved.getPrice());
                // view 레파지 토리에 save
                dashboardRepository.save(dashboard);
                }

        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenCanceled_then_UPDATE_2(@Payload Canceled canceled) {
        try {
            if (!canceled.validate()) return;
                // view 객체 조회

                    List<Dashboard> dashboardList = dashboardRepository.findByOrderId(canceled.getOrderId());
                    for(Dashboard dashboard : dashboardList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    dashboard.setOrderStatus(canceled.getOrderStatus());
                    dashboard.setCancelDate(canceled.getCancelDate());
                // view 레파지 토리에 save
                dashboardRepository.save(dashboard);
                }

        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenReturned_then_UPDATE_3(@Payload Returned returned) {
        try {
            if (!returned.validate()) return;
                // view 객체 조회

                    List<Dashboard> dashboardList = dashboardRepository.findByOrderId(returned.getOrderId());
                    for(Dashboard dashboard : dashboardList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    dashboard.setOrderStatus(returned.getOrderStatus());
                    dashboard.setReturnDate(returned.getReturnDate());
                // view 레파지 토리에 save
                dashboardRepository.save(dashboard);
                }

        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenPaymentCanceled_then_UPDATE_4(@Payload PaymentCanceled paymentCanceled) {
        try {
            if (!paymentCanceled.validate()) return;
                // view 객체 조회

                    List<Dashboard> dashboardList = dashboardRepository.findByOrderId(paymentCanceled.getOrderId());
                    for(Dashboard dashboard : dashboardList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    dashboard.setPayStatus(paymentCanceled.getPayStatus());
                    dashboard.setPayCancelDate(paymentCanceled.getPayCancelDate());
                // view 레파지 토리에 save
                dashboardRepository.save(dashboard);
                }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
```
CQRS에 대한 테스트는 아래와 같다
주문생성 시 주문 및 결제까지 정상적으로 수행 및 등록이 되며
![image](https://user-images.githubusercontent.com/22028798/125186608-465d6700-e266-11eb-863e-3403c96f5782.png)

dashbaord CQRS 결과는 아래와 같다

![image](https://user-images.githubusercontent.com/22028798/125186621-5d03be00-e266-11eb-85a6-58cede9ce417.png) 

## 폴리글랏 퍼시스턴스
- CQRS 를 위한 Dashboard 서비스만 DB를 구분하여 적용함. 인메모리 DB인 hsqldb 사용.
```xml
		<!-- <dependency>
			<groupId>com.h2database</groupId>
			<artifactId>h2</artifactId>
			<scope>runtime</scope>
		</dependency> -->

		<dependency>
		    <groupId>org.hsqldb</groupId>
		    <artifactId>hsqldb</artifactId>
		    <version>2.4.0</version>
		    <scope>runtime</scope>
		</dependency>
```
- 변경 후에도 정상 구동됨을 확인
![구동확인1](https://user-images.githubusercontent.com/30138356/125391898-2782e000-e3e0-11eb-8f50-5c1a3ff963f8.PNG)
![구동확인](https://user-images.githubusercontent.com/30138356/125391896-2651b300-e3e0-11eb-9be6-2410b0e51e49.PNG)

# 운영

## Deploy / Pipeline
각 구현체 들의 pipeline build script 는 shared-mobility/kubernetes/sharedmobility 내 
포함되어 있다. ( ex. order.yml )

- Build 및 ECR 에 Build/Push 하기
```
# order
cd Order
mvn package
docker build -t 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user01-order:latest .
docker push 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user01-order:latest

# payment
cd ..
cd Payment
mvn package
docker build -t 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user01-payment:latest .
docker push 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user01-payment:latest

# rent
cd ..
cd Rent
mvn package
docker build -t 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user01-rent:latest .
docker push 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user01-rent:latest

# stock
cd ..
cd Stock
mvn package
docker build -t 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user01-stock:latest .
docker push 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user01-stock:latest

# dashboard
cd ..
cd Dashboard
mvn package
docker build -t 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user01-dashboard:latest .
docker push 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user01-dashboard:latest

# gateway
cd ..
cd gateway
mvn package
docker build -t 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user01-gateway:latest .
docker push 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user01-gateway:latest
```

- Kubernetes Deploy 및 Service 생성
```
cd ..
kubectl apply  -f kubernetes/sharedmobility/order.yml
kubectl apply  -f kubernetes/sharedmobility/payment.yml
kubectl apply  -f kubernetes/sharedmobility/rent.yml
kubectl apply  -f kubernetes/sharedmobility/stock.yml
kubectl apply  -f kubernetes/sharedmobility/dashboard.yml
kubectl apply  -f kubernetes/sharedmobility/gateway.yml
```

- kubernetes/sharedmobility/order.yml 파일
```YML
---

apiVersion: apps/v1
kind: Deployment
metadata:
  name: order
  labels:
    app: order
spec:
  replicas: 1
  selector:
    matchLabels:
      app: order
  template:
    metadata:
      labels:
        app: order
    spec:
      containers:
        - name: order
          image: 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user01-order:latest
          ports:
            - containerPort: 8080
          env:
            - name: ORDER-URL
              valueFrom:
                configMapKeyRef:
                  name: order-configmap
                  key: order-url
          resources:
            requests:
              memory: "64Mi"
              cpu: "250m"
            limits:
              memory: "500Mi"
              cpu: "500m"                     
          readinessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 10
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 10
          livenessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 120
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 5


---


apiVersion: v1
kind: Service
metadata:
  name: order
  labels:
    app: order
spec:
  ports:
    - port: 8080
      targetPort: 8080
  selector:
    app: order


---


apiVersion: v1
kind: ConfigMap
metadata:
  name: order-configmap
data:
  order-url: payment:8080
```

- Deploy 완료
![image](https://user-images.githubusercontent.com/30138356/125383175-f7ccdb80-e3d1-11eb-81c5-522009d5a4ce.PNG)


## 무정지 재배포(Readiness Probe)
- 현재 정상적으로 동작중인 상황 확인

![image](https://user-images.githubusercontent.com/22028798/125400383-c6add480-e3ec-11eb-8e0b-aeddf0a0c8fb.png)

- order.yml 파일에 Readiness Probe 부분 설정

![image](https://user-images.githubusercontent.com/22028798/125400485-e5ac6680-e3ec-11eb-92ca-96c3abe91876.png)

- 디플로이 시작

![image](https://user-images.githubusercontent.com/22028798/125400543-fc52bd80-e3ec-11eb-8dc6-1a8ac53ef31d.png)

- siege로 부하 시작 -> 가용률 100% 확인

![image](https://user-images.githubusercontent.com/22028798/125400628-18565f00-e3ed-11eb-9c9c-ea4c64c6717d.png)


## Self-healing (Liveness Probe)
- deployment.yml에 정상 적용되어 있는 livenessProbe

![image](https://user-images.githubusercontent.com/22028798/125394269-69ae2080-e3e4-11eb-9611-3a79a072cdfc.png)

- 정상작동 중 확인

![image](https://user-images.githubusercontent.com/22028798/125394378-906c5700-e3e4-11eb-9728-ed329ef46efc.png)

- 포트 및 경로 잘못된 값으로 변경 후 retry 시도 확인

![image](https://user-images.githubusercontent.com/22028798/125394475-b1cd4300-e3e4-11eb-8c80-d953e29bed0c.png)
![image](https://user-images.githubusercontent.com/22028798/125394858-4c2d8680-e3e5-11eb-98d6-3a7e3409bb6e.png)


## Config Map

- 변경 가능성이 있는 설정을 ConfigMap을 사용하여 관리  
  - order 서비스에서 바라보는 payment 서비스 url 일부분을 ConfigMap 사용하여 구현​  

- order 서비스 내 FeignClient (order/src/main/java/sharedmobility/external/PaymentInfoService.java)
```java
@FeignClient(name="payment", url="http://${api.url.order}")
public interface PaymentInfoService {
    @RequestMapping(method= RequestMethod.POST, path="/payment")
    public boolean pay(@RequestBody PaymentInfo paymentInfo);

}
```

- order 서비스 application.yml
```yml
api: 
  url: 
    order: ${order-url}
```

- order 서비스 order.yml
```yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order
  labels:
    app: order
spec:
  -- 생략 --
          env:
            - name: ORDER-URL
              valueFrom:
                configMapKeyRef:
                  name: order-configmap
                  key: order-url         
  -- 생략 --
apiVersion: v1
kind: ConfigMap
metadata:
  name: order-configmap
data:
  order-url: payment:8080
```

- 적용 후 상세내역 확인 가능
![KakaoTalk_20210713_132118829](https://user-images.githubusercontent.com/30138356/125390117-4469e400-e3dd-11eb-991e-a5731893f401.png)


## Circuit Breaker
- 서킷 브레이킹 프레임워크 선택 : Hystrix 옵션을 사용하여 구현함
시나리오는 사용신청(order)-->결제(payment) 시 RESTful Request/Response 로 구현되어 있고 
결제 요청이 과도할 경우 CB 를 통하여 장애격리.

- Hystrix 를 설정: 요청처리 쓰레드에서 처리시간이 610 밀리가 넘어서기 시작하여 어느정도 유지되면 CB 회로가 닫히도록 (요청을 빠르게 실패처리, 차단) 설정
```yml
# order.yml

hystrix:
  command:
    # 전역설정
    default:
      execution.isolation.thread.timeoutInMilliseconds: 610
```

- 피호출 서비스(결제:payment) 의 부하 처리
```JAVA
    @PostPersist
    public void onPostPersist(){
        // 부하테스트 주석
        try {
            Thread.currentThread().sleep((long) (400 + Math.random() * 220));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 결제 완료 후 KAFKA 전송
        if(this.payStatus == "PAIED"){
            PaymentApproved paymentApproved = new PaymentApproved();
            BeanUtils.copyProperties(this, paymentApproved);
            paymentApproved.publishAfterCommit();
        }
    }
```

- siege 툴을 통한 서킷 브레이커 동작 확인
![image](https://user-images.githubusercontent.com/30138356/125381495-f6e67a80-e3ce-11eb-85fc-d6b454018209.PNG)
![image](https://user-images.githubusercontent.com/30138356/125381513-006fe280-e3cf-11eb-9323-fe7775b8b1b4.PNG)

## 오토스케일 아웃
- 결제 서비스에 대한 Replica를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 15%를 넘어서면 Replica 를 10개까지 늘려준다.

- (오토스케일 미적용 시) siege -c10 -t105 -r10 -v --content-type "application/json" 'http://gateway:8080/order POST {"orderId" : 1, "customerId": 1}'
![KakaoTalk_20210713_115034531](https://user-images.githubusercontent.com/30138356/125382776-43cb5080-e3d1-11eb-946e-381e02d18f79.png)

- (오토스케일 적용) kubectl autoscale deploy payment --min=1 --max=10 --cpu-percent=15
![KakaoTalk_20210713_114857158](https://user-images.githubusercontent.com/30138356/125382849-69585a00-e3d1-11eb-95cf-cf69d29b5a44.png)

- (오토스케일 적용 결과) siege -c10 -t105 -r10 -v --content-type "application/json" 'http://gateway:8080/order POST {"orderId" : 1, "customerId": 1}'
![KakaoTalk_20210713_114858955](https://user-images.githubusercontent.com/30138356/125382970-9a388f00-e3d1-11eb-9dd8-9c8359f79433.png)
![KakaoTalk_20210713_114900577](https://user-images.githubusercontent.com/30138356/125382972-9ad12580-e3d1-11eb-8e54-7811f98966b8.png)
