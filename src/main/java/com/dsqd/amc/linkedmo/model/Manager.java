package com.dsqd.amc.linkedmo.model;

import java.util.Date;

import com.dsqd.amc.linkedmo.util.AES256Util;

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

public class Manager {
	
	private String username; // loginid
	private String password; // 비밀번호
	private String korname;  // 한글이름
	private Date lastlogined; // 최종로그인시간
	private String lastloginip; // 최종로그인IP
	private int failcount;
    private String group;

	public String toJSONString() {
		JSONObject jsonObject = new JSONObject();
		try {
			String encpasswd = AES256Util.encrypt(password);
			jsonObject.put("username", username);
			jsonObject.put("password", encpasswd);
			jsonObject.put("korname", korname);
			jsonObject.put("lastlogined", lastlogined);
			jsonObject.put("lastloginip", lastloginip);
			jsonObject.put("failcount", failcount);
		} catch (Exception e) {
			e.printStackTrace();
		}
        return jsonObject.toJSONString();
	}
}
