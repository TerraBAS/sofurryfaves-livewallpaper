package com.sofurry.favorites;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Log;

public class Authentication {
	public static final String PREFS_NAME = "SoFurryPreferences";
	public static final int AJAXTYPE_OTPAUTH = 6;
	private static String authenticationPadding = "@6F393fk6FzVz9aM63CfpsWE0J1Z7flEl9662X";
	private static long currentAuthenticationSequence = 0;
	private static String username = null;
	private static String password = null;
	private static String salt = "";
	
	//Get the MD5 sum of a given input string
	private static String getMd5Hash(final String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] messageDigest = md.digest(input.getBytes());
			BigInteger number = new BigInteger(1, messageDigest);
			String md5 = number.toString(16);
			while (md5.length() < 32)
				md5 = "0" + md5;
			return md5;
		} catch (NoSuchAlgorithmException e) {
			Log.e("MD5", e.getMessage());
			return null;
		}
	}
	
	//Create a has using the current authentication sequence counter, thus "salting" the hash. 
	public static String generateRequestHash() {
		String hashedPassword = getMd5Hash(password + salt);
	    String hash = getMd5Hash(hashedPassword + authenticationPadding + currentAuthenticationSequence);
	    Log.d("Auth", "Password: " + hashedPassword +
	    			" padding: " + authenticationPadding + 
	    			" sequence: " + currentAuthenticationSequence +
	    			" salt: " + salt);
	    return hash;
	}

	
	public static Map<String, String> addAuthParametersToQuery(Map<String, String> queryParams) {
		Map<String, String> result = new HashMap<String, String>(queryParams);
		result.put("otpuser", username);
		result.put("otphash", generateRequestHash());
		result.put("otpsequence", ""+currentAuthenticationSequence);
		currentAuthenticationSequence = currentAuthenticationSequence+1;
		return result;
	}
		
	public static long getCurrentAuthenticationSequence() {
		return currentAuthenticationSequence;
	}

	public static void setCurrentAuthenticationSequence(long newSequence) {
		currentAuthenticationSequence = newSequence;
	}

	public static void setCurrentAuthenticationPadding(String newPadding) {
		authenticationPadding = newPadding;
	}

	public static void setCurrentAuthenticationSalt(String newSalt) {
		salt = newSalt;
	}

	public static String getUsername() {
		return username;
	}

	public static String getPassword() {
		return password;
	}
	
	public static void updateAuthenticationInformation(String newUsername, String newPassword) {
		username = newUsername;
		password = newPassword;
	}
	
	/**
	 * Check if passed json string contains data indicating a sequence mismatch, as well as the new sequence data
	 * @param httpResult
	 * @return true if no sequence data found or sequence correct, false if the request needs to be resent with the new enclosed sequence data
	 * @throws JSONException 
	 */
	public static boolean parseResponse(String httpResult) {
		try {
			//check for OTP sequence json and parse it.
			Log.d("Auth.parseResponse", "response: "+httpResult);
			JSONObject jsonParser;
			jsonParser = new JSONObject(httpResult);
			int messageType = jsonParser.getInt("messageType");
			if (messageType == AJAXTYPE_OTPAUTH) {
				int newSequence = jsonParser.getInt("newSequence");
				String newPadding = jsonParser.getString("newPadding");
				String newSalt = jsonParser.getString("salt");
				String otpVersion = jsonParser.getString("version");
				Log.d("Auth.parseResponse", "OTP Version: " + otpVersion + 
							" new Sequence: " + newSequence +
							" new Padding: " + newPadding +
							" new salt: " + newSalt );
				setCurrentAuthenticationSequence(newSequence);
				setCurrentAuthenticationPadding(newPadding);
				setCurrentAuthenticationSalt(newSalt);
				return false;
			}
		} catch (JSONException e) {
			Log.d("Auth.parseResponse", e.toString());
		}
		
		return true;
	}

}
