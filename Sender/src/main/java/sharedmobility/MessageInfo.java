package sharedmobility;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="MessageInfo_table")
public class MessageInfo {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long alarmId;
    private Long customerId;
    private Long orderId;
    private String rentDate;
    private String rentStatus;
    private Long rentId;

    @PostPersist
    public void onPostPersist(){
        Sended sended = new Sended();
        BeanUtils.copyProperties(this, sended);
        sended.publishAfterCommit();

    }

    public Long getAlarmId() {
        return alarmId;
    }

    public void setAlarmId(Long alarmId) {
        this.alarmId = alarmId;
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
    public String getRentDate() {
        return rentDate;
    }

    public void setRentDate(String rentDate) {
        this.rentDate = rentDate;
    }
    public String getRentStatus() {
        return rentStatus;
    }

    public void setRentStatus(String rentStatus) {
        this.rentStatus = rentStatus;
    }
    public Long getRentId() {
        return rentId;
    }

    public void setRentId(Long rentId) {
        this.rentId = rentId;
    }




}