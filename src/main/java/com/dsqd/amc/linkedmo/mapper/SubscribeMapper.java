package com.dsqd.amc.linkedmo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.dsqd.amc.linkedmo.model.Subscribe;

@Mapper
public interface SubscribeMapper {

	Subscribe getSubscribeById(@Param("id") int id);
	
	List<Subscribe> getSubscribeByMobileno(Subscribe data);
	
	List<Subscribe> getTodaySubscribeByMobileno(Subscribe data);
	
	List<Subscribe> getSubscribeAll();
	
	List<Subscribe> getSubscribeAllActive();

    void insertSubscribe(Subscribe data);

    void updateSubscribe(Subscribe data);
    
    void updateSubscribeStatus(Subscribe data);

    void deleteSubscribe(@Param("id") int id);

    void deleteSubscribeT0(@Param("id") int id);
    
    List<Subscribe> getCancelList();

    List<Subscribe> getSubscribeAlltoBatch01();

    String  getFirstSubscriber(String mobileno);
}
