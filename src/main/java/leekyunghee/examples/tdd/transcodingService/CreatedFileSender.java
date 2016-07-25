package leekyunghee.examples.tdd.transcodingService;

import java.io.File;
import java.util.List;

public interface CreatedFileSender {

	void send(List<File> multimediaFiles, List<File> thumbnails, Long jobId);

}
