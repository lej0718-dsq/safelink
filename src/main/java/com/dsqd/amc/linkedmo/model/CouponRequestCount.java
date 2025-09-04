package com.dsqd.amc.linkedmo.model;

import lombok.*;

import java.util.Date;

@ToString
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponRequestCount {

    private int coupon_request_count_id;
    private int count;
    private int max_count;
    private String offercode;
    private String no_req;
    private Date startDate;
    private Date endDate;

}
