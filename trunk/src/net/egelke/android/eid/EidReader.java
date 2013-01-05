package net.egelke.android.eid;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

import net.egelke.android.eid.model.Address;
import net.egelke.android.eid.model.Identity;
import net.egelke.android.eid.model.ObjectFactory;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.acs.smartcard.Reader;
import com.acs.smartcard.Reader.OnStateChangeListener;
import com.acs.smartcard.ReaderException;

//TODO:encapsulate in content provider (http://developer.android.com/guide/topics/providers/content-providers.html)
public class EidReader implements Closeable {
	
	public static enum File {
		IDENTITY(
				new byte[] {0x00, (byte) 0xA4, 0x08, 0x0C, 0x06, 0x3F, 0x00, (byte) 0xDF, 0x01, 0x40, 0x31}),
		ADDRESS(
				new byte[] {0x00, (byte) 0xA4, 0x08, 0x0C, 0x06, 0x3F, 0x00, (byte) 0xDF, 0x01, 0x40, 0x33}),
		PHOTO(
				new byte[] {0x00, (byte) 0xA4, 0x08, 0x0C, 0x06, 0x3F, 0x00, (byte) 0xDF, 0x01, 0x40, 0x35}),
		AUTH_CERT(
				new byte[] {0x00, (byte) 0xA4, 0x08, 0x0C, 0x06, 0x3F, 0x00, (byte) 0xDF, 0x00, 0x50, 0x38}),
		SIGN_CERT(
				new byte[] {0x00, (byte) 0xA4, 0x08, 0x0C, 0x06, 0x3F, 0x00, (byte) 0xDF, 0x00, 0x50, 0x39}),
		INTCA_CERT(
				new byte[] {0x00, (byte) 0xA4, 0x08, 0x0C, 0x06, 0x3F, 0x00, (byte) 0xDF, 0x00, 0x50, 0x3A}),
		ROOTCA_CERT(
				new byte[] {0x00, (byte) 0xA4, 0x08, 0x0C, 0x06, 0x3F, 0x00, (byte) 0xDF, 0x00, 0x50, 0x3B}),
		RRN_CERT(
				new byte[] {0x00, (byte) 0xA4, 0x08, 0x0C, 0x06, 0x3F, 0x00, (byte) 0xDF, 0x00, 0x50, 0x3C}),
		IDENTITY_SIGN(
				new byte[] {0x00, (byte) 0xA4, 0x08, 0x0C, 0x06, 0x3F, 0x00, (byte) 0xDF, 0x01, 0x40, 0x32}),
		ADDRESS_SIGN(
				new byte[] {0x00, (byte) 0xA4, 0x08, 0x0C, 0x06, 0x3F, 0x00, (byte) 0xDF, 0x01, 0x40, 0x34});
				
		
		private final byte[] cmd;
		
		private File(byte[] cmd) {
			this.cmd = cmd;
		}
	}
	
	private static final byte[] READ_BINARY = {0x00, (byte) 0xB0, 0x00, 0x00, (byte)0x00};

	private static final ReentrantLock lock = new ReentrantLock();
	
	final private Reader reader;
	private Handler handler = null;
	
	public EidReader(final Activity activity, final UsbDevice device) {
		UsbManager manager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
		this.reader = new Reader(manager);
		this.reader.open(device);
		
		this.reader.setOnStateChangeListener(new OnStateChangeListener() {
			
			@Override
			public void onStateChange(int slotNum, int prevState, int currState) {
				if (currState == Reader.CARD_PRESENT) {
					try {
						reader.power(slotNum, Reader.CARD_WARM_RESET);
						reader.setProtocol(slotNum, Reader.PROTOCOL_T0 | Reader.PROTOCOL_T1);
					} catch (final Exception e) {
						Log.e("net.egelke.android.eid", "Failed listener", e);
					}
				}
				
				if (handler != null) {
					Message msg = handler.obtainMessage(currState);
					msg.arg1 = slotNum;
					msg.arg2 = prevState;
					handler.sendMessage(msg);
				}
			}
		});
	}
	
	public void setStateNotifier(Handler value) {
		handler = value;
	}
	
	public Handler getStateNotifier() {
		return handler;
	}
	
	public int getDeviceId() {
		return reader.getDevice().getDeviceId();
	}
	
	
	@Override
	public void close() throws IOException {
		handler = null;
		reader.setOnStateChangeListener(null);
		reader.close();
	}
	
	public byte[] readFileRaw(int slotNum, File file) throws IOException {
		lock.lock();
		try {
			Log.d("net.egelke.android.eid", "Reading file: " + file.name());
			selectFile(slotNum, file.cmd);
			return readSelectedFile(slotNum);
		} catch (Exception e) {
			throw new IOException("Could not read file", e);
		} finally {
			lock.unlock();
		}
	}
	
