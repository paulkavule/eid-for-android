package net.egelke.android.eid;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;

import net.egelke.android.eid.EidReader.File;

public class CertificateCollection implements Collection<X509Certificate> {
	
	private static File[] FILES = new File[] { File.ROOTCA_CERT, File.RRN_CERT, File.INTCA_CERT, File.AUTH_CERT, File.SIGN_CERT };
	
	private class Iterator implements java.util.Iterator<X509Certificate> {
		
		private int curr = 0;

		@Override
		public boolean hasNext() {
			return curr < FILES.length;
		}

		@Override
		public X509Certificate next() {
			try {
				CertificateFactory factory = CertificateFactory.getInstance("X.509");
				
				byte[] bytes = reader.readFileRaw(slotnr, FILES[curr++]);
				return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(bytes));
			} catch (RuntimeException re) {
				throw re;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("read only");
		}
		
	}
	
	private int slotnr;
	private EidReader reader;
	
	public CertificateCollection(EidReader reader, int slotnr) {
		this.reader = reader;
		this.slotnr = slotnr;
	}

	@Override
	public boolean add(X509Certificate object) {
		throw new UnsupportedOperationException("read only");
	}

	@Override
	public boolean addAll(Collection<? extends X509Certificate> arg0) {
		throw new UnsupportedOperationException("read only");
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException("read only");
	}

	@Override
	public boolean contains(Object object) {
		throw new UnsupportedOperationException("just the iterator for now");
	}

	@Override
	public boolean containsAll(Collection<?> arg0) {
		throw new UnsupportedOperationException("just the iterator for now");
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public java.util.Iterator<X509Certificate> iterator() {
		return new Iterator();
	}

	@Override
	public boolean remove(Object object) {
		throw new UnsupportedOperationException("read only");
	}

	@Override
	public boolean removeAll(Collection<?> arg0) {
		throw new UnsupportedOperationException("read only");
	}

	@Override
	public boolean retainAll(Collection<?> arg0) {
		throw new UnsupportedOperationException("read only");
	}

	@Override
	public int size() {
		return FILES.length;
	}

	@Override
	public Object[] toArray() {
		throw new UnsupportedOperationException("just the iterator for now");
	}

	@Override
	public <T> T[] toArray(T[] array) {
		throw new UnsupportedOperationException("just the iterator for now");
	}

}
