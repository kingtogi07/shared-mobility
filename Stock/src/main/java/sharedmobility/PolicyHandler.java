package sharedmobility;

import sharedmobility.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired StockInfoRepository repository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverRented_StockChange(@Payload Rented rented){

        if(!rented.validate()) return;

        System.out.println("Notice : 재고 감소되었습니다.");

        StockInfo stockInfo = new StockInfo(); // 신규 생성
        stockInfo.setOrderId(rented.getOrderId());  // orderId 저장
        stockInfo.setRentId(rented.getRentId());  // orderId 저장
        stockInfo.setStockAmt(Long.valueOf("-1"));  // 렌트 상태 저장

        repository.save(stockInfo);
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverReturned_StockChange(@Payload Returned returned){
        
        if(!returned.validate()) return;

        System.out.println("Notice : 재고 증가하였습니다.");

        StockInfo stockInfo = new StockInfo(); // 신규 생성
        stockInfo.setOrderId(returned.getOrderId());  // orderId 저장
        stockInfo.setRentId(returned.getRentId());  // orderId 저장
        stockInfo.setStockAmt(Long.valueOf("1"));  // 렌트 상태 저장

        repository.save(stockInfo);
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
