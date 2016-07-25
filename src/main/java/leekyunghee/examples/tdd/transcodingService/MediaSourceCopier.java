package leekyunghee.examples.tdd.transcodingService;

import java.io.File;

public interface MediaSourceCopier {
	File copy(Long jobId);
}
