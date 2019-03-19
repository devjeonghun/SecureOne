package com.example.jhuncho.secureone.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;

public class Encrypt {
    private static final String algorithm = "AES";
    private static final String transformation = algorithm + "/ECB/PKCS5Padding";

    public static void crypt(int mode, String seed_string, File source, File dest) throws Exception {
        Cipher cipher = getCipher(mode, seed_string);
        InputStream input = null;
        OutputStream output = null;
        try {
            input = new BufferedInputStream(new FileInputStream(source));
            output = new BufferedOutputStream(new FileOutputStream(dest));
            byte[] buffer = new byte[1024];
            int read = -1;
            while ((read = input.read(buffer)) != -1) {
                output.write(cipher.update(buffer, 0, read));
            }
            output.write(cipher.doFinal());
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ie) {
                }
            }
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ie) {
                }
            }
        }
    }

    // 지정한 시드 문자열로 보안 키를 생성한다.
    public static byte[] generateRawKey(String seed_string) throws Exception {
        SecureRandom secure_random = SecureRandom.getInstance("SHA1PRNG");
        secure_random.setSeed(seed_string.getBytes("UTF-8"));
        KeyGenerator key_generator = KeyGenerator.getInstance("AES");
        key_generator.init(128, secure_random);
        return (key_generator.generateKey()).getEncoded();
    }

    // 지정한 모드와 시드 문자열로 javax.crypto.Cipher 객체를 초기화해 반환한다.
    public static Cipher getCipher(int mode, String seed_string) throws Exception {
        SecretKeySpec key_spec = new SecretKeySpec(toBytes(seed_string,16), algorithm);
        Cipher cipher = Cipher.getInstance(transformation);
        cipher.init(mode, key_spec);
        return cipher;
    }

    public static byte[] toBytes(String digits, int radix) throws IllegalArgumentException, NumberFormatException {
        if (digits == null) {
            return null;
        }
        if (radix != 16 && radix != 10 && radix != 8) {
            throw new IllegalArgumentException("For input radix: \"" + radix + "\"");
        }
        int divLen = (radix == 16) ? 2 : 3;
        int length = digits.length();
        if (length % divLen == 1) {
            throw new IllegalArgumentException("For input string: \"" + digits + "\"");
        }
        length = length / divLen;
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            int index = i * divLen;
            bytes[i] = (byte) (Short.parseShort(digits.substring(index, index + divLen), radix));
        }
        return bytes;
    }
}
