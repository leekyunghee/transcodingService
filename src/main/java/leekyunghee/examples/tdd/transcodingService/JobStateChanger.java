package leekyunghee.examples.tdd.transcodingService;

import leekyunghee.examples.tdd.transcodingService.Job.State;

public interface JobStateChanger {

	void changeJobState(Long jobId, State newJobState);

}
