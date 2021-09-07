package sharedmobility;

import sharedmobility.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired MessageInfoRepository messageInfoRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverApproved_Send(@Payload Approved approved){

        if(!approved.validate()) return;

        System.out.println("Notice : 고객에게 알림톡 발송");



        // Sample Logic //
        // MessageInfo messageInfo = new MessageInfo();
        // messageInfoRepository.save(messageInfo);

    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverRented_Send(@Payload Rented rented){

        if(!rented.validate()) return;

        System.out.println("Notice : 고객에게 알림톡 발송");



        // Sample Logic //
        // MessageInfo messageInfo = new MessageInfo();
        // messageInfoRepository.save(messageInfo);

    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverReturned_Send(@Payload Returned returned){

        if(!returned.validate()) return;

        System.out.println("\n\n##### listener Send : " + returned.toJson() + "\n\n");



        // Sample Logic //
        // MessageInfo messageInfo = new MessageInfo();
        // messageInfoRepository.save(messageInfo);

    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}