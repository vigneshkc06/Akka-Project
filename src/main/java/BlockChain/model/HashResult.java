package BlockChain.model;

public class HashResult {

	private int nonce;
	private String hash;
	private boolean complete = false;
	
	public HashResult() {}
	
	public int getNonce() {
		return nonce;
	}

	public String getHash() {
		return hash;
	}
	
	public boolean isComplete() {
		return complete;
	}
	
	public synchronized void foundAHash(String hash, int nonce) {
		this.hash = hash;
		this.nonce = nonce;
		this.complete = true;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		HashResult that = (HashResult) o;

		if (nonce != that.nonce) return false;
		return hash != null ? hash.equals(that.hash) : that.hash == null;
	}

	@Override
	public int hashCode() {
		int result = nonce;
		result = 31 * result + (hash != null ? hash.hashCode() : 0);
		return result;
	}
}
