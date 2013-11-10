package net.egelke.android.eid;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

import net.egelke.android.eid.model.Address;
import net.egelke.android.eid.model.Identity;
import net.egelke.android.eid.model.ObjectFactory;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

//TODO:encapsulate in content provider (http://developer.android.com/guide/topics/providers/content-providers.html)
public class EidReader implements Closeable {
	
	public final static int MSG_CARD_INSERTED = 0;
	public final static int MSG_CARD_REMOVED = 1;
	
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
	
	//final private Reader reader;
	private int sequence;
	private UsbManager usbManager;
	private UsbDevice usbDevice;
	private UsbInterface usbInterface;
	private UsbDeviceConnection usbConnection;
	private UsbEndpoint usbInterupt;
	private UsbEndpoint usbOut;
	private UsbEndpoint usbIn;
	private boolean requestClose;
	private Thread interuptThread;
	private Handler handler = null;
	
	public EidReader(final Activity activity, final UsbDevice device) {
		usbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
		usbDevice = device;
	}
	
	public void setStateNotifier(Handler value) {
		handler = value;
	}
	
	public Handler getStateNotifier() {
		return handler;
	}
	
	public int getDeviceId() {
		return usbDevice.getDeviceId();
	}
	
	public void open() throws IOException {
		if (usbDevice == null) throw new IllegalArgumentException("Device can't be null");
		for(int i=0; i < usbDevice.getInterfaceCount(); i++) {
			UsbInterface usbIf = usbDevice.getInterface(i);
			if (usbIf.getInterfaceClass() == 0x0B 
					&& usbIf.getInterfaceSubclass() == 0x00 
					&& usbIf.getInterfaceProtocol() == 0x00) {
				usbInterface = usbIf;
			}
		}
		if(usbInterface == null) throw new IllegalStateException("The device hasn't a smard card reader");
	
		usbConnection = usbManager.openDevice(usbDevice);
		usbConnection.claimInterface(usbInterface, true);
		
		//Get the interfaces
		for (int i=0; i < usbInterface.getEndpointCount(); i++) {
			UsbEndpoint usbEp = usbInterface.getEndpoint(i);
			if (usbEp.getDirection() == UsbConstants.USB_DIR_IN && usbEp.getType() == UsbConstants.USB_ENDPOINT_XFER_INT && usbEp.getAttributes() == 0x03) {
				usbInterupt = usbEp;
			}
			if (usbEp.getDirection() == UsbConstants.USB_DIR_OUT && usbEp.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK && usbEp.getAttributes() == 0x02) {
				usbOut = usbEp;
			}
			if (usbEp.getDirection() == UsbConstants.USB_DIR_IN && usbEp.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK && usbEp.getAttributes() == 0x02) {
				usbIn = usbEp;
			}
		}
		
		//check for changes.
		if (usbInterupt != null) {
			requestClose = false;
			interuptThread = new Thread(new Runnable() {
				
				@Override
				public void run() {
					int count;
					byte[] buffer = new byte[4];
					while (!requestClose) {
						if ((count = usbConnection.bulkTransfer(usbInterupt, buffer, buffer.length, 250)) >= 0) {
							if (count == 0) continue;
							
							switch (buffer[0]) {
							case 0x50: //status change
								if (count < 2) {
									Log.i("net.egelke.android.eid", "we got no input");
									continue;
								}
								if (count > 2) {
									Log.w("net.egelke.android.eid", "we only support up to 4 slots, ignoring the rest");
								}
								if (handler != null) {
									notify(buffer, 0);
									notify(buffer, 1);
									notify(buffer, 2);
									notify(buffer, 3);
								}
								break;
							default:
								Log.w("net.egelke.android.eid", String.format("Unsupported interupt received: %x", buffer[0]));
								break;
							}
						}
						try {
							Thread.sleep(250);
						} catch (InterruptedException e) {
							Log.w("net.egelke.android.eid", "Sleep interupted", e);
							break;
						}
					}
					
				}

				private void notify(byte[] buffer, int slot) {
					byte stateLoc = (byte) (0x01 << (slot * 2));
					byte changedLoc = (byte) (0x01 << ((slot * 2) + 1));
					if ((buffer[1] & changedLoc) == changedLoc) {
						Message msg;
						if ((buffer[1] & stateLoc) == stateLoc) {
							msg = handler.obtainMessage(EidReader.MSG_CARD_INSERTED);
							//Lets prepare the card before sending the notification
							lock.lock();
							try {
								int count;
								byte[] powerOnCmd = new byte[] {(byte) 0x62, 0x00, 0x00, 0x00, 0x00, (byte) slot, (byte) sequence, 0x00, 0x00, 0x00};
								count = usbConnection.bulkTransfer(usbOut, powerOnCmd, powerOnCmd.length, 1000);
								byte[] powerOnRsp = new byte[255];
								count = usbConnection.bulkTransfer(usbIn, powerOnRsp, powerOnRsp.length, 5000);
								if (count >= 10) {
									if (powerOnRsp[0] != (byte) 0x80) {
										Log.e("net.egelke.android.eid", String.format("Unsupported PowerOn Response received, Type: %d", powerOnRsp[0]));
										return;
									}
									if (powerOnRsp[6] != (byte) sequence) {
										Log.e("net.egelke.android.eid", String.format("Received Response of different request"));
										return;
									}
									if ((powerOnRsp[7] & (byte) 0xA0) != 0x00) {
										Log.e("net.egelke.android.eid", String.format("Power on failed with status %x and error: %x", powerOnRsp[7], powerOnRsp[8]));
										return;
									}
								} else {
									Log.e("net.egelke.android.eid", String.format("Unsupported PowerOn Response received, Len: %d", count));
									return;
								}
							} finally {
								sequence = (sequence + 1) % 0xFF;
								lock.unlock();
							}
						} else {
							msg = handler.obtainMessage(EidReader.MSG_CARD_REMOVED);
						}
						msg.arg1 = slot;
						handler.sendMessage(msg);
					}
				}
			}, "EidInteruptThead");
			interuptThread.start();
		}
	}
	
	
	@Override
	public void close() throws IOException {
		handler = null;
		if (interuptThread != null) {
			requestClose = true;
			try {
				interuptThread.join(1000);
			} catch (InterruptedException e) {
				Log.w("net.egelke.android.eid", "Join interupted", e);
			}
			interuptThread = null;
		}
		if (usbInterface != null) {
			lock.lock();
			try {
				usbConnection.releaseInterface(usbInterface);
				usbConnection.close();
			} finally {
				lock.unlock();
			}
		}
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
		ObjectFactory factory = new ObjectFactory();
		byte[] bytes = readFileRaw(slotNum, file);
		return factory.createTvMap(bytes);
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
	
	public CertificateCollection readFileCerts(int slotNum) {
		return new CertificateCollection(this, slotNum);
	}
	
	private void selectFile(int slotNum, byte[] cmd) throws Exception {
		int rspLen;
		byte[] rsp = new byte[258];
		rspLen = transmit(slotNum, cmd, cmd.length, rsp, rsp.length);
		if (rspLen != 2) {
			Log.e("net.egelke.android.eid", "APDU select identify file command did not return 2 bytes but: " + rspLen);
			throw new Exception("Invalid card");
		}
		if (rsp[0] != ((byte) 0x90) || rsp[1] != 0x0) {
			Log.e("net.egelke.android.eid", String.format("APDU select identify file command failed: %X %X", rsp[0], rsp[1]));
			throw new Exception("Unsupported card");
		}
	}
	
	private byte[] readSelectedFile(int slotNum) throws Exception {
		int rspLen;
		int offset = 0;
		byte[] rsp = new byte[258];
		byte[] cmd = Arrays.copyOf(READ_BINARY, READ_BINARY.length);
		ByteArrayOutputStream idFileOut = new ByteArrayOutputStream(128);
		do {
			cmd[2] = (byte) (offset >> 8);
			cmd[3] = (byte) (offset & 0xFF);
			rspLen = transmit(slotNum, cmd, cmd.length, rsp, rsp.length);
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

	private int transmit(int slot, byte[] cmd, int cmdLen, byte[] rsp, int rspLen) {
		int count = -1;
		byte[] usbCmd = new byte[cmdLen + 10];
		usbCmd[0] = 0x6F; //type
		usbCmd[1] = (byte) cmdLen; //len
		usbCmd[2] = 0x00; //we don't support long lenghts
		usbCmd[3] = 0x00;
		usbCmd[4] = 0x00;
		usbCmd[5] = (byte) slot;
		usbCmd[6] = (byte) sequence;
		usbCmd[7] = 0x00; //not used
		usbCmd[8] = 0x00; //Param, not used for T=0
		usbCmd[9] = 0x00;
		System.arraycopy(cmd, 0, usbCmd, 10, cmdLen);
		
		int loop = 0;
		byte[] usbRsp = null;
		while(loop++ < 5 && count == -1) {
			count = usbConnection.bulkTransfer(usbOut, usbCmd, usbCmd.length, 1000);
			Log.d("net.egelke.android.eid", "Sent data to card reader: " + count);
			
			usbRsp = new byte[rspLen + 50];
			count = usbConnection.bulkTransfer(usbIn, usbRsp, usbRsp.length, 1000);
			Log.d("net.egelke.android.eid", "Read data from card reader: " + count);

			if (count >= 10) {
				if (usbRsp[0] != (byte) 0x80) {
					Log.e("net.egelke.android.eid", String.format("Unsupported bulk response received, Type: %d", usbRsp[0]));
					count = -1;
				}
				if (usbRsp[6] != (byte) sequence) {
					Log.e("net.egelke.android.eid", String.format("Received Response of different request"));
					count = -1;
				}
			} else {
				Log.e("net.egelke.android.eid", String.format("Unsupported bulk Response received, Len: %d", count));
				count = -1;
			}
		}
		
		if ((usbRsp[7] & (byte) 0xA0) != 0x00) {
			Log.e("net.egelke.android.eid", String.format("bulk response failed with status %x and error: %x", usbRsp[7], usbRsp[8]));
			return -1;
		}
		System.arraycopy(usbRsp, 10, rsp, 0, count - 10);
		return count - 10;
	}

}
