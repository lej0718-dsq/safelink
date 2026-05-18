package com.dsqd.amc.linkedmo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.dsqd.amc.linkedmo.model.Refund;
import com.dsqd.amc.linkedmo.model.RefundDetail;
import com.dsqd.amc.linkedmo.model.RefundQuery;

@Mapper
public interface RefundMapper {

    void insertRefund(Refund refund);

    void insertRefundDetail(RefundDetail detail);

    Refund selectById(@Param("id") long id);

    List<Refund> selectByQuery(@Param("q") RefundQuery q);

    int countByMobilenoAndStatus(@Param("mobileno") String mobileno,
                                 @Param("status") String status);

    int updateToCompleted(@Param("id") long id,
                          @Param("processedBy") String processedBy,
                          @Param("memo") String memo);

    List<RefundDetail> selectDetailsByRefundId(@Param("refundId") long refundId);
}
