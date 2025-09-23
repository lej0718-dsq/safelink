package com.dsqd.amc.linkedmo.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import org.apache.ibatis.io.Resources;

public class JwtUtil {
    private static final String SECRET = "jwt-256-bit-secret";
    private static final Algorithm algorithm = Algorithm.HMAC256(SECRET);
    
    private static final String server_SECRET = "amc_maru_server_$%13*&";
    private static final Algorithm server_algorithm = Algorithm.HMAC256(server_SECRET); 

    private static final String naru_SECRET = "naru_service_(%98-!";
    private static final Algorithm naru_algorithm = Algorithm.HMAC256(naru_SECRET); 

    private static final String admin_SECRET = "amc_linksafe_!*03^";
    private static final Algorithm admin_algorithm = Algorithm.HMAC256(admin_SECRET); 
    
    public JwtUtil() {
    	Properties properties = new Properties();
        try {
			properties.load(Resources.getResourceAsStream("application.properties"));
			AES256Util.setKey(properties.getProperty("aes.key"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public static String createToken(String clientIP, String username, String korname, String group) throws Exception {
        String encryptedIP = AES256Util.encrypt(clientIP);
        String encryptedUsername = AES256Util.encrypt(username);
        return JWT.create()
                .withIssuer("auth0")
                .withExpiresAt(new Date(System.currentTimeMillis() + 1800_000)) // 30 minutes
                .withClaim("clientIP", encryptedIP)
                .withClaim("username", encryptedUsername)
                .withClaim("korname", korname)
                .withClaim("group", group)
                .sign(algorithm);
    }
    
    public static String createUserToken(String status, String spuserid, String mobileno) throws Exception {
    	JSONObject data = new JSONObject();
    	data.put("status", status);
    	data.put("spuserid", spuserid);
    	data.put("mobileno", mobileno);
        String subcred = AES256Util.encrypt(data.toJSONString());
        System.out.println("json : " + data.toJSONString());
        System.out.println("json(enc) : " + subcred);
        return JWT.create()
                .withIssuer("allmycredit")
                .withExpiresAt(new Date(System.currentTimeMillis() + 180_000)) // 3 minutes
                .withClaim("subcred", subcred)
                .sign(algorithm);
    }
    
    public static DecodedJWT verifyUserToken(String token) throws JWTVerificationException {
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer("allmycredit")
                .build();
        return verifier.verify(token);
    }
    
    public static JSONObject getJSONObject(DecodedJWT jwt) {
    	String encBody = jwt.getClaim("subcred").asString();
    	if (encBody != null && !encBody.equals("")) {
    		try {
				String body = AES256Util.decrypt(encBody);
				System.out.println(body);
				return (JSONObject) JSONValue.parse(body);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return new JSONObject();
			}
    		
    	} else {
    		return new JSONObject();
    	}
    }

    public static DecodedJWT verifyToken(String token) throws JWTVerificationException {
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer("auth0")
                .build();
        return verifier.verify(token);
    }
    
    public static String getClientIP(DecodedJWT decodedJWT) throws Exception {
        String encryptedIP = decodedJWT.getClaim("clientIP").asString();
        return AES256Util.decrypt(encryptedIP);
    }
    
    public static String getUsername(DecodedJWT decodedJWT) throws Exception {
        String encryptedUsername = decodedJWT.getClaim("username").asString();
        return AES256Util.decrypt(encryptedUsername);
    }

    public static String getKorname(DecodedJWT decodedJWT) throws Exception {
    	return  decodedJWT.getClaim("korname").asString();
    }

    public static String getGroup(DecodedJWT decodedJWT) throws Exception {
    	return  decodedJWT.getClaim("group").asString();
    }
    
    // 서버 토큰용 
    public static String createServerToken(String clientIP, String username, long expire_sec) throws Exception {
        return JwtUtil.createServerToken(clientIP, username, expire_sec, server_algorithm);
    }

    public static String createServerToken(String clientIP, String username, long expire_sec, Algorithm algorithm) throws Exception {
        return JWT.create()
                .withIssuer("auth0")
                .withExpiresAt(new Date(System.currentTimeMillis() + (expire_sec*1000)))
                .withClaim("clientIP", clientIP)
                .withClaim("username", username)
                .sign(algorithm);
    }
    
    public static DecodedJWT verifyServerToken(String token) throws JWTVerificationException {
        return JwtUtil.verifyServerToken(token, server_algorithm);
    }
    
    public static DecodedJWT verifyServerToken(String token, Algorithm algorithm) throws JWTVerificationException {
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer("auth0")
                .build();
        return verifier.verify(token);
    }
    
    public static boolean verifyNaruToken(String token, String owner) {
    	boolean ret = false;
    	try {
    	DecodedJWT dtoken = JwtUtil.verifyServerToken(token, naru_algorithm);
    	String username = dtoken.getClaim("username").asString();
    	System.out.println(owner + ":" + username);
	    	if (owner.equals(username)) {
	    		ret = true;
	    	} 
    	} catch (JWTDecodeException jde) {
    		jde.printStackTrace();
    		ret = false;
    	} catch (Exception e) {
    		e.printStackTrace();
    		ret = false;
    	}
    	return ret;
    }
    
    public static void main(String [] args) {
    	try {
    		JwtUtil jwt = new JwtUtil();
			String token1 = createServerToken("114.108.153.73", "amcinc-prod", (60*60*24*30*14l + 60*60*24*8 ));
			System.out.println(token1);
			
			DecodedJWT dtoken1 = verifyServerToken(token1);
			System.out.println("SECRET : [" + server_SECRET + "]");
			System.out.println();
			System.out.println("Verify Token - issuer    : " + dtoken1.getIssuer());
			System.out.println("Verify Token - clientIP  : " + dtoken1.getClaim("clientIP").asString());
			System.out.println("Verify Token - username  : " + dtoken1.getClaim("username").asString());
			System.out.println("Verify Token - expiresAt : " + dtoken1.getExpiresAt());
			System.out.println();
			System.out.println();
			System.out.println();
			
			String token2 = createServerToken("114.108.153.56", "amcinc-dev", (60*60*24*30*24l + 60*60*24*8 ));
			System.out.println(token2);
			DecodedJWT dtoken2 = verifyServerToken(token2);
			System.out.println("SECRET : [" + server_SECRET + "]");
			System.out.println();
			System.out.println("Verify Token - issuer    : " + dtoken2.getIssuer());
			System.out.println("Verify Token - clientIP  : " + dtoken2.getClaim("clientIP").asString());
			System.out.println("Verify Token - username  : " + dtoken2.getClaim("username").asString());
			System.out.println("Verify Token - expiresAt : " + dtoken2.getExpiresAt());
			System.out.println();
			System.out.println();
			System.out.println();
			
			String userToken = JwtUtil.createUserToken("SMS", "11111112", "01012341234");
			System.out.println(userToken);
			DecodedJWT duserToken = JwtUtil.verifyUserToken(userToken);
			System.out.println("Verify Token - issuer    : " + duserToken.getIssuer());
			System.out.println("Verify Token - subcred    : " + duserToken.getClaim("subcred").asString());
			JSONObject tokenJSON = JwtUtil.getJSONObject(duserToken);
			System.out.println("get status - " + tokenJSON.getAsString("status"));
			System.out.println("get spuserid - " + tokenJSON.getAsString("spuserid"));
			System.out.println("get mobileno - " + tokenJSON.getAsString("mobileno"));
			System.out.println();
			System.out.println();
			System.out.println();

			String token4 = createServerToken("27.255.97.166", "naru-dev", (60*60*24*30*24l + 60*60*24*8 ), naru_algorithm);
			System.out.println(token4);
			DecodedJWT dtoken4 = JwtUtil.verifyServerToken(token4, naru_algorithm);
			System.out.println("SECRET : [" + naru_SECRET + "]");
			System.out.println();
			System.out.println("Verify Token - issuer    : " + dtoken4.getIssuer());
			System.out.println("Verify Token - clientIP  : " + dtoken4.getClaim("clientIP").asString());
			System.out.println("Verify Token - username  : " + dtoken4.getClaim("username").asString());
			System.out.println("Verify Token - expiresAt : " + dtoken4.getExpiresAt());
			System.out.println();
			System.out.println();
			System.out.println();
			
			String token5 = createServerToken("27.255.97.166", "naru-prod", (60*60*24*30*24l + 60*60*24*8 ), naru_algorithm);
			System.out.println(token5);
			DecodedJWT dtoken5 = JwtUtil.verifyServerToken(token5, naru_algorithm);
			System.out.println("SECRET : [" + naru_SECRET + "]");
			System.out.println();
			System.out.println("Verify Token - issuer    : " + dtoken5.getIssuer());
			System.out.println("Verify Token - clientIP  : " + dtoken5.getClaim("clientIP").asString());
			System.out.println("Verify Token - username  : " + dtoken5.getClaim("username").asString());
			System.out.println("Verify Token - expiresAt : " + dtoken5.getExpiresAt());
			System.out.println();
			System.out.println();
			System.out.println();
			
			String token6 = createServerToken("27.255.97.166", "naru-local", (60*60*24*30*24l + 60*60*24*8 ), naru_algorithm);
			System.out.println(token6);
			DecodedJWT dtoken6 = JwtUtil.verifyServerToken(token6, naru_algorithm);
			System.out.println("SECRET : [" + naru_SECRET + "]");
			System.out.println();
			System.out.println("Verify Token - issuer    : " + dtoken6.getIssuer());
			System.out.println("Verify Token - clientIP  : " + dtoken6.getClaim("clientIP").asString());
			System.out.println("Verify Token - username  : " + dtoken6.getClaim("username").asString());
			System.out.println("Verify Token - expiresAt : " + dtoken6.getExpiresAt());
			System.out.println();
			System.out.println();
			System.out.println();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
