package com.dsqd.amc.linkedmo.mapper;

import com.dsqd.amc.linkedmo.model.CouponRequest;
import com.dsqd.amc.linkedmo.model.CouponRequestCount;
import com.dsqd.amc.linkedmo.model.CouponResponse;
import com.dsqd.amc.linkedmo.model.Subscribe;
import org.apache.ibatis.annotations.Mapper;

import java.util.HashMap;
import java.util.List;

@Mapper
public interface CouponMapper {
    List<Subscribe> getCouponTargetList(HashMap<String,Object> params);
    List<Subscribe> getMegaCouponTargetList(HashMap<String,Object> params);

    void insertCouponRequest(HashMap<String,Object> params);
    void insertCouponResponse(CouponResponse couponResponse);

    void updateCouponRequestCount(String offercode, String noReq, int couponCount);
}
