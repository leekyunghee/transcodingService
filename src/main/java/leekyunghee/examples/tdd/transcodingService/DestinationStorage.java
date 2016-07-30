package leekyunghee.examples.tdd.transcodingService;

import java.io.File;
import java.util.List;

public interface DestinationStorage {

	void save(List<File> multimediaFiles, List<File> thumbnails);

}
