package leekyunghee.examples.tdd.transcodingService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class TranscodingServiceTest {

	private Long jobId = new Long(1);
	
	@Mock
	private MediaSourceCopier mediaSourceCopier;
	@Mock
	private Transcoder transcoder;
	@Mock
	private CreatedFileSender createdFileSender;
	@Mock
	private ThumbnailExtractor thumbnailExtractor;
	@Mock
	private JobResultNotifier jobResultNotifier;
	
	// 테스트 클래스가 TranscodingService 클래스를 사용하도록 변경할 차례
	private TranscodingServiceImpl transcodingService;
	
	@Before
	public void setup() {
		transcodingService = new TranscodingServiceImpl(mediaSourceCopier, transcoder, 
				thumbnailExtractor, createdFileSender, jobResultNotifier);
	}

	@Test
	public void transcodeSuccessfully() {
		//Long jobId = new Long(1);
		File mockMultimediaFile = mock(File.class);
		when(mediaSourceCopier.copy(jobId)).thenReturn(mockMultimediaFile);
		
		List<File> mockMultimediaFiles = new ArrayList<File>();
		when(transcoder.transcode(mockMultimediaFile, jobId)).thenReturn(mockMultimediaFiles);
		
		List<File> mockThumbnails = new ArrayList<File>();
		when(thumbnailExtractor.extract(mockMultimediaFile, jobId)).thenReturn(mockThumbnails);
		
		transcodingService.transcode(jobId);
		
		verify(mediaSourceCopier, only()).copy(jobId);
		verify(transcoder, only()).transcode(mockMultimediaFile, jobId);
		verify(thumbnailExtractor, only()).extract(mockMultimediaFile, jobId);
		verify(createdFileSender, only()).send(mockMultimediaFiles, mockThumbnails, jobId);
		verify(jobResultNotifier, only()).notifyToRequester(jobId);
	}

}
