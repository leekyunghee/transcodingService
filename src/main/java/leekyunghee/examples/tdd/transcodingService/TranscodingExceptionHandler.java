package leekyunghee.examples.tdd.transcodingService;

public interface TranscodingExceptionHandler {

	void notifyToJob(Long jobId, RuntimeException ex);

}