	public Map<Integer, byte[]> readFileTlv(int slotNum, File file) throws IOException {
		byte[] bytes = readFileRaw(slotNum, file);
		
		int tag;
		Map<Integer, byte[]> values = new TreeMap<Integer, byte[]>();
		ByteArrayInputStream idFileIn = new ByteArrayInputStream(bytes);
		while ((tag = idFileIn.read()) != -1) {
			int len = 0;
			int lenByte;
			do {
				lenByte = idFileIn.read();
				len = (len << 7) + (lenByte & 0x7F);
			} while ((lenByte & 0x80) == 0x80);
			
			//In case the file is padded with nulls
			if (tag == 0 && len == 0) break;
			
			byte[] value = new byte[len];
			int read = 0;
			while (read < len) {
				read += idFileIn.read(value, read, len - read);
			}
			Log.d("net.egelke.android.eid", String.format("Added tag %d (len %d) to %s", tag, value.length, file.name()));
			values.put(tag, value);
		}
		return values;
	}
	
	public Identity readFileIdentity(int slotNum) throws IOException {
		Map<Integer, byte[]> tvMap = readFileTlv(slotNum, File.IDENTITY);
		
		ObjectFactory factory = new ObjectFactory();
		return factory.createIdentity(tvMap);
	}
	
	public Address readFileAddress(int slotNum) throws IOException {
		Map<Integer, byte[]> tvMap = readFileTlv(slotNum, File.ADDRESS);
		
		ObjectFactory factory = new ObjectFactory();
		return factory.createAddress(tvMap);
	}
	
	public Drawable readFilePhoto(int slotNum) throws IOException {
		byte[] bytes = readFileRaw(slotNum, File.PHOTO);
		return Drawable.createFromStream(new ByteArrayInputStream(bytes), "idPic");
	}
	
	public List<X509Certificate> readFileCerts(int slotNum) throws IOException, CertificateException {
		byte[] bytes;
		List<X509Certificate> retVal = new LinkedList<X509Certificate>();
		CertificateFactory factory = CertificateFactory.getInstance("X.509");
		
		bytes = readFileRaw(slotNum, File.ROOTCA_CERT);
		retVal.add((X509Certificate) factory.generateCertificate(new ByteArrayInputStream(bytes)));
		
		bytes = readFileRaw(slotNum, File.RRN_CERT);
		retVal.add((X509Certificate) factory.generateCertificate(new ByteArrayInputStream(bytes)));
		
		bytes = readFileRaw(slotNum, File.INTCA_CERT);
		retVal.add((X509Certificate) factory.generateCertificate(new ByteArrayInputStream(bytes)));
		
		bytes = readFileRaw(slotNum, File.AUTH_CERT);
		retVal.add((X509Certificate) factory.generateCertificate(new ByteArrayInputStream(bytes)));
		
		bytes = readFileRaw(slotNum, File.SIGN_CERT);
		retVal.add((X509Certificate) factory.generateCertificate(new ByteArrayInputStream(bytes)));
		
		return retVal;
	}
	
	private void selectFile(int slotNum, byte[] cmd) throws ReaderException, Exception {
		
		int rspLen;
		byte[] rsp = new byte[258];
		rspLen = reader.transmit(slotNum, cmd, cmd.length, rsp, rsp.length);
		if (rspLen != 2) {
			Log.e("net.egelke.android.eid", "APDU select identify file command did not return 2 bytes but: " + rspLen);
			throw new Exception("Invalid card");
		}
		if (rsp[0] != ((byte) 0x90) || rsp[1] != 0x0) {
			Log.e("net.egelke.android.eid", String.format("APDU select identify file command failed: %X %X", rsp[0], rsp[1]));
			throw new Exception("Unsupported card");
		}
	}
	
	private byte[] readSelectedFile(int slotNum) throws ReaderException, Exception {
		int rspLen;
		int offset = 0;
		byte[] rsp = new byte[258];
		byte[] cmd = Arrays.copyOf(READ_BINARY, READ_BINARY.length);
		ByteArrayOutputStream idFileOut = new ByteArrayOutputStream(128);
		do {
			cmd[2] = (byte) (offset >> 8);
			cmd[3] = (byte) (offset & 0xFF);
			rspLen = reader.transmit(slotNum, cmd, cmd.length, rsp, rsp.length);
			if (rspLen < 2) {
				Log.e("net.egelke.android.eid", "APDU read identify file command did return less then 2 bytes: " + rspLen);
				throw new Exception("Invalid card");
			}
			Log.d("net.egelke.android.eid", String.format("Result %X %X, data length: %d", rsp[rspLen - 2], rsp[rspLen - 1], rspLen));
			if (rsp[rspLen - 2] == ((byte) 0x6B) && rsp[rspLen - 1] == 0x0) {
				// Finished, there where no more bytes
				break;
			}
			if (rsp[rspLen - 2] == ((byte) 0x6C)) {
				// Almost finished, reading less
				cmd[4] = rsp[1];
				continue;
			}

			if ((rsp[rspLen - 2] != ((byte) 0x90) || rsp[rspLen - 1] != 0x0)) {
				Log.e("net.egelke.android.eid", String.format("APDU read identify file command failed: %X %X", rsp[0], rsp[1]));
				throw new Exception("Unsupported card");
			}
			idFileOut.write(rsp, 0, rspLen - 2);
			offset += rspLen - 2;
		} while (rspLen == 258 || rsp[0] == ((byte) 0x6C));

		Log.d("net.egelke.android.eid", String.format("File read (len %d)", idFileOut.toByteArray().length));
		return idFileOut.toByteArray();
	}

}
