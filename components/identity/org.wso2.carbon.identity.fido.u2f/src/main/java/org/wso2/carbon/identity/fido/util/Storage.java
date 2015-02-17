package org.wso2.carbon.identity.fido.util;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.Collection;

/**
 * Created by ananthaneshan on 12/22/14.
 */
public class Storage {
	private static Log log = LogFactory.getLog(Storage.class);
//	private static Map<String, String> requestStorage = new HashMap<String, String>();
	private static Multimap<String, String> userStorage = ArrayListMultimap.create();

	private static void serialiseObject(String file, Object obj) {
		try {
			// if file doesnt exists, then create it
			File newFile = new File(file);
			if (!newFile.exists()) {

				newFile.createNewFile();

			}
			FileOutputStream fos =
					new FileOutputStream(file);

			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(obj);
			oos.close();
			fos.close();

		} catch (IOException e) {
			log.error("Error while serialising FIDO object", e);
		}
	}

	private static Object deSerialiseObject(String file) {
		Object obj = null;
		try {
			FileInputStream fis = new FileInputStream(file);
			ObjectInputStream ois = new ObjectInputStream(fis);

			obj = ois.readObject();
			ois.close();
			fis.close();
		} catch (FileNotFoundException e) {
			File newFile = new File(file);

			// if file doesnt exists, then create it
			if (!newFile.exists()) {
				try {
					newFile.createNewFile();
				} catch (IOException e1) {
					log.error("Error while de-serialising FIDO object: File not found", e);
				}
			}
		} catch (IOException e) {
			log.error("Error while de-serialising FIDO object", e);
			return obj;

		} catch (ClassNotFoundException e) {
			log.error("Error while de-serialising FIDO object", e);
		}
		return obj;
	}

	/*private static void readRequestStorage() {
		Object obj = deSerialiseObject("requestStorage");
		requestStorage =
				(null == obj ? requestStorage : (HashMap<String, String>) obj);
	}*/

	/*private static void writeRequestStorage() {
		serialiseObject("requestStorage", requestStorage);
	}*/

	private static void readUserStorage() {
		Object obj = deSerialiseObject("userStorage");
		userStorage = (null == obj ? userStorage : (Multimap<String, String>) obj);
	}

	private static void writeUserStorage() {
		serialiseObject("userStorage", userStorage);
	}

	/*public static void storeToRequestStorage(String str1, String str2) {
		readRequestStorage();
		requestStorage.put(str1, str2);
		writeRequestStorage();

	}*/

	/*public static String retrieveRequestStorage(String str1) {
		readRequestStorage();
		return requestStorage.get(str1);

	}*/

	/*public static String removeFromRequestStorage(String str1) {
		String request;
		readRequestStorage();
		request = requestStorage.remove(str1);
		writeRequestStorage();
		System.out.println("request : " + request);
		return request;
	}*/

	public static void storeToUserStorage(String str1, String str2) {
		readUserStorage();
		userStorage.put(str1, str2);
		writeUserStorage();

	}

	public static Collection retrieveFromUserStorage(String str1) {
		readUserStorage();
		return userStorage.get(str1);

	}

}
