package ws.temple.graw.crypt;

public interface Crypter {
	
	byte[] encrypt(char[] plaintext, String charset) throws CrypterException;
	char[] decrypt(byte[] ciphertext, String charset) throws CrypterException;

}
