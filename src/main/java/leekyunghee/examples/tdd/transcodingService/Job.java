package leekyunghee.examples.tdd.transcodingService;

public class Job {
	
	public static enum State {
		MEDIASOURCECOPYING, COMPLETED, TRANSCODER, CREATEDFILESENDOR, JOBRESULTNOTIFIER, THUMBNAILEXTRACTOR
	}
	private State state;
	private Exception occurredException;
	
	public boolean isSuccess() {
		return state == State.COMPLETED;
	}
	public State getLastState() {
		return this.state;
	}
	public void changeState(State newState) {
		this.state = newState;
	}
	public boolean isFinished() {
		
		return isSuccess() || isExceptionOccurred();
	}
	private boolean isExceptionOccurred() {
		return occurredException != null;
	}
	public Exception getOccurredException() {
		return occurredException;
	}
	public void exceptionOccurred(RuntimeException ex) {
		occurredException = ex;
	}
	
}