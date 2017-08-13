package ws.temple.graw.crypt;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESCrypter implements Crypter {
	
	private static final int BLOCK_LENGTH = 16;

	private final Cipher cipher;
	private final SecretKey key;
	
	public AESCrypter(SecretKey key) throws CrypterException {
		try {
			this.cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			this.key = key;
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			throw new CrypterException("Unable to retrieve cipher instance", e);
		}
	}
	
	public AESCrypter(byte[] key) throws CrypterException {
		this(new SecretKeySpec(key, "AES"));
		Arrays.fill(key, (byte) 0x00);
	}
	
	@Override
	public byte[] encrypt(char[] plaintext, String charset) throws CrypterException {
		final ByteBuffer data = Charset.forName(charset).encode(CharBuffer.wrap(plaintext));
		return encrypt(data).array();
	}
	
	@Override
	public char[] decrypt(byte[] ciphertext, String charset) throws CrypterException {
		return Charset.forName(charset).decode(decrypt(ByteBuffer.wrap(ciphertext))).array();
	}
	
	public byte[] encrypt(byte[] plaintext) throws CrypterException {
		return encrypt(ByteBuffer.wrap(plaintext)).array();
	}
	
	public byte[] decrypt(byte[] ciphertext) throws CrypterException {
		return decrypt(ByteBuffer.wrap(ciphertext)).array();
	}
	
	public ByteBuffer encrypt(ByteBuffer plaintext) throws CrypterException {
		ByteBuffer ciphertext = null;
		try {
			final int cipherLength = (plaintext.remaining() / BLOCK_LENGTH + 1) * BLOCK_LENGTH;
			
			synchronized(cipher) {
				cipher.init(Cipher.ENCRYPT_MODE, key);
				ciphertext = ByteBuffer.allocate(BLOCK_LENGTH + cipherLength).put(cipher.getIV());
				cipher.doFinal(plaintext, ciphertext);
			}
			
			ciphertext.flip();
		}
		catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | ShortBufferException e) {
			throw new CrypterException("Exception while performing decryption", e);
		}
		return ciphertext;
	}
	
	public ByteBuffer decrypt(ByteBuffer ciphertext) throws CrypterException {
		ByteBuffer plaintext = null;
		try {
			final byte[] initVector = new byte[BLOCK_LENGTH];
			ciphertext.get(initVector);
			plaintext = ByteBuffer.allocate(ciphertext.remaining());
			
			synchronized(cipher) {
				cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(initVector));
				cipher.doFinal(ciphertext, plaintext);
			}
			
			plaintext.flip();
		}
		catch (InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException | ShortBufferException e) {
			throw new CrypterException("Exception while performing encryption", e);
		}
		
		return plaintext;
	}
	
}
