package com.dsqd.amc.linkedmo.model;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import net.minidev.json.JSONObject;

@ToString
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class Subscribe {
	private int id;
	private String spcode;
	private String mobileno;
	private boolean agree1;
	private boolean agree2;
	private boolean agree3;
	private Date createDate;
	private String bFP;
	private String status;
	private String offercode;
	private Date cancelDate;
	private String cncode;
	private String spuserid;
	private String checkcode;
    private String no_req;
    private int coupon_count;
    private int coupon_max_count;
    private String linkno;

    public String toJSONString() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("spcode", spcode);
        jsonObject.put("mobileno", mobileno);
        jsonObject.put("agree1", agree1);
        jsonObject.put("agree2", agree2);
        jsonObject.put("agree3", agree3);
        jsonObject.put("createDate", createDate);
        jsonObject.put("bFP", bFP);
        jsonObject.put("status", status);
        jsonObject.put("offercode", offercode);
        jsonObject.put("cancelDate", cancelDate);
        jsonObject.put("cncode", cncode);
        jsonObject.put("spuserid", spuserid);
        jsonObject.put("checkcode", checkcode);
        jsonObject.put("no_req", no_req);
        jsonObject.put("coupon_count", coupon_count);
        jsonObject.put("coupon_max_count", coupon_max_count);
        jsonObject.put("linkno", linkno);
        return jsonObject.toJSONString();
    }
}
